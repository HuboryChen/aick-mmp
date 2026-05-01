package com.aick.mmp.central.task;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摄像头软删除清理任务
 * 定期清理超过保留期限的已删除摄像头
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CameraSoftDeleteCleanupTask {

    private final CameraRepository cameraRepository;

    /**
     * 软删除保留天数
     */
    private static final int RETENTION_DAYS = 30;

    /**
     * 每日凌晨 2:00 执行清理任务
     * 删除 30 天前软删除的摄像头
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupSoftDeletedCameras() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);

        log.info("[清理任务] 开始清理 {} 天前软删除的摄像头", RETENTION_DAYS);

        // 查询所有已删除的摄像头
        List<Camera> deletedCameras = cameraRepository.findAllDeleted();

        // 过滤出超过保留期限的摄像头
        List<Camera> expiredCameras = deletedCameras.stream()
                .filter(c -> c.getDeletedAt() != null && c.getDeletedAt().isBefore(cutoffDate))
                .toList();

        if (expiredCameras.isEmpty()) {
            log.info("[清理任务] 没有需要清理的已删除摄像头");
            return;
        }

        log.info("[清理任务] 准备清理 {} 个超过保留期的摄像头", expiredCameras.size());

        // 执行物理删除
        cameraRepository.deleteAll(expiredCameras);

        log.info("[清理任务] 成功清理 {} 个摄像头", expiredCameras.size());
    }

    /**
     * 手动触发清理（供管理员接口调用）
     *
     * @param retentionDays 保留天数（默认 30 天）
     * @return 清理的摄像头数量
     */
    @Transactional
    public int manualCleanup(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        log.info("[手动清理] 开始清理 {} 天前软删除的摄像头", retentionDays);

        List<Camera> deletedCameras = cameraRepository.findAllDeleted();
        List<Camera> expiredCameras = deletedCameras.stream()
                .filter(c -> c.getDeletedAt() != null && c.getDeletedAt().isBefore(cutoffDate))
                .toList();

        if (expiredCameras.isEmpty()) {
            log.info("[手动清理] 没有需要清理的摄像头");
            return 0;
        }

        cameraRepository.deleteAll(expiredCameras);
        log.info("[手动清理] 成功清理 {} 个摄像头", expiredCameras.size());

        return expiredCameras.size();
    }
}
