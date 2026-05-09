package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 视频墙偏好设置实体类
 * 用于存储用户的视频墙默认偏好设置
 */
@Entity
@Table(name = "video_wall_preferences",
       indexes = {
           @Index(name = "idx_preferences_user", columnList = "user_id", unique = true)
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoWallPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 默认布局模式 (如: 1, 4, 9, 16)
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String layout = "4";

    /**
     * 默认画质 (如: 480p, 720p, 1080p)
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String quality = "720p";

    /**
     * 默认码率 (kbps)
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
     * 是否自动应用上次设置
     */
    @Column(name = "auto_apply")
    @Builder.Default
    private Boolean autoApply = true;

    /**
     * 上次使用的预设ID
     */
    @Column(name = "last_preset_id")
    private Long lastPresetId;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
