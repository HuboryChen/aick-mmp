package com.aick.mmp.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stream_sessions")
public class StreamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    @Column(name = "edge_node_status")
    private String edgeNodeStatus;

    @Column(name = "cdn_node_id")
    private String cdnNodeId;

    @Transient
    private Map<String, Double> networkMetrics;

    @Column(name = "has_motion_detected")
    private boolean hasMotionDetected = false;

    @Column(name = "ai_event_count")
    private int aiEventCount = 0;

    @Column(name = "motion_detection_enabled")
    private boolean motionDetectionEnabled = false;

    @Column(name = "ai_processing_enabled")
    private boolean aiProcessingEnabled = false;

    @Column(name = "last_network_update")
    private LocalDateTime lastNetworkUpdate;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Column(name = "stream_url")
    private String streamUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StreamStatus status;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Column(name = "resolution")
    private String resolution;

    // 添加缺失的字段
    @Column(name = "frame_rate")
    private Integer frameRate;

    @Column(name = "bytes_transferred")
    private Long bytesTransferred = 0L;

    @Column(name = "is_recording")
    private Boolean isRecording = false;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 添加 startedAt 字段以匹配 StreamingServiceImpl 中的使用
    @Transient
    private Long startedAt;

    public enum StreamStatus {
        INITIATED, CONNECTING, STREAMING, PAUSED, DISCONNECTED, ERROR
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = StreamStatus.INITIATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
