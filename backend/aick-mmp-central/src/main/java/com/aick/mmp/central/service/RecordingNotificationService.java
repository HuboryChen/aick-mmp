package com.aick.mmp.central.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 录像服务通知服务
 * 通过 Kafka 消息通知录像服务开始/停止录像
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingNotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String RECORDING_TOPIC = "mmp-recording-commands";

    @Value("${notification.recording.enabled:true}")
    private boolean recordingNotificationEnabled;

    @Value("${notification.recording.topic:mmp-recording-commands}")
    private String recordingTopic;

    /**
     * 发送开始录像命令
     *
     * @param cameraId     摄像头ID
     * @param cameraName   摄像头名称
     * @param streamSessionId 流会话ID
     * @param startTime    开始时间
     * @param config       额外配置（如录像格式、质量等）
     */
    public void sendStartRecordingCommand(Long cameraId, String cameraName,
                                          String streamSessionId, LocalDateTime startTime,
                                          Map<String, Object> config) {
        if (!recordingNotificationEnabled) {
            log.info("Recording notification disabled, skipping start recording command for camera {}", cameraId);
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", "START_RECORDING");
            message.put("messageId", UUID.randomUUID().toString());
            message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            message.put("payload", Map.of(
                    "cameraId", cameraId,
                    "cameraName", cameraName != null ? cameraName : "Unknown Camera",
                    "streamSessionId", streamSessionId,
                    "startTime", startTime != null ? startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "config", config != null ? config : getDefaultRecordingConfig()
            ));

            String key = String.valueOf(cameraId);
            kafkaTemplate.send(recordingTopic, key, message);

            log.info("Sent START_RECORDING command to Kafka for camera {} with messageId {}",
                    cameraId, message.get("messageId"));

        } catch (Exception e) {
            log.error("Failed to send START_RECORDING command for camera {}: {}", cameraId, e.getMessage());
            throw new RuntimeException("Failed to send recording command", e);
        }
    }

    /**
     * 发送停止录像命令
     *
     * @param cameraId     摄像头ID
     * @param cameraName   摄像头名称
     * @param streamSessionId 流会话ID
     * @param stopTime     停止时间
     */
    public void sendStopRecordingCommand(Long cameraId, String cameraName, String streamSessionId, LocalDateTime stopTime) {
        if (!recordingNotificationEnabled) {
            log.info("Recording notification disabled, skipping stop recording command for camera {}", cameraId);
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", "STOP_RECORDING");
            message.put("messageId", UUID.randomUUID().toString());
            message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            message.put("payload", Map.of(
                    "cameraId", cameraId,
                    "cameraName", cameraName != null ? cameraName : "Unknown Camera",
                    "streamSessionId", streamSessionId,
                    "stopTime", stopTime != null ? stopTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));

            String key = String.valueOf(cameraId);
            kafkaTemplate.send(recordingTopic, key, message);

            log.info("Sent STOP_RECORDING command to Kafka for camera {} with messageId {}",
                    cameraId, message.get("messageId"));

        } catch (Exception e) {
            log.error("Failed to send STOP_RECORDING command for camera {}: {}", cameraId, e.getMessage());
            throw new RuntimeException("Failed to send recording command", e);
        }
    }

    /**
     * 获取默认录像配置
     */
    private Map<String, Object> getDefaultRecordingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("format", "mp4");
        config.put("videoCodec", "h264");
        config.put("audioCodec", "aac");
        config.put("quality", "high");
        config.put("maxDuration", 3600); // 最大录制时长（秒）
        config.put("maxFileSize", 512); // 最大文件大小（MB）
        return config;
    }

    /**
     * 查询录像回放 URL
     * 通过 HTTP 调用录像服务获取指定时间范围的录像 URL
     */
    public String queryRecordingPlaybackUrl(Long cameraId, String cameraName,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 构建查询参数
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("cameraId", cameraId);
            queryParams.put("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            queryParams.put("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 实际项目中，这里应该调用录像服务的 HTTP API
            // 例如: http://recording-service:8081/api/v1/recordings/playback-url
            // String recordingServiceUrl = "http://recording-service:8081/api/v1/recordings/playback-url";

            // 返回模拟的录像回放 URL
            // 实际实现中应该从录像服务获取真实 URL
            String playbackUrl = buildPlaybackUrl(cameraId, cameraName, startTime, endTime);

            log.info("Generated playback URL for camera {} from {} to {}: {}",
                    cameraId, startTime, endTime, playbackUrl);

            return playbackUrl;

        } catch (Exception e) {
            log.error("Failed to query recording playback URL for camera {}: {}", cameraId, e.getMessage());
            throw new RuntimeException("Failed to query recording playback URL", e);
        }
    }

    /**
     * 构建录像回放 URL
     * 实际项目中应该从录像服务获取真实 URL
     */
    private String buildPlaybackUrl(Long cameraId, String cameraName,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        // 使用统一的录像回放 API
        // 前端通过此 URL 获取录像数据
        return String.format("/api/v1/recordings/playback/%d?start=%s&end=%s",
                cameraId,
                startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * 发送录像状态查询命令
     */
    public Map<String, Object> queryRecordingStatus(Long cameraId, String streamSessionId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", "QUERY_STATUS");
            message.put("messageId", UUID.randomUUID().toString());
            message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            message.put("payload", Map.of(
                    "cameraId", cameraId,
                    "streamSessionId", streamSessionId
            ));

            String key = String.valueOf(cameraId);
            kafkaTemplate.send(recordingTopic, key, message);

            log.info("Sent QUERY_STATUS command to Kafka for camera {} with messageId {}",
                    cameraId, message.get("messageId"));

            // 返回查询状态（实际项目中应该等待响应）
            Map<String, Object> status = new HashMap<>();
            status.put("messageId", message.get("messageId"));
            status.put("cameraId", cameraId);
            status.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            status.put("message", "Status query sent, response will be delivered asynchronously");

            return status;

        } catch (Exception e) {
            log.error("Failed to send QUERY_STATUS command for camera {}: {}", cameraId, e.getMessage());
            throw new RuntimeException("Failed to query recording status", e);
        }
    }
}
