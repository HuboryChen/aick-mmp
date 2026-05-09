package com.aick.mmp.edge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Extended Heartbeat Request DTO that includes camera status reports.
 * This is used by edge nodes to send both system metrics and camera statuses to the central server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeHeartbeatRequest {
    
    /**
     * Camera status reports list
     */
    private List<CameraStatusReportDTO> cameraStatuses;
    
    /**
     * Number of cameras currently managed by this edge node
     */
    private Integer managedCameraCount;
    
    /**
     * Number of cameras currently streaming
     */
    private Integer activeStreamCount;
    
    /**
     * Number of cameras currently recording
     */
    private Integer recordingCount;
    
    /**
     * Total bandwidth used by all cameras in Mbps
     */
    private Double totalBandwidthMbps;
    
    /**
     * Camera status report DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CameraStatusReportDTO {
        
        /**
         * Camera ID
         */
        private String cameraId;
        
        /**
         * Camera status (ONLINE/OFFLINE/ERROR/CONNECTING/MAINTENANCE)
         */
        private String status;
        
        /**
         * Whether recording is enabled
         */
        private boolean recordingEnabled;
        
        /**
         * Current bitrate in kbps
         */
        private Integer currentBitrate;
        
        /**
         * Current frames per second
         */
        private Double currentFps;
        
        /**
         * Video encoding format (H.264, H.265, etc.)
         */
        private String encodingFormat;
        
        /**
         * Error code if status is ERROR
         */
        private String errorCode;
        
        /**
         * Error message if status is ERROR
         */
        private String errorMessage;
        
        /**
         * Storage used by this camera in bytes
         */
        private Long storageUsed;
        
        /**
         * Available storage for this camera in bytes
         */
        private Long storageAvailable;
    }
}
