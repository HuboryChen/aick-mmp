package com.aick.mmp.shared.dto;

import com.aick.mmp.shared.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraDTO {
    private Long id;

    @NotBlank(message = "Camera name is required")
    @Size(max = 100, message = "Camera name cannot exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String location;

    /**
     * Associated region ID for hierarchical management
     */
    private Long regionId;

    /**
     * Associated region name (for display)
     */
    private String regionName;

    private Long edgeNodeId;
    private String edgeNodeName;

    @NotNull(message = "Protocol is required")
    private Camera.Protocol protocol;

    @NotBlank(message = "Connection URL is required")
    private String connectionUrl;

    private String username;
    private String password;
    private String resolution;
    private Integer frameRate;
    private Integer bitrate;
    private Camera.CameraStatus status;
    private LocalDateTime lastActiveTime;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}