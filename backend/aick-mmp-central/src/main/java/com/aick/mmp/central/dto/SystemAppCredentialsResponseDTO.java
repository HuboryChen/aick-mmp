package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for system app credentials response (one-time display).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAppCredentialsResponseDTO {
    
    /**
     * System app ID
     */
    private Long id;
    
    /**
     * App name
     */
    private String name;
    
    /**
     * App Key (Access Key) - starts with "ak_"
     */
    private String appKey;
    
    /**
     * App Secret (Secret Key) - starts with "sk_"
     * This is only shown once on creation/regeneration
     */
    private String appSecret;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Warning message for the user
     */
    private String warning;
}
