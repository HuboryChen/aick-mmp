package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.AlertRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 告警通知服务
 * 支持 WebSocket、邮件、短信、webhook 等多种通知方式
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;
    private final WebClient.Builder webClientBuilder;

    private static final String ALERT_TOPIC = "/topic/alerts";

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@aick-mmp.com}")
    private String emailFrom;

    @Value("${notification.email.recipients:}")
    private String emailRecipients;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.api-url:}")
    private String smsApiUrl;

    @Value("${notification.sms.api-key:}")
    private String smsApiKey;

    @Value("${notification.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${notification.webhook.url:}")
    private String webhookUrl;

    @Value("${notification.webhook.secret:}")
    private String webhookSecret;

    /**
     * 发送告警通知
     */
    public void sendAlertNotification(AlertRecord alert) {
        try {
            // 发送 WebSocket 通知
            sendWebSocketNotification(alert);

            // 根据配置发送其他通知
            if (emailEnabled) {
                sendEmailNotification(alert);
            }

            if (smsEnabled) {
                sendSmsNotification(alert);
            }

            if (webhookEnabled) {
                sendWebhookNotification(alert);
            }

            log.info("Sent all enabled notifications for alert record {}", alert.getId());

        } catch (Exception e) {
            log.error("Failed to send alert notification: {}", e.getMessage());
            throw new RuntimeException("Notification sending failed", e);
        }
    }

    /**
     * 发送告警状态更新通知
     */
    public void sendAlertStatusUpdate(AlertRecord alert) {
        try {
            messagingTemplate.convertAndSend(ALERT_TOPIC + "/update", alert);
            log.info("Sent alert status update notification for record {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send alert status update: {}", e.getMessage());
        }
    }

    /**
     * 发送告警统计更新
     */
    public void sendAlertStatisticsUpdate(Object statistics) {
        try {
            messagingTemplate.convertAndSend(ALERT_TOPIC + "/statistics", statistics);
        } catch (Exception e) {
            log.error("Failed to send alert statistics update: {}", e.getMessage());
        }
    }

    // ==================== WebSocket 通知 ====================

    private void sendWebSocketNotification(AlertRecord alert) {
        try {
            messagingTemplate.convertAndSend(ALERT_TOPIC, alert);
            log.info("Sent WebSocket alert notification for record {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }

    // ==================== 邮件通知 ====================

    private void sendEmailNotification(AlertRecord alert) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(parseRecipients(emailRecipients));
            message.setSubject(buildEmailSubject(alert));
            message.setText(buildEmailBody(alert));

            CompletableFuture.runAsync(() -> {
                try {
                    mailSender.send(message);
                    log.info("Sent email notification for alert {}", alert.getId());
                } catch (Exception e) {
                    log.error("Failed to send email for alert {}: {}", alert.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    private String[] parseRecipients(String recipients) {
        if (recipients == null || recipients.isBlank()) {
            return new String[0];
        }
        return recipients.split("[,;]");
    }

    private String buildEmailSubject(AlertRecord alert) {
        String levelPrefix = switch (alert.getLevel().name()) {
            case "CRITICAL" -> "[严重] ";
            case "WARNING" -> "[警告] ";
            case "INFO" -> "[信息] ";
            default -> "";
        };
        return String.format("%s%s - %s", levelPrefix, alert.getTitle(), formatTime(alert.getAlertTime()));
    }

    private String buildEmailBody(AlertRecord alert) {
        StringBuilder body = new StringBuilder();
        body.append("告警通知\n");
        body.append("========\n\n");
        body.append(String.format("告警级别: %s\n", alert.getLevel()));
        body.append(String.format("告警类型: %s\n", alert.getAlertType()));
        body.append(String.format("告警时间: %s\n", formatTime(alert.getAlertTime())));
        body.append(String.format("告警标题: %s\n", alert.getTitle()));
        body.append(String.format("告警详情: %s\n\n", alert.getMessage()));

        if (alert.getTargetName() != null) {
            body.append(String.format("监控目标: %s\n", alert.getTargetName()));
        }
        if (alert.getCameraName() != null) {
            body.append(String.format("摄像头: %s\n", alert.getCameraName()));
        }
        if (alert.getActualValue() != null && alert.getThresholdValue() != null) {
            body.append(String.format("实际值: %s (阈值: %s)\n", alert.getActualValue(), alert.getThresholdValue()));
        }

        body.append("\n========\n");
        body.append("AICK-MMP 多区域视频监控平台\n");
        return body.toString();
    }

    // ==================== 短信通知 ====================

    private void sendSmsNotification(AlertRecord alert) {
        if (smsApiUrl == null || smsApiUrl.isBlank()) {
            log.warn("SMS API URL not configured, skipping SMS notification");
            return;
        }

        try {
            String message = buildSmsMessage(alert);

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);
            // SMS API specific fields can be added here

            WebClient webClient = webClientBuilder.baseUrl(smsApiUrl).build();

            CompletableFuture.runAsync(() -> {
                try {
                    webClient.post()
                            .uri(smsApiUrl)
                            .header("X-API-Key", smsApiKey)
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(String.class)
                            .subscribe(
                                    response -> log.info("SMS sent successfully for alert {}", alert.getId()),
                                    error -> log.error("Failed to send SMS for alert {}: {}", alert.getId(), error.getMessage())
                            );
                } catch (Exception e) {
                    log.error("Failed to send SMS notification: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send SMS notification: {}", e.getMessage());
        }
    }

    private String buildSmsMessage(AlertRecord alert) {
        // 短信内容需要简洁，限制长度
        String level = switch (alert.getLevel().name()) {
            case "CRITICAL" -> "严重";
            case "WARNING" -> "警告";
            case "INFO" -> "信息";
            default -> alert.getLevel().name();
        };

        String target = alert.getTargetName() != null ? alert.getTargetName() :
                        alert.getCameraName() != null ? alert.getCameraName() : "未知目标";

        return String.format("[%s] %s: %s @ %s",
                level, alert.getTitle(), target, formatTime(alert.getAlertTime()));
    }

    // ==================== Webhook 通知 ====================

    private void sendWebhookNotification(AlertRecord alert) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Webhook URL not configured, skipping webhook notification");
            return;
        }

        try {
            Map<String, Object> payload = buildWebhookPayload(alert);

            WebClient webClient = webClientBuilder.baseUrl(webhookUrl).build();

            CompletableFuture.runAsync(() -> {
                try {
                    webClient.post()
                            .uri(webhookUrl)
                            .header("Content-Type", "application/json")
                            .header("X-Webhook-Secret", webhookSecret)
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(String.class)
                            .subscribe(
                                    response -> log.info("Webhook sent successfully for alert {}", alert.getId()),
                                    error -> log.error("Failed to send webhook for alert {}: {}", alert.getId(), error.getMessage())
                            );
                } catch (Exception e) {
                    log.error("Failed to send webhook notification: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send webhook notification: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildWebhookPayload(AlertRecord alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "alert.triggered");
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> alertMap = new HashMap<>();
        alertMap.put("id", alert.getId());
        alertMap.put("level", alert.getLevel() != null ? alert.getLevel().name() : null);
        alertMap.put("type", alert.getAlertType() != null ? alert.getAlertType().name() : null);
        alertMap.put("title", alert.getTitle());
        alertMap.put("message", alert.getMessage());
        alertMap.put("status", alert.getStatus() != null ? alert.getStatus().name() : null);
        alertMap.put("alertTime", formatTime(alert.getAlertTime()));
        alertMap.put("targetType", alert.getTargetType() != null ? alert.getTargetType().name() : null);
        alertMap.put("targetId", alert.getTargetId());
        alertMap.put("targetName", alert.getTargetName());
        alertMap.put("cameraId", alert.getCameraId());
        alertMap.put("cameraName", alert.getCameraName());
        alertMap.put("actualValue", alert.getActualValue());
        alertMap.put("thresholdValue", alert.getThresholdValue());
        alertMap.put("edgeNodeId", alert.getEdgeNodeId());
        alertMap.put("regionId", alert.getRegionId());
        alertMap.put("source", alert.getSource());

        payload.put("alert", alertMap);
        return payload;
    }

    // ==================== 批量通知 ====================

    /**
     * 发送批量告警通知
     */
    public void sendBatchAlertNotification(List<AlertRecord> alerts) {
        for (AlertRecord alert : alerts) {
            try {
                sendAlertNotification(alert);
            } catch (Exception e) {
                log.error("Failed to send notification for alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    // ==================== 辅助方法 ====================

    private String formatTime(LocalDateTime time) {
        if (time == null) return "未知";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
