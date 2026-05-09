package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating API key status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyStatusRequestDTO {
    
    @NotNull(message = "Status is required")
    private ApiKeyStatus status;
}
