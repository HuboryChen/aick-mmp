package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件通知渠道实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean enabled;

    @Value("${notification.email.from:noreply@aick-mmp.com}")
    private String defaultFrom;

    @Value("${notification.email.subject-prefix:[AICK-MMP]}")
    private String subjectPrefix;

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    public boolean isAvailable() {
        return enabled && mailSender != null;
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target) {
        return send(alert, target, null);
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target, String extraConfig) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "邮件通知渠道未启用");
        }

        if (target == null || target.isBlank()) {
            return NotificationResult.failureNonRetryable("INVALID_TARGET", "邮件地址不能为空");
        }

        long startTime = System.currentTimeMillis();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFrom);
            message.setTo(parseRecipients(target));
            message.setSubject(buildSubject(alert));
            message.setText(buildText(alert));
            message.setSentDate(java.util.Date.from(alert.getAlertTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));

            mailSender.send(message);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("邮件通知发送成功: alertId={}, target={}, costTime={}ms",
                    alert.getId(), maskEmail(target), costTime);

            return NotificationResult.success("邮件发送成功", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("邮件通知发送失败: alertId={}, target={}, error={}, costTime={}ms",
                    alert.getId(), maskEmail(target), e.getMessage(), costTime);

            return NotificationResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public NotificationResult testConnection(String target) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "邮件通知渠道未启用");
        }

        long startTime = System.currentTimeMillis();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFrom);
            message.setTo(parseRecipients(target));
            message.setSubject(subjectPrefix + " 测试邮件");
            message.setText("这是一封来自 AICK-MMP 监控平台的测试邮件。\n\n如果您收到此邮件，说明邮件通知渠道配置正确。");
            message.setSentDate(new java.util.Date());

            mailSender.send(message);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("测试邮件发送成功: target={}, costTime={}ms", maskEmail(target), costTime);

            return NotificationResult.success("测试邮件发送成功", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("测试邮件发送失败: target={}, error={}, costTime={}ms", 
                    maskEmail(target), e.getMessage(), costTime);

            return NotificationResult.failure("TEST_FAILED", e.getMessage());
        }
    }

    private String[] parseRecipients(String recipients) {
        if (recipients == null || recipients.isBlank()) {
            return new String[0];
        }
        return recipients.split("[,;]");
    }

    private String buildSubject(AlertRecord alert) {
        String levelPrefix = switch (alert.getLevel() != null ? alert.getLevel().name() : "INFO") {
            case "CRITICAL" -> "[严重] ";
            case "WARNING" -> "[警告] ";
            case "INFO" -> "[信息] ";
            default -> "";
        };
        return String.format("%s%s%s", subjectPrefix, levelPrefix, alert.getTitle());
    }

    private String buildText(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("告警通知\n");
        sb.append("=".repeat(40)).append("\n\n");

        sb.append(String.format("告警级别: %s\n", alert.getLevel()));
        sb.append(String.format("告警类型: %s\n", alert.getAlertType()));
        sb.append(String.format("告警时间: %s\n", formatTime(alert.getAlertTime())));
        sb.append(String.format("告警标题: %s\n\n", alert.getTitle()));
        sb.append(String.format("告警详情: %s\n\n", alert.getMessage()));

        if (alert.getTargetName() != null) {
            sb.append(String.format("监控目标: %s\n", alert.getTargetName()));
        }
        if (alert.getCameraName() != null) {
            sb.append(String.format("摄像头: %s\n", alert.getCameraName()));
        }
        if (alert.getActualValue() != null && alert.getThresholdValue() != null) {
            sb.append(String.format("实际值: %s (阈值: %s)\n", alert.getActualValue(), alert.getThresholdValue()));
        }

        sb.append("\n").append("=".repeat(40)).append("\n");
        sb.append("AICK-MMP 多区域视频监控平台\n");
        sb.append("请及时处理此告警，如有疑问请联系管理员。\n");

        return sb.toString();
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "未知";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}
