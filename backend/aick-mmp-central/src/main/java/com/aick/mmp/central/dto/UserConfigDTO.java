package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfigDTO {
    
    private Long id;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 配置值 (JSON格式)
     */
    private String configValue;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
