package com.aick.mmp.edge.dto;

import com.aick.mmp.model.StreamSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Edge Stream DTO - simplified stream session information for edge nodes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeStreamDTO {
    private String sessionId;
    private Long cameraId;
    private String protocol;
    private StreamSession.StreamStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Edge-specific fields
    private String localStreamUrl;
    private String edgeNodeId;
    private int qualityLevel;
    private double bitrate;
    private double frameRate;
    private String resolution;
    private boolean isActive;
    private Map<String, Object> metrics;
    private String errorMessage;
    private int connectionRetries;
    private LocalDateTime lastHeartbeat;
}