package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for API key responses (excludes secret key).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDTO {
    
    private Long id;
    private String accessKey;
    private String name;
    private ApiKeyType type;
    private ApiKeyStatus status;
    private Long appId;
    private String appName;
    private Long userId;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
