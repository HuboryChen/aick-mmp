package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for Edge node registration request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeRegisterRequestDTO {
    
    @NotBlank(message = "Node name is required")
    private String name;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotBlank(message = "IP address is required")
    private String ipAddress;
    
    @NotNull(message = "Port is required")
    @Positive(message = "Port must be positive")
    private Integer port;
    
    @NotNull(message = "Max camera support is required")
    @Positive(message = "Max camera support must be positive")
    private Integer maxCameraSupport;
}
