package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket 应用内通知渠道实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationChannel implements NotificationChannel {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String ALERT_TOPIC = "/topic/alerts";
    private static final String ALERT_UPDATE_TOPIC = "/topic/alerts/update";

    @Override
    public String getChannelType() {
        return "IN_APP";
    }

    @Override
    public boolean isAvailable() {
        return messagingTemplate != null;
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target) {
        return send(alert, target, null);
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target, String extraConfig) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "WebSocket通知渠道不可用");
        }

        long startTime = System.currentTimeMillis();
        try {
            // 构建通知消息
            AlertNotificationMessage message = buildNotificationMessage(alert);

            // 发送到全局告警主题
            messagingTemplate.convertAndSend(ALERT_TOPIC, message);

            // 如果指定了用户目标，发送到用户私有主题
            if (target != null && !target.isBlank()) {
                String userTopic = "/user/" + target + "/alerts";
                messagingTemplate.convertAndSend(userTopic, message);
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("WebSocket通知发送成功: alertId={}, target={}, costTime={}ms",
                    alert.getId(), target, costTime);

            return NotificationResult.success("应用内通知发送成功", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("WebSocket通知发送失败: alertId={}, target={}, error={}, costTime={}ms",
                    alert.getId(), target, e.getMessage(), costTime);

            return NotificationResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public NotificationResult testConnection(String target) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "WebSocket通知渠道不可用");
        }

        long startTime = System.currentTimeMillis();
        try {
            // 发送测试消息
            AlertNotificationMessage testMessage = AlertNotificationMessage.builder()
                    .type("TEST")
                    .alertId(0L)
                    .level("INFO")
                    .title("WebSocket 连接测试")
                    .message("这是一条测试消息，用于验证WebSocket通知渠道配置是否正确。")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            messagingTemplate.convertAndSend(ALERT_TOPIC, testMessage);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("WebSocket测试消息发送成功: costTime={}ms", costTime);

            return NotificationResult.success("WebSocket测试消息发送成功", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("WebSocket测试消息发送失败: error={}, costTime={}ms", e.getMessage(), costTime);

            return NotificationResult.failure("TEST_FAILED", e.getMessage());
        }
    }

    /**
     * 发送告警状态更新通知
     */
    public NotificationResult sendStatusUpdate(AlertRecord alert) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "WebSocket通知渠道不可用");
        }

        long startTime = System.currentTimeMillis();
        try {
            messagingTemplate.convertAndSend(ALERT_UPDATE_TOPIC, alert);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("告警状态更新通知发送成功: alertId={}, costTime={}ms", alert.getId(), costTime);

            return NotificationResult.success("告警状态更新通知发送成功", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("告警状态更新通知发送失败: alertId={}, error={}, costTime={}ms", alert.getId(), e.getMessage(), costTime);

            return NotificationResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    private AlertNotificationMessage buildNotificationMessage(AlertRecord alert) {
        return AlertNotificationMessage.builder()
                .type("ALERT")
                .alertId(alert.getId())
                .level(alert.getLevel() != null ? alert.getLevel().name() : null)
                .alertType(alert.getAlertType() != null ? alert.getAlertType().name() : null)
                .title(alert.getTitle())
                .message(alert.getMessage())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .targetType(alert.getTargetType() != null ? alert.getTargetType().name() : null)
                .targetId(alert.getTargetId())
                .targetName(alert.getTargetName())
                .cameraId(alert.getCameraId())
                .cameraName(alert.getCameraName())
                .actualValue(alert.getActualValue())
                .thresholdValue(alert.getThresholdValue())
                .timestamp(alert.getAlertTime() != null ?
                        alert.getAlertTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    /**
     * 告警通知消息（用于WebSocket传输）
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlertNotificationMessage {
        private String type;              // 消息类型: ALERT, TEST
        private Long alertId;             // 告警ID
        private String level;             // 告警级别
        private String alertType;         // 告警类型
        private String title;             // 标题
        private String message;           // 消息内容
        private String status;            // 状态
        private String targetType;        // 目标类型
        private Long targetId;            // 目标ID
        private String targetName;        // 目标名称
        private Long cameraId;            // 摄像头ID
        private String cameraName;        // 摄像头名称
        private Double actualValue;       // 实际值
        private Double thresholdValue;    // 阈值
        private String timestamp;         // 时间戳
    }
}
