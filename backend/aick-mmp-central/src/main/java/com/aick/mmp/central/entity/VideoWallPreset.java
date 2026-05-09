package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 视频墙预设实体类
 * 用于存储用户的视频墙预设配置，包括布局、画质、码率、摄像头列表等
 */
@Entity
@Table(name = "video_wall_presets", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "preset_name"}),
       indexes = {
           @Index(name = "idx_preset_user", columnList = "user_id"),
           @Index(name = "idx_preset_user_default", columnList = "user_id, is_default")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoWallPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 预设名称
     */
    @Column(name = "preset_name", nullable = false, length = 50)
    private String presetName;

    /**
     * 布局模式 (如: 1, 4, 9, 16)
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String layout = "4";

    /**
     * 画质 (如: 480p, 720p, 1080p)
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String quality = "720p";

    /**
     * 码率 (kbps)
     */
    @Column
    @Builder.Default
    private Integer bitrate = 2048;

    /**
     * 摄像头ID列表 (JSON格式)
     */
    @Column(name = "camera_ids", columnDefinition = "TEXT")
    private String cameraIds;

    /**
     * 是否为默认预设
     */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

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
}
