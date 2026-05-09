package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.OwnerType;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for system app responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAppDTO {
    
    private Long id;
    private String appKey;
    private String name;
    private String description;
    private OwnerType ownerType;
    private Long ownerId;
    private String status;
    private Set<SystemAppPermission> permissions;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Credential status
    private Boolean hasCredentials;
    
    // Computed fields
    private Integer edgeNodeCount;
}
