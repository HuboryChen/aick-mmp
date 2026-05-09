package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for API key creation response (includes secret key - one-time display).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreatedResponseDTO {
    
    /**
     * Access Key (AK) - can be stored and used
     */
    private String accessKey;
    
    /**
     * Secret Key (SK) - only displayed once during creation
     */
    private String secretKey;
    
    private String name;
    private LocalDateTime createdAt;
}
