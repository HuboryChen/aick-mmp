package com.aick.mmp.edge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Edge node API key authentication.
 */
@Configuration
@ConfigurationProperties(prefix = "api-key")
@EnableConfigurationProperties(ApiKeyConfig.class)
@Data
public class ApiKeyConfig {
    
    /**
     * Access Key (AK) for API authentication
     */
    private String accessKey;
    
    /**
     * Secret Key (SK) for API authentication
     */
    private String secretKey;
    
    /**
     * Timestamp tolerance in seconds (default: 5 minutes)
     */
    private int timestampToleranceSeconds = 300;
}
