package com.aick.mmp.dto;

import com.aick.mmp.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

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