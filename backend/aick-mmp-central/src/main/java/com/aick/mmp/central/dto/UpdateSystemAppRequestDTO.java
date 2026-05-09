package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.SystemAppPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for system app update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemAppRequestDTO {
    
    private String name;
    private String description;
    private Set<SystemAppPermission> permissions;
    private String status;
}
