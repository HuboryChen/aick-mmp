package com.aick.mmp.central.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 视频质量更新请求
 */
@Data
public class StreamQualityRequest {
    
    @NotNull(message = "Resolution cannot be null")
    private String resolution;
    
    @NotNull(message = "Bitrate cannot be null")
    @Min(value = 100, message = "Bitrate must be at least 100")
    @Max(value = 20000, message = "Bitrate cannot exceed 20000")
    private Integer bitrate;
    
    @NotNull(message = "Frame rate cannot be null")
    @Min(value = 1, message = "Frame rate must be at least 1")
    @Max(value = 60, message = "Frame rate cannot exceed 60")
    private Integer frameRate;
}
