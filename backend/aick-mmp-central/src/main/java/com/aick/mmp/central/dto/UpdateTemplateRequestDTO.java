package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequestDTO {
    private String brand;
    private String model;
    private String protocol;
    private Integer defaultPort;
    private String urlPathTemplate;
    private String presetParameters;
}
