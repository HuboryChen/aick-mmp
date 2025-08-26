package com.aick.mmp.edge.service;

import com.aick.mmp.edge.dto.EdgeCameraDTO;
import com.aick.mmp.edge.dto.EdgeCameraStatusDTO;
import com.aick.mmp.shared.model.Camera;

import java.util.List;
import java.util.Optional;

/**
 * Edge Camera Service - handles local camera management on edge nodes
 */
public interface EdgeCameraService {
    
    /**
     * Get all cameras managed by this edge node
     */
    List<EdgeCameraDTO> getAllCameras();
    
    /**
     * Get camera by ID
     */
    Optional<EdgeCameraDTO> getCameraById(Long cameraId);
    
    /**
     * Add a new camera to this edge node
     */
    EdgeCameraDTO addCamera(EdgeCameraDTO cameraDTO);
    
    /**
     * Update camera configuration
     */
    EdgeCameraDTO updateCamera(Long cameraId, EdgeCameraDTO cameraDTO);
    
    /**
     * Remove camera from this edge node
     */
    void removeCamera(Long cameraId);
    
    /**
     * Test camera connection
     */
    boolean testCameraConnection(Long cameraId);
    
    /**
     * Get camera status with metrics
     */
    EdgeCameraStatusDTO getCameraStatus(Long cameraId);
    
    /**
     * Update camera status
     */
    void updateCameraStatus(Long cameraId, Camera.CameraStatus status, String errorMessage);
    
    /**
     * Get cameras by status
     */
    List<EdgeCameraDTO> getCamerasByStatus(Camera.CameraStatus status);
    
    /**
     * Get online cameras count
     */
    long getOnlineCamerasCount();
    
    /**
     * Get all camera statuses for reporting to central server
     */
    List<EdgeCameraStatusDTO> getAllCameraStatuses();
    
    /**
     * Initialize cameras on edge node startup
     */
    void initializeCameras();
    
    /**
     * Shutdown all camera connections
     */
    void shutdownCameras();
    
    /**
     * Reconnect failed cameras
     */
    void reconnectFailedCameras();
    
    /**
     * Get camera stream URL
     */
    String getCameraStreamUrl(Long cameraId);
    
    /**
     * Update camera credentials
     */
    void updateCameraCredentials(Long cameraId, String username, String password);
}