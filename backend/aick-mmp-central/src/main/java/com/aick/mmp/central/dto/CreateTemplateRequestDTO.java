package com.aick.mmp.central.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequestDTO {
    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Protocol is required")
    private String protocol;

    @NotNull(message = "Default port is required")
    private Integer defaultPort;

    @NotBlank(message = "URL path template is required")
    private String urlPathTemplate;

    private String presetParameters;
}
