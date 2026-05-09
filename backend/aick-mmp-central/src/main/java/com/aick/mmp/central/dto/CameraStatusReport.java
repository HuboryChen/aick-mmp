package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Camera Status Report DTO for edge node status bidirectional sync.
 * This DTO is used by the central server to receive camera status updates from edge nodes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraStatusReport {
    
    /**
     * Unique identifier for the camera
     */
    @NotBlank(message = "Camera ID cannot be blank")
    private String cameraId;
    
    /**
     * Current camera status (ONLINE/OFFLINE/ERROR/CONNECTING/MAINTENANCE/PENDING_ALLOCATION)
     */
    @NotNull(message = "Status cannot be null")
    private Camera.CameraStatus status;
    
    /**
     * Whether recording is currently enabled for this camera
     */
    private boolean recordingEnabled;
    
    /**
     * Current bitrate in kbps (null if not available)
     */
    private Integer currentBitrate;
    
    /**
     * Current frames per second (null if not available)
     */
    private Double currentFps;
    
    /**
     * Video encoding format (e.g., H.264, H.265)
     */
    private String encodingFormat;
    
    /**
     * Last heartbeat timestamp from the camera
     */
    private LocalDateTime lastHeartbeat;
    
    /**
     * Total storage used by this camera in bytes
     */
    private Long storageUsed;
    
    /**
     * Available storage for this camera in bytes
     */
    private Long storageAvailable;
    
    /**
     * Error code if camera is in ERROR status
     */
    private String errorCode;
    
    /**
     * Error message if camera is in ERROR status
     */
    private String errorMessage;
}
