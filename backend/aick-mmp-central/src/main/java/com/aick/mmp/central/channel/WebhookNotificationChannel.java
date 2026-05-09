package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook 通知渠道实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationChannel implements NotificationChannel {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${notification.webhook.enabled:false}")
    private boolean enabled;

    @Value("${notification.webhook.default-url:}")
    private String defaultUrl;

    @Value("${notification.webhook.secret:}")
    private String defaultSecret;

    @Value("${notification.webhook.timeout:10000}")
    private int timeout;

    @Value("${notification.webhook.retry-count:3}")
    private int retryCount;

    @Override
    public String getChannelType() {
        return "WEBHOOK";
    }

    @Override
    public boolean isAvailable() {
        return enabled && defaultUrl != null && !defaultUrl.isBlank();
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target) {
        return send(alert, target, null);
    }

    @Override
    public NotificationResult send(AlertRecord alert, String target, String extraConfig) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "Webhook通知渠道未启用");
        }

        String webhookUrl = (target != null && !target.isBlank()) ? target : defaultUrl;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return NotificationResult.failureNonRetryable("INVALID_TARGET", "Webhook URL不能为空");
        }

        // 从extraConfig中提取secret（如果存在）
        String secret = extractSecret(extraConfig, defaultSecret);

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> payload = buildPayload(alert);
            String payloadJson = objectMapper.writeValueAsString(payload);

            WebClient webClient = webClientBuilder.baseUrl(webhookUrl).build();

            WebClient.RequestBodySpec requestSpec = webClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Secret", secret)
                    .header("X-Alert-Id", String.valueOf(alert.getId()))
                    .header("X-Timestamp", String.valueOf(System.currentTimeMillis()));

            String response = requestSpec
                    .bodyValue(payloadJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            long costTime = System.currentTimeMillis() - startTime;

            if (isSuccessResponse(response)) {
                log.info("Webhook通知发送成功: alertId={}, url={}, costTime={}ms",
                        alert.getId(), maskUrl(webhookUrl), costTime);
                return NotificationResult.success("Webhook发送成功", costTime);
            } else {
                log.warn("Webhook通知发送失败: alertId={}, url={}, response={}",
                        alert.getId(), maskUrl(webhookUrl), response);
                return NotificationResult.failure("SEND_FAILED", response);
            }

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Webhook通知发送失败: alertId={}, url={}, error={}, costTime={}ms",
                    alert.getId(), maskUrl(webhookUrl), e.getMessage(), costTime);

            if (e instanceof java.util.concurrent.TimeoutException) {
                return NotificationResult.failure("TIMEOUT", "Webhook请求超时");
            }
            return NotificationResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public NotificationResult testConnection(String target) {
        if (!isAvailable()) {
            return NotificationResult.failureNonRetryable("CHANNEL_DISABLED", "Webhook通知渠道未启用");
        }

        String webhookUrl = (target != null && !target.isBlank()) ? target : defaultUrl;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return NotificationResult.failureNonRetryable("INVALID_TARGET", "Webhook URL不能为空");
        }

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "test");
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            payload.put("message", "AICK-MMP Webhook测试消息");

            String payloadJson = objectMapper.writeValueAsString(payload);

            WebClient webClient = webClientBuilder.baseUrl(webhookUrl).build();

            String response = webClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Secret", defaultSecret)
                    .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                    .bodyValue(payloadJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            long costTime = System.currentTimeMillis() - startTime;

            if (isSuccessResponse(response)) {
                log.info("Webhook测试成功: url={}, costTime={}ms", maskUrl(webhookUrl), costTime);
                return NotificationResult.success("Webhook测试成功", costTime);
            } else {
                log.warn("Webhook测试失败: url={}, response={}", maskUrl(webhookUrl), response);
                return NotificationResult.failure("TEST_FAILED", response);
            }

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Webhook测试失败: url={}, error={}, costTime={}ms", maskUrl(webhookUrl), e.getMessage(), costTime);

            if (e instanceof java.util.concurrent.TimeoutException) {
                return NotificationResult.failure("TIMEOUT", "Webhook请求超时");
            }
            return NotificationResult.failure("TEST_FAILED", e.getMessage());
        }
    }

    /**
     * 获取重试次数配置
     */
    public int getRetryCount() {
        return retryCount;
    }

    private Map<String, Object> buildPayload(AlertRecord alert) {
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

    private String extractSecret(String extraConfig, String defaultSecret) {
        if (extraConfig == null || extraConfig.isBlank()) {
            return defaultSecret;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(extraConfig, Map.class);
            Object secret = config.get("secret");
            return secret != null ? secret.toString() : defaultSecret;
        } catch (JsonProcessingException e) {
            log.warn("解析webhook配置失败: {}", e.getMessage());
            return defaultSecret;
        }
    }

    private boolean isSuccessResponse(String response) {
        if (response == null) return false;
        return response.contains("\"code\":0") ||
               response.contains("\"success\":true") ||
               response.contains("\"code\":\"0000\"") ||
               response.contains("ok");
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return null;
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
               time.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private String maskUrl(String url) {
        if (url == null) return null;
        // 移除敏感信息，只保留域名和路径
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + "/***";
        } catch (Exception e) {
            return url.length() > 20 ? url.substring(0, 20) + "..." : url;
        }
    }
}
