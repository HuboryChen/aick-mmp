package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.SystemAppPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * DTO for system app creation requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSystemAppRequestDTO {
    
    @NotBlank(message = "App name is required")
    @Size(max = 100, message = "App name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    /**
     * Permissions to grant to this app
     */
    private Set<SystemAppPermission> permissions;
    
    /**
     * Owner type: SYSTEM or USER
     */
    private String ownerType;
    
    /**
     * Owner ID (required if ownerType is USER)
     */
    private Long ownerId;
}
