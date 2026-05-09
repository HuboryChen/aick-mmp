package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 移动侦测事件实体类
 * 用于记录摄像头检测到的移动事件
 */
@Entity
@Table(name = "motion_events", indexes = {
    @Index(name = "idx_motion_event_camera", columnList = "camera_id"),
    @Index(name = "idx_motion_event_time", columnList = "event_time"),
    @Index(name = "idx_motion_event_camera_time", columnList = "camera_id, event_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 摄像头ID
     */
    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    /**
     * 事件时间
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    /**
     * 事件持续时长（秒）
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * 移动区域（JSON格式，描述触发检测的区域坐标）
     */
    @Column(name = "detection_area", columnDefinition = "TEXT")
    private String detectionArea;

    /**
     * 移动强度/置信度 (0-100)
     */
    @Column(name = "intensity")
    private Integer intensity;

    /**
     * 事件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20)
    @Builder.Default
    private EventType eventType = EventType.MOTION;

    /**
     * 是否触发了录像
     */
    @Column(name = "triggered_recording")
    @Builder.Default
    private Boolean triggeredRecording = false;

    /**
     * 关联的录像ID（如果有）
     */
    @Column(name = "recording_id")
    private Long recordingId;

    /**
     * 事件元数据（JSON格式，存储额外信息如移动速度、方向等）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 边缘节点ID
     */
    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 一般移动
         */
        MOTION,
        
        /**
         * 强移动
         */
        STRONG_MOTION,
        
        /**
         * 持续移动
         */
        CONTINUOUS_MOTION,
        
        /**
         * 区域入侵
         */
        INTRUSION,
        
        /**
         * 徘徊检测
         */
        LOITERING
    }
}
