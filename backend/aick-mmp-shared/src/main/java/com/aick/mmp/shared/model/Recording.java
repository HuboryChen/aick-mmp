package com.aick.mmp.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * 录像记录实体
 */
@Entity
@Table(name = "recordings", indexes = {
    @Index(name = "idx_recording_camera", columnList = "camera_id"),
    @Index(name = "idx_recording_start_time", columnList = "start_time"),
    @Index(name = "idx_recording_status", columnList = "status"),
    @Index(name = "idx_recordings_is_deleted", columnList = "is_deleted"),
    @Index(name = "idx_recordings_deleted_at", columnList = "is_deleted, deleted_at"),
    @Index(name = "idx_recordings_orphaned_at", columnList = "orphaned_at")
})
@Where(clause = "is_deleted = false")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 摄像头ID
     */
    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    /**
     * 录像名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 录像文件路径
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * 文件大小(字节)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MD5校验码
     */
    @Column(name = "md5", length = 32)
    private String md5;

    /**
     * 存储路径
     */
    @Column(name = "storage_path", length = 500)
    private String storagePath;

    /**
     * 完整性状态 (PENDING/COMPLETED/CORRUPTED/DELETED)
     */
    @Column(name = "integrity_status", length = 20)
    private String integrityStatus;

    /**
     * 锁定状态 (下载时锁定)
     */
    @Column(name = "lock_status")
    private Boolean lockStatus = false;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 录像时长(秒)
     */
    private Integer duration;

    /**
     * 录像类型(continuous/timed/motion/alert)
     */
    @Column(name = "recording_type", length = 20)
    private String recordingType;

    /**
     * 录像状态(recording/completed/failed/deleted)
     */
    @Column(length = 20)
    private String status;

    /**
     * 录像格式
     */
    @Column(length = 10)
    private String format;

    /**
     * 分辨率
     */
    @Column(length = 20)
    private String resolution;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== 软删除支持字段 ==========

    /**
     * 软删除标志（主查询条件）
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 删除时间（用于清理策略和审计）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 孤立时间（当关联摄像头被删除时标记）
     */
    @Column(name = "orphaned_at")
    private LocalDateTime orphanedAt;

    /**
     * 孤立原因（通常是关联的摄像头ID）
     */
    @Column(name = "orphaned_by")
    private Long orphanedBy;
}
