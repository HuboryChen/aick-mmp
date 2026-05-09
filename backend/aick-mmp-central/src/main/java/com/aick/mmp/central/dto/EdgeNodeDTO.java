package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.EdgeNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EdgeNodeDTO {
    private Long id;

    private String uuid;

    @NotBlank(message = "Node name is required")
    @Size(max = 100, message = "Node name cannot exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    /**
     * Associated region ID for hierarchical management
     */
    private Long regionId;

    /**
     * Associated region name (for display)
     */
    private String regionName;

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @NotNull(message = "Port is required")
    @Positive(message = "Port must be a positive number")
    private Integer port;

    private EdgeNode.NodeStatus status;
    private LocalDateTime lastHeartbeatTime;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double storageUsage;
    private Integer maxCameraSupport;
    private Integer currentCameraCount;
    private String softwareVersion;
    private String hardwareInfo;
    private String networkBandwidth;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}