package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 通知发送服务
 * 负责协调多个通知渠道发送告警通知，并处理重试逻辑
 */
@Service
@Slf4j
public class NotificationSenderService {

    private final NotificationChannelFactory channelFactory;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notification.retry.delay-seconds:60}")
    private int retryDelaySeconds;

    @Value("${notification.async.enabled:true}")
    private boolean asyncEnabled;

    public NotificationSenderService(NotificationChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    /**
     * 发送通知（使用 AlertNotification 配置）
     *
     * @param alert 告警记录
     * @param notification 通知配置
     * @return 发送结果
     */
    public NotificationResult sendNotification(AlertRecord alert, AlertNotification notification) {
        if (notification == null || !Boolean.TRUE.equals(notification.getIsEnabled())) {
            log.debug("通知配置未启用或为空: alertId={}", alert.getId());
            return NotificationResult.failureNonRetryable("NOTIFICATION_DISABLED", "通知配置未启用");
        }

        String channelType = notification.getChannelType().name();
        return sendToChannel(alert, channelType, notification.getTarget(), notification.getExtraConfig());
    }

    /**
     * 发送通知到指定渠道
     *
     * @param alert 告警记录
     * @param channelType 渠道类型
     * @param target 通知目标
     * @return 发送结果
     */
    public NotificationResult sendToChannel(AlertRecord alert, String channelType, String target) {
        return sendToChannel(alert, channelType, target, null);
    }

    /**
     * 发送通知到指定渠道（带额外配置）
     *
     * @param alert 告警记录
     * @param channelType 渠道类型
     * @param target 通知目标
     * @param extraConfig 额外配置
     * @return 发送结果
     */
    public NotificationResult sendToChannel(AlertRecord alert, String channelType, String target, String extraConfig) {
        var channelOpt = channelFactory.getChannel(channelType);
        if (channelOpt.isEmpty()) {
            log.warn("未知的通知渠道类型: channelType={}, alertId={}", channelType, alert.getId());
            return NotificationResult.failureNonRetryable("UNKNOWN_CHANNEL", "未知的通知渠道类型: " + channelType);
        }

        NotificationChannel channel = channelOpt.get();
        if (!channel.isAvailable()) {
            log.warn("通知渠道不可用: channelType={}, alertId={}", channelType, alert.getId());
            return NotificationResult.failureNonRetryable("CHANNEL_UNAVAILABLE", "通知渠道不可用: " + channelType);
        }

        NotificationResult result = channel.send(alert, target, extraConfig);

        // 如果发送失败且可重试，执行重试逻辑
        if (!result.isSuccess() && result.isRetryable()) {
            result = retrySend(channel, alert, target, extraConfig, maxRetryAttempts);
        }

        return result;
    }

    /**
     * 发送通知到多个渠道
     *
     * @param alert 告警记录
     * @param notifications 通知配置列表
     * @return 各渠道的发送结果
     */
    public List<NotificationResult> sendToMultipleChannels(AlertRecord alert, List<AlertNotification> notifications) {
        List<NotificationResult> results = new ArrayList<>();
        for (AlertNotification notification : notifications) {
            try {
                NotificationResult result = sendNotification(alert, notification);
                results.add(result);
            } catch (Exception e) {
                log.error("发送通知时发生异常: alertId={}, channel={}, error={}",
                        alert.getId(), notification.getChannelType(), e.getMessage());
                results.add(NotificationResult.failure("SEND_EXCEPTION", e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 异步发送通知
     *
     * @param alert 告警记录
     * @param notification 通知配置
     * @return CompletableFuture
     */
    @Async
    public CompletableFuture<NotificationResult> sendNotificationAsync(AlertRecord alert, AlertNotification notification) {
        NotificationResult result = sendNotification(alert, notification);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 异步发送通知到多个渠道
     *
     * @param alert 告警记录
     * @param notifications 通知配置列表
     * @return CompletableFuture列表
     */
    @Async
    public CompletableFuture<List<NotificationResult>> sendToMultipleChannelsAsync(
            AlertRecord alert, List<AlertNotification> notifications) {
        List<NotificationResult> results = sendToMultipleChannels(alert, notifications);
        return CompletableFuture.completedFuture(results);
    }

    /**
     * 重试发送通知
     */
    private NotificationResult retrySend(NotificationChannel channel, AlertRecord alert,
                                        String target, String extraConfig, int remainingAttempts) {
        if (remainingAttempts <= 0) {
            log.warn("通知发送重试次数用尽: channel={}, alertId={}", channel.getChannelType(), alert.getId());
            return NotificationResult.failure("RETRY_EXHAUSTED", "重试次数用尽");
        }

        log.info("开始重试发送通知: channel={}, alertId={}, remainingAttempts={}",
                channel.getChannelType(), alert.getId(), remainingAttempts);

        try {
            // 等待一段时间后重试
            TimeUnit.SECONDS.sleep(retryDelaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NotificationResult.failure("RETRY_INTERRUPTED", "重试被中断");
        }

        NotificationResult result = channel.send(alert, target, extraConfig);

        if (!result.isSuccess() && result.isRetryable()) {
            // 递归重试
            return retrySend(channel, alert, target, extraConfig, remainingAttempts - 1);
        }

        return result;
    }

    /**
     * 测试渠道连接
     *
     * @param channelType 渠道类型
     * @param target 测试目标
     * @return 测试结果
     */
    public NotificationResult testChannel(String channelType, String target) {
        var channelOpt = channelFactory.getChannel(channelType);
        if (channelOpt.isEmpty()) {
            return NotificationResult.failureNonRetryable("UNKNOWN_CHANNEL", "未知的通知渠道类型: " + channelType);
        }

        NotificationChannel channel = channelOpt.get();
        if (!channel.isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_UNAVAILABLE", "通知渠道不可用: " + channelType);
        }

        return channel.testConnection(target);
    }
}
