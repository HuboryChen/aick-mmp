package com.aick.mmp.central.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RecordingDTO {
    private Long id;
    private Long cameraId;
    private String cameraName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration; // in seconds
    private Long size; // in bytes
    private String quality;
    private String storagePath;
    private LocalDateTime createdAt;

    // ========== 录像元数据增强 ==========
    private String status; // 录像状态: PENDING, RECORDING, COMPLETED, CORRUPTED, DELETED
    private String recordingType; // 录像类型: continuous, timed, motion, event
    private String format; // 录像格式: mp4, avi, mkv
    private String md5; // MD5校验码
    private String integrityStatus; // 完整性状态: PENDING, COMPLETED, CORRUPTED, DELETED
    private Boolean lockStatus; // 锁定状态
    private LocalDateTime updatedAt;

    // ========== 软删除支持字段 ==========
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime orphanedAt;
}