package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频墙预设数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoWallPresetDTO {
    
    private Long id;
    
    /**
     * 预设名称
     */
    private String presetName;
    
    /**
     * 布局模式 (如: 1, 4, 9, 16)
     */
    private String layout;
    
    /**
     * 画质 (如: 480p, 720p, 1080p)
     */
    private String quality;
    
    /**
     * 码率 (kbps)
     */
    private Integer bitrate;
    
    /**
     * 摄像头ID列表
     */
    private List<Long> cameraIds;
    
    /**
     * 是否为默认预设
     */
    private Boolean isDefault;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
