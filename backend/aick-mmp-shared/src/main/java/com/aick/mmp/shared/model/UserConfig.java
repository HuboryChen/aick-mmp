package com.aick.mmp.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户配置实体
 * 用于存储用户的个性化配置，如视频墙布局、画质等
 */
@Entity
@Table(name = "user_configs", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "config_key"}),
       indexes = {
           @Index(name = "idx_user_config_key", columnList = "user_id, config_key")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 配置键
     * 如: VIDEO_WALL_LAYOUT, VIDEO_WALL_QUALITY, VIDEO_WALL_CAMERAS
     */
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;
    
    /**
     * 配置值 (JSON格式存储)
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    /**
     * 配置描述
     */
    @Column(name = "description", length = 255)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 配置键枚举
     */
    public static final class ConfigKeys {
        public static final String VIDEO_WALL_LAYOUT = "VIDEO_WALL_LAYOUT";
        public static final String VIDEO_WALL_QUALITY = "VIDEO_WALL_QUALITY";
        public static final String VIDEO_WALL_CAMERAS = "VIDEO_WALL_CAMERAS";
        public static final String VIDEO_WALL_CONFIG = "VIDEO_WALL_CONFIG"; // 综合配置
        
        private ConfigKeys() {}
    }
}
