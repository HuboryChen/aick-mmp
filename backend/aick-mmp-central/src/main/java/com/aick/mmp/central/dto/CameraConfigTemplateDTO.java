package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraConfigTemplateDTO {
    private Long id;
    private String brand;
    private String model;
    private String protocol;
    private Integer defaultPort;
    private String urlPathTemplate;
    private String presetParameters;
    private Boolean isPreset;
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
