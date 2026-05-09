package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO for API key creation requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequestDTO {
    
    @NotBlank(message = "Key name is required")
    @Size(max = 100, message = "Key name must not exceed 100 characters")
    private String name;
    
    /**
     * For system-level keys: the app ID to associate with
     */
    private Long appId;
    
    /**
     * Optional expiration date
     */
    private LocalDateTime expiresAt;
}
