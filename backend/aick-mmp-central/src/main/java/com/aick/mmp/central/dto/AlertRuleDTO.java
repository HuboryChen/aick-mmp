package com.aick.mmp.central.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AlertRuleDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Type is required")
    private String type;

    private Boolean enabled;

    @NotNull(message = "Threshold value is required")
    private Integer thresholdValue;
}