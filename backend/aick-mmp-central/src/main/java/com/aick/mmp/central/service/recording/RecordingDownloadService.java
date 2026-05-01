package com.aick.mmp.central.service.recording;

import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.central.repository.RecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 录像下载服务
 */
@Service
public class RecordingDownloadService {

    private static final Logger log = LoggerFactory.getLogger(RecordingDownloadService.class);

    private final RecordingRepository recordingRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    // 并发控制：最多3个并发下载
    private final Semaphore downloadSemaphore;

    // 当前下载计数
    private final AtomicInteger currentDownloads = new AtomicInteger(0);

    // 活动下载追踪
    private final ConcurrentHashMap<Long, DownloadTask> activeDownloads = new ConcurrentHashMap<>();

    public RecordingDownloadService(
            RecordingRepository recordingRepository,
            FileStorageService fileStorageService,
            StorageProperties storageProperties) {
        this.recordingRepository = recordingRepository;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
        this.downloadSemaphore = new Semaphore(storageProperties.getMaxDownloadConcurrency());
    }

    /**
     * 获取录像文件的输入流
     */
    public Optional<DownloadResult> prepareDownload(Long recordingId) {
        // 检查并发限制
        if (!downloadSemaphore.tryAcquire()) {
            log.warn("下载并发限制已达上限，当前下载数: {}", currentDownloads.get());
            return Optional.empty();
        }

        try {
            // 查找录像记录
            Recording recording = recordingRepository.findById(recordingId).orElse(null);
            if (recording == null) {
                downloadSemaphore.release();
                return Optional.empty();
            }

            // 检查文件是否被锁定
            if (Boolean.TRUE.equals(recording.getLockStatus())) {
                log.warn("录像 {} 已被锁定，无法下载", recordingId);
                downloadSemaphore.release();
                return Optional.empty();
            }

            // 检查文件是否存在
            if (!fileStorageService.fileExists(recording.getStoragePath())) {
                log.warn("录像文件不存在: {}", recording.getStoragePath());
                downloadSemaphore.release();
                return Optional.empty();
            }

            // 获取文件大小
            Optional<Long> fileSize = fileStorageService.getFileSize(recording.getStoragePath());
            if (fileSize.isEmpty()) {
                downloadSemaphore.release();
                return Optional.empty();
            }

            // 锁定文件
            recording.setLockStatus(true);
            recordingRepository.save(recording);

            // 创建下载任务
            DownloadTask task = new DownloadTask(recordingId, recording.getName(), fileSize.get());
            activeDownloads.put(recordingId, task);
            currentDownloads.incrementAndGet();

            // 获取输入流
            Optional<InputStream> inputStream = fileStorageService.getInputStream(recording.getStoragePath());
            if (inputStream.isEmpty()) {
                // 获取失败，释放资源
                releaseDownload(recordingId);
                return Optional.empty();
            }

            return Optional.of(new DownloadResult(
                    inputStream.get(),
                    recording.getName(),
                    fileSize.get(),
                    recording.getFormat()
            ));

        } catch (Exception e) {
            log.error("准备下载失败: recordingId={}", recordingId, e);
            downloadSemaphore.release();
            return Optional.empty();
        }
    }

    /**
     * 释放下载资源
     */
    @Transactional
    public void releaseDownload(Long recordingId) {
        activeDownloads.remove(recordingId);
        currentDownloads.decrementAndGet();
        downloadSemaphore.release();

        // 解锁录像记录
        recordingRepository.findById(recordingId).ifPresent(recording -> {
            recording.setLockStatus(false);
            recordingRepository.save(recording);
        });

        log.info("释放下载资源: recordingId={}", recordingId);
    }

    /**
     * 获取当前活动下载数
     */
    public int getActiveDownloadCount() {
        return currentDownloads.get();
    }

    /**
     * 获取活动下载列表
     */
    public List<DownloadTask> getActiveDownloads() {
        return List.copyOf(activeDownloads.values());
    }

    /**
     * 批量准备下载
     */
    public List<DownloadResult> prepareBatchDownload(List<Long> recordingIds) {
        return recordingIds.stream()
                .map(this::prepareDownload)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 下载结果
     */
    public record DownloadResult(
            InputStream inputStream,
            String fileName,
            long fileSize,
            String format
    ) {}

    /**
     * 下载任务
     */
    public record DownloadTask(
            Long recordingId,
            String fileName,
            long fileSize
    ) {}
}
