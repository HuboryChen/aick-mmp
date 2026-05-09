package com.aick.mmp.central.dto;

import com.aick.mmp.central.entity.MotionEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 移动侦测事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotionEventDTO {

    /**
     * 摄像头ID
     */
    @NotNull(message = "摄像头ID不能为空")
    private Long cameraId;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 事件持续时长（秒）
     */
    private Integer durationSeconds;

    /**
     * 移动区域（JSON格式）
     */
    private String detectionArea;

    /**
     * 移动强度/置信度 (0-100)
     */
    private Integer intensity;

    /**
     * 事件类型
     */
    private MotionEvent.EventType eventType;

    /**
     * 是否触发了录像
     */
    private Boolean triggeredRecording;

    /**
     * 关联的录像ID
     */
    private Long recordingId;

    /**
     * 事件元数据（JSON格式）
     */
    private String metadata;

    /**
     * 边缘节点ID
     */
    private Long edgeNodeId;
}
