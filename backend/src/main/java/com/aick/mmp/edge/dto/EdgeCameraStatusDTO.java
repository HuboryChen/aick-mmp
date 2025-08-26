package com.aick.mmp.edge.dto;

import com.aick.mmp.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Edge Camera Status DTO - camera status information for reporting to central services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeCameraStatusDTO {
    private Long cameraId;
    private String edgeNodeId;
    private Camera.CameraStatus status;
    private LocalDateTime lastActiveTime;
    private boolean isConnected;
    private double networkLatency;
    private String qualityLevel;
    private int retryCount;
    private String errorMessage;
    
    // Performance metrics
    private Map<String, Object> performanceMetrics;
    private double cpuUsage;
    private double memoryUsage;
    private double bandwidthUsage;
    
    // Stream metrics
    private String currentStreamId;
    private double currentBitrate;
    private double currentFrameRate;
    private String currentResolution;
    private LocalDateTime lastStreamActivity;
}