package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信通知渠道实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {

    private final WebClient.Builder webClientBuilder;

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    @Value("${notification.sms.api-url:}")
    private String apiUrl;

    @Value("${notification.sms.api-key:}")
    private String apiKey;

    @Value("${notification.sms.api-secret:}")
    private String apiSecret;

    @Value("${notification.sms.sign-name:AICK-MMP}")
    private String signName;

    @Value("${notification.sms.timeout:10000}")
    private int timeout;

    @Override
    public String getChannelType() {
        return "SMS";
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiUrl != null && !apiUrl.isBlank();
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target) {
        return send(alert, target, null);
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target, String extraConfig) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "短信通知渠道未启用");
        }

        if (target == null || target.isBlank()) {
            return NotificationResult.failureNonRetryable("INVALID_TARGET", "手机号不能为空");
        }

        if (!isValidPhoneNumber(target)) {
            return NotificationResult.failureNonRetryable("INVALID_PHONE", "手机号格式不正确");
        }

        long startTime = System.currentTimeMillis();
        try {
            String message = buildSmsContent(alert);
            Map<String, Object> payload = buildPayload(target, message);

            WebClient webClient = webClientBuilder
                    .baseUrl(apiUrl)
                    .build();

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .header("X-API-Secret", apiSecret)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            long costTime = System.currentTimeMillis() - startTime;

            // 根据实际API响应判断成功与否
            if (isSuccessResponse(response)) {
                log.info("短信通知发送成功: alertId={}, target={}, costTime={}ms",
                        alert.getId(), maskPhone(target), costTime);
                return NotificationResult.success("短信发送成功", costTime);
            } else {
                log.warn("短信通知发送失败: alertId={}, target={}, response={}",
                        alert.getId(), maskPhone(target), response);
                return NotificationResult.failure("SEND_FAILED", response);
            }

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("短信通知发送失败: alertId={}, target={}, error={}, costTime={}ms",
                    alert.getId(), maskPhone(target), e.getMessage(), costTime);

            // 判断是否为超时错误
            if (e instanceof java.util.concurrent.TimeoutException) {
                return NotificationResult.failure("TIMEOUT", "短信发送超时");
            }
            return NotificationResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public NotificationResult testConnection(String target) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "短信通知渠道未启用");
        }

        if (!isValidPhoneNumber(target)) {
            return NotificationResult.failureNonRetryable("INVALID_PHONE", "手机号格式不正确");
        }

        long startTime = System.currentTimeMillis();
        try {
            String message = String.format("【%s】您有一条测试短信，收到此短信表示短信通道配置正常。", signName);
            Map<String, Object> payload = buildPayload(target, message);

            WebClient webClient = webClientBuilder
                    .baseUrl(apiUrl)
                    .build();

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .header("X-API-Secret", apiSecret)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            long costTime = System.currentTimeMillis() - startTime;

            if (isSuccessResponse(response)) {
                log.info("测试短信发送成功: target={}, costTime={}ms", maskPhone(target), costTime);
                return NotificationResult.success("测试短信发送成功", costTime);
            } else {
                log.warn("测试短信发送失败: target={}, response={}", maskPhone(target), response);
                return NotificationResult.failure("TEST_FAILED", response);
            }

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("测试短信发送失败: target={}, error={}, costTime={}ms", 
            maskPhone(target), e.getMessage(), costTime);

            if (e instanceof java.util.concurrent.TimeoutException) {
                return NotificationResult.failure("TIMEOUT", "短信发送超时");
            }
            return NotificationResult.failure("TEST_FAILED", e.getMessage());
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        // 中国大陆手机号格式校验（简单版本）
        return phone.matches("^1[3-9]\\d{9}$");
    }

    private String buildSmsContent(AlertRecord alert) {
        String level = switch (alert.getLevel() != null ? alert.getLevel().name() : "INFO") {
            case "CRITICAL" -> "严重";
            case "WARNING" -> "警告";
            case "INFO" -> "信息";
            default -> "未知";
        };

        String target = alert.getTargetName() != null ? alert.getTargetName() :
                alert.getCameraName() != null ? alert.getCameraName() : "未知目标";

        // 短信内容限制在70字以内
        String content = String.format("[%s]%s:%s",
                level, alert.getTitle(), target);

        if (content.length() > 67) {
            content = content.substring(0, 64) + "...";
        }

        return String.format("【%s】%s", signName, content);
    }

    private Map<String, Object> buildPayload(String phone, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("message", message);
        payload.put("sign", signName);
        return payload;
    }

    private boolean isSuccessResponse(String response) {
        if (response == null) return false;
        // 简单判断，根据实际API调整
        return response.contains("\"code\":0") ||
               response.contains("\"success\":true") ||
               response.contains("\"Code\":\"OK\"") ||
               response.contains("ok");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
