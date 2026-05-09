package com.aick.mmp.central.task;

import com.aick.mmp.central.channel.NotificationChannelFactory;
import com.aick.mmp.central.channel.NotificationResult;
import com.aick.mmp.central.channel.NotificationSenderService;
import com.aick.mmp.central.repository.AlertRecordRepository;
import com.aick.mmp.central.repository.NotificationSendLogRepository;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.NotificationSendLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知发送重试调度任务
 * 定期检查并重试发送失败的通知
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryTask {

    private final NotificationSendLogRepository sendLogRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final NotificationChannelFactory channelFactory;
    private final NotificationSenderService notificationSenderService;

    @Value("${notification.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${notification.retry.batch-size:100}")
    private int batchSize;

    @Value("${notification.retry.delay-seconds:60}")
    private int retryDelaySeconds;

    private static final int MAX_CONCURRENT_RETRIES = 10;

    /**
     * 定期检查需要重试的通知
     * 默认每分钟执行一次
     */
    @Scheduled(fixedDelayString = "${notification.retry.check-interval:60000}")
    public void checkAndRetryNotifications() {
        if (!retryEnabled) {
            return;
        }

        log.debug("开始检查需要重试的通知...");

        try {
            // 查询所有需要重试的通知
            List<NotificationSendLog> retryableLogs = sendLogRepository.findRetryableNotifications(LocalDateTime.now());

            if (retryableLogs.isEmpty()) {
                log.debug("没有需要重试的通知");
                return;
            }

            log.info("发现 {} 条需要重试的通知", retryableLogs.size());

            // 限制并发重试数量
            int toProcess = Math.min(retryableLogs.size(), MAX_CONCURRENT_RETRIES);
            for (int i = 0; i < toProcess; i++) {
                NotificationSendLog sendLog = retryableLogs.get(i);
                retryNotification(sendLog);
            }

            // 如果还有更多待处理的通知，记录日志
            if (retryableLogs.size() > MAX_CONCURRENT_RETRIES) {
                log.warn("还有 {} 条通知等待重试，将在下次检查时处理", 
                        retryableLogs.size() - MAX_CONCURRENT_RETRIES);
            }

        } catch (Exception e) {
            log.error("重试通知时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 重试单条通知
     */
    private void retryNotification(NotificationSendLog sendLog) {
        try {
            // 获取关联的告警记录
            Optional<AlertRecord> alertOpt = alertRecordRepository.findById(sendLog.getAlertRecordId());
            if (alertOpt.isEmpty()) {
                log.warn("告警记录不存在，跳过重试: alertRecordId={}", sendLog.getAlertRecordId());
                sendLog.markFailed("ALERT_NOT_FOUND", "告警记录不存在");
                sendLogRepository.save(sendLog);
                return;
            }

            AlertRecord alert = alertOpt.get();
            String channelType = sendLog.getChannelType().name();

            // 获取对应渠道
            var channelOpt = channelFactory.getChannel(channelType);
            if (channelOpt.isEmpty() || !channelOpt.get().isAvailable()) {
                log.warn("通知渠道不可用，跳过重试: channelType={}", channelType);
                sendLog.markFailed("CHANNEL_UNAVAILABLE", "通知渠道不可用");
                sendLogRepository.save(sendLog);
                return;
            }

            // 执行重试
            log.info("开始重试通知: logId={}, alertId={}, channel={}, retryCount={}",
                    sendLog.getId(), sendLog.getAlertRecordId(), channelType, sendLog.getRetryCount() + 1);

            sendLog.markSending();
            sendLogRepository.save(sendLog);

            NotificationResult result = notificationSenderService.sendToChannel(
                    alert, channelType, sendLog.getTarget(), sendLog.getExtraConfig());

            if (result.isSuccess()) {
                sendLog.markSuccess(result.getResponseMessage(), result.getCostTime());
                log.info("通知重试成功: logId={}", sendLog.getId());
            } else {
                if (result.isRetryable() && sendLog.getRetryCount() < sendLog.getMaxRetry()) {
                    sendLog.markRetrying(retryDelaySeconds);
                    log.info("通知重试失败，将继续重试: logId={}, error={}", 
                            sendLog.getId(), result.getErrorMessage());
                } else {
                    sendLog.markFailed(result.getErrorCode(), result.getErrorMessage());
                    log.warn("通知重试失败，已达最大重试次数或不可重试: logId={}, error={}",
                            sendLog.getId(), result.getErrorMessage());
                }
            }

            sendLogRepository.save(sendLog);

        } catch (Exception e) {
            log.error("重试通知时发生异常: logId={}, error={}", sendLog.getId(), e.getMessage(), e);
            sendLog.markFailed("RETRY_EXCEPTION", e.getMessage());
            sendLogRepository.save(sendLog);
        }
    }

    /**
     * 清理过期的发送日志
     * 默认每天凌晨3点执行
     */
    @Scheduled(cron = "${notification.log.cleanup-cron:0 0 3 * * ?}")
    public void cleanupOldLogs() {
        log.info("开始清理过期的通知发送日志...");

        try {
            // 保留最近30天的日志
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            sendLogRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("通知发送日志清理完成，删除 {} 之前的记录", cutoffTime);

        } catch (Exception e) {
            log.error("清理通知发送日志时发生异常: {}", e.getMessage(), e);
        }
    }
}
