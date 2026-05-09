package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频墙偏好设置数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoWallPreferencesDTO {
    
    private Long id;
    
    /**
     * 默认布局模式 (如: 1, 4, 9, 16)
     */
    private String layout;
    
    /**
     * 默认画质 (如: 480p, 720p, 1080p)
     */
    private String quality;
    
    /**
     * 默认码率 (kbps)
     */
    private Integer bitrate;
    
    /**
     * 摄像头ID列表
     */
    private List<Long> cameraIds;
    
    /**
     * 是否自动应用上次设置
     */
    private Boolean autoApply;
    
    /**
     * 上次使用的预设ID
     */
    private Long lastPresetId;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
