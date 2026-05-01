package com.aick.mmp.central.service.recording;

import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.central.repository.RecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 录像清理服务
 */
@Service
public class RecordingCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RecordingCleanupService.class);

    private final RecordingRepository recordingRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    public RecordingCleanupService(
            RecordingRepository recordingRepository,
            FileStorageService fileStorageService,
            StorageProperties storageProperties) {
        this.recordingRepository = recordingRepository;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
    }

    /**
     * 定时清理任务 (每天凌晨2点执行)
     */
    @Scheduled(cron = "${recording.cleanup.cron:0 0 2 * * ?}")
    public void scheduledCleanup() {
        if (!storageProperties.isEnabled()) {
            log.info("录像清理服务已禁用");
            return;
        }

        log.info("开始执行录像定时清理任务");
        CleanupResult result = performCleanup();
        log.info("录像定时清理任务完成: deletedCount={}, freedBytes={}", 
                result.deletedCount(), result.freedBytes());
    }

    /**
     * 执行清理
     */
    public CleanupResult performCleanup() {
        long startTime = System.currentTimeMillis();
        int deletedCount = 0;
        long freedBytes = 0;

        // 1. 按时间清理过期录像
        CleanupResult timeResult = cleanupByRetentionDays();
        deletedCount += timeResult.deletedCount();
        freedBytes += timeResult.freedBytes();

        // 2. 检查磁盘使用率，如果超过阈值则清理
        CleanupResult diskResult = cleanupByDiskUsage();
        deletedCount += diskResult.deletedCount();
        freedBytes += diskResult.freedBytes();

        long duration = System.currentTimeMillis() - startTime;
        log.info("录像清理完成: deletedCount={}, freedBytes={}, duration={}ms", 
                deletedCount, freedBytes, duration);

        return new CleanupResult(deletedCount, freedBytes, duration);
    }

    /**
     * 按保留天数清理
     */
    @Transactional
    public CleanupResult cleanupByRetentionDays() {
        LocalDate cutoffDate = LocalDate.now().minusDays(storageProperties.getRetentionDays());
        LocalDateTime cutoffDateTime = cutoffDate.atStartOfDay();

        log.info("按保留天数清理: retentionDays={}, cutoffDate={}", 
                storageProperties.getRetentionDays(), cutoffDate);

        // 查找过期的录像
        Page<Recording> expiredRecordings = recordingRepository
                .findExpiredRecordings(cutoffDateTime, Pageable.unpaged());

        int deletedCount = 0;
        long freedBytes = 0;

        for (Recording recording : expiredRecordings) {
            // 跳过正在下载的录像
            if (Boolean.TRUE.equals(recording.getLockStatus())) {
                log.debug("跳过锁定的录像: id={}", recording.getId());
                continue;
            }

            // 删除文件
            boolean deleted = fileStorageService.deleteFile(recording.getStoragePath());
            if (deleted) {
                // 更新数据库状态
                recording.setIntegrityStatus("DELETED");
                recordingRepository.save(recording);
                deletedCount++;
                freedBytes += Optional.ofNullable(recording.getFileSize()).orElse(0L);
                
                log.debug("删除过期录像: id={}, path={}", 
                        recording.getId(), recording.getStoragePath());
            }
        }

        log.info("按保留天数清理完成: deletedCount={}, freedBytes={}", deletedCount, freedBytes);
        return new CleanupResult(deletedCount, freedBytes, 0);
    }

    /**
     * 按磁盘使用率清理
     */
    @Transactional
    public CleanupResult cleanupByDiskUsage() {
        long totalBytes = fileStorageService.getTotalDiskSpace();
        long usedBytes = fileStorageService.getUsedDiskSpace();
        
        if (totalBytes == 0) {
            log.warn("无法获取磁盘空间信息");
            return new CleanupResult(0, 0, 0);
        }

        double usageRatio = (double) usedBytes / totalBytes;
        
        if (usageRatio < storageProperties.getDiskUsageThreshold()) {
            log.info("磁盘使用率未达到清理阈值: usageRatio={}, threshold={}", 
                    usageRatio, storageProperties.getDiskUsageThreshold());
            return new CleanupResult(0, 0, 0);
        }

        log.info("磁盘使用率超过阈值，开始清理: usageRatio={}, threshold={}", 
                usageRatio, storageProperties.getDiskUsageThreshold());

        // 计算清理目标
        long targetBytes = storageProperties.getCleanupTargetBytes(totalBytes);
        long bytesToFree = usedBytes - targetBytes;

        // 按日期倒序查找录像（从最旧的开始）
        Page<Recording> recordings = recordingRepository
                .findRecordingsForCleanup(Pageable.unpaged());

        int deletedCount = 0;
        long freedBytes = 0;

        for (Recording recording : recordings) {
            if (freedBytes >= bytesToFree) {
                break;
            }

            // 跳过正在下载的录像
            if (Boolean.TRUE.equals(recording.getLockStatus())) {
                continue;
            }

            // 删除文件
            boolean deleted = fileStorageService.deleteFile(recording.getStoragePath());
            if (deleted) {
                // 更新数据库状态
                recording.setIntegrityStatus("DELETED");
                recordingRepository.save(recording);
                deletedCount++;
                freedBytes += Optional.ofNullable(recording.getFileSize()).orElse(0L);
            }
        }

        log.info("按磁盘使用率清理完成: deletedCount={}, freedBytes={}", deletedCount, freedBytes);
        return new CleanupResult(deletedCount, freedBytes, 0);
    }

    /**
     * 验证并更新录像完整性状态
     */
    @Transactional
    public int verifyIntegrity() {
        log.info("开始验证录像完整性");

        // 查找所有 COMPLETED 状态的录像
        List<Recording> recordings = recordingRepository.findByIntegrityStatus("COMPLETED");

        int corruptedCount = 0;

        for (Recording recording : recordings) {
            // 跳过正在下载的录像
            if (Boolean.TRUE.equals(recording.getLockStatus())) {
                continue;
            }

            // 检查文件是否存在
            if (!fileStorageService.fileExists(recording.getStoragePath())) {
                recording.setIntegrityStatus("CORRUPTED");
                recordingRepository.save(recording);
                corruptedCount++;
                log.warn("录像文件丢失: id={}, path={}", 
                        recording.getId(), recording.getStoragePath());
                continue;
            }

            // 验证MD5
            if (recording.getMd5() != null) {
                boolean valid = fileStorageService.verifyIntegrity(
                        recording.getStoragePath(), recording.getMd5());
                if (!valid) {
                    recording.setIntegrityStatus("CORRUPTED");
                    recordingRepository.save(recording);
                    corruptedCount++;
                    log.warn("录像MD5验证失败: id={}", recording.getId());
                }
            }
        }

        log.info("录像完整性验证完成: total={}, corrupted={}", 
                recordings.size(), corruptedCount);
        return corruptedCount;
    }

    /**
     * 清理结果
     */
    public record CleanupResult(
            int deletedCount,
            long freedBytes,
            long durationMs
    ) {}
}
