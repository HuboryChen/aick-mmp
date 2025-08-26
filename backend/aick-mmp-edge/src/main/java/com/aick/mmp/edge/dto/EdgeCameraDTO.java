package com.aick.mmp.edge.dto;

import com.aick.mmp.shared.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Edge Camera DTO - simplified camera information for edge nodes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeCameraDTO {
    private Long id;
    private String name;
    private String location;
    private String connectionUrl;
    private String username;
    private String password;
    private Camera.Protocol protocol;
    private Camera.CameraStatus status;
    private String resolution;
    private Integer frameRate;
    private Integer bitrate;
    private boolean enabled;
    private LocalDateTime lastActiveTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Edge-specific fields
    private String localStreamUrl;
    private String edgeNodeId;
    private boolean isConnected;
    private double networkLatency;
    private String qualityLevel;
    private int retryCount;
}