package com.aick.mmp.edge.service.impl;

import com.aick.mmp.shared.adapter.protocol.ProtocolAdapter;
import com.aick.mmp.shared.adapter.protocol.ProtocolAdapterFactory;
import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.EdgeCameraDTO;
import com.aick.mmp.edge.dto.EdgeCameraStatusDTO;
import com.aick.mmp.edge.service.EdgeCameraService;
import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeCameraServiceImpl implements EdgeCameraService {

    private final EdgeNodeConfig edgeNodeConfig;
    private final ProtocolAdapterFactory protocolAdapterFactory;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;
    
    // Local camera storage (in a real implementation, this might be persisted)
    private final Map<Long, EdgeCameraDTO> localCameras = new ConcurrentHashMap<>();
    private final Map<Long, EdgeCameraStatusDTO> cameraStatuses = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing EdgeCameraService for node: {}", edgeNodeConfig.getNodeId());
        initializeCameras();
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down EdgeCameraService");
        shutdownCameras();
    }

    @Override
    public List<EdgeCameraDTO> getAllCameras() {
        return localCameras.values().stream().collect(Collectors.toList());
    }

    @Override
    public Optional<EdgeCameraDTO> getCameraById(Long cameraId) {
        return Optional.ofNullable(localCameras.get(cameraId));
    }

    @Override
    public EdgeCameraDTO addCamera(EdgeCameraDTO cameraDTO) {
        log.info("Adding camera: {} to edge node: {}", cameraDTO.getName(), edgeNodeConfig.getNodeId());
        
        cameraDTO.setEdgeNodeId(edgeNodeConfig.getNodeId());
        cameraDTO.setCreatedAt(LocalDateTime.now());
        cameraDTO.setUpdatedAt(LocalDateTime.now());
        cameraDTO.setEnabled(true);
        cameraDTO.setStatus(Camera.CameraStatus.CONNECTING);
        
        localCameras.put(cameraDTO.getId(), cameraDTO);
        
        // Initialize camera status
        EdgeCameraStatusDTO status = EdgeCameraStatusDTO.builder()
                .cameraId(cameraDTO.getId())
                .edgeNodeId(edgeNodeConfig.getNodeId())
                .status(Camera.CameraStatus.CONNECTING)
                .lastActiveTime(LocalDateTime.now())
                .isConnected(false)
                .retryCount(0)
                .performanceMetrics(new HashMap<>())
                .build();
        
        cameraStatuses.put(cameraDTO.getId(), status);
        
        // Test connection asynchronously
        testCameraConnectionAsync(cameraDTO.getId());
        
        return cameraDTO;
    }

    @Override
    public EdgeCameraDTO updateCamera(Long cameraId, EdgeCameraDTO cameraDTO) {
        log.info("Updating camera: {} on edge node: {}", cameraId, edgeNodeConfig.getNodeId());
        
        EdgeCameraDTO existingCamera = localCameras.get(cameraId);
        if (existingCamera == null) {
            throw new RuntimeException("Camera not found: " + cameraId);
        }
        
        // Update fields
        existingCamera.setName(cameraDTO.getName());
        existingCamera.setLocation(cameraDTO.getLocation());
        existingCamera.setConnectionUrl(cameraDTO.getConnectionUrl());
        existingCamera.setUsername(cameraDTO.getUsername());
        existingCamera.setPassword(cameraDTO.getPassword());
        existingCamera.setProtocol(cameraDTO.getProtocol());
        existingCamera.setResolution(cameraDTO.getResolution());
        existingCamera.setFrameRate(cameraDTO.getFrameRate());
        existingCamera.setBitrate(cameraDTO.getBitrate());
        existingCamera.setEnabled(cameraDTO.isEnabled());
        existingCamera.setUpdatedAt(LocalDateTime.now());
        
        // Test connection with new settings
        testCameraConnectionAsync(cameraId);
        
        return existingCamera;
    }

    @Override
    public void removeCamera(Long cameraId) {
        log.info("Removing camera: {} from edge node: {}", cameraId, edgeNodeConfig.getNodeId());
        
        EdgeCameraDTO camera = localCameras.remove(cameraId);
        cameraStatuses.remove(cameraId);
        
        if (camera != null) {
            // Stop any active streams
            stopCameraStreams(cameraId);
        }
    }

    @Override
    public boolean testCameraConnection(Long cameraId) {
        EdgeCameraDTO camera = localCameras.get(cameraId);
        if (camera == null) {
            return false;
        }
        
        try {
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
            if (adapter == null) {
                log.warn("No protocol adapter found for: {}", camera.getProtocol());
                return false;
            }
            
            Camera cameraEntity = convertToEntity(camera);
            boolean connected = adapter.testConnection(cameraEntity);
            
            updateCameraConnectionStatus(cameraId, connected);
            
            return connected;
        } catch (Exception e) {
            log.error("Error testing camera connection for camera {}: {}", cameraId, e.getMessage());
            updateCameraConnectionStatus(cameraId, false);
            return false;
        }
    }

    @Override
    public EdgeCameraStatusDTO getCameraStatus(Long cameraId) {
        return cameraStatuses.get(cameraId);
    }

    @Override
    public void updateCameraStatus(Long cameraId, Camera.CameraStatus status, String errorMessage) {
        EdgeCameraDTO camera = localCameras.get(cameraId);
        EdgeCameraStatusDTO statusDTO = cameraStatuses.get(cameraId);
        
        if (camera != null) {
            camera.setStatus(status);
            camera.setLastActiveTime(LocalDateTime.now());
        }
        
        if (statusDTO != null) {
            statusDTO.setStatus(status);
            statusDTO.setErrorMessage(errorMessage);
            statusDTO.setLastActiveTime(LocalDateTime.now());
        }
    }

    @Override
    public List<EdgeCameraDTO> getCamerasByStatus(Camera.CameraStatus status) {
        return localCameras.values().stream()
                .filter(camera -> camera.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public long getOnlineCamerasCount() {
        return localCameras.values().stream()
                .filter(camera -> camera.getStatus() == Camera.CameraStatus.ONLINE)
                .count();
    }

    @Override
    public List<EdgeCameraStatusDTO> getAllCameraStatuses() {
        return cameraStatuses.values().stream().collect(Collectors.toList());
    }

    @Override
    public void initializeCameras() {
        log.info("Initializing cameras for edge node: {}", edgeNodeConfig.getNodeId());
        
        // In a real implementation, load cameras from configuration or local database
        // For now, we'll load from central server
        loadCamerasFromCentral();
    }

    @Override
    public void shutdownCameras() {
        log.info("Shutting down all camera connections");
        
        localCameras.keySet().forEach(this::stopCameraStreams);
        localCameras.clear();
        cameraStatuses.clear();
    }

    @Override
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void reconnectFailedCameras() {
        log.debug("Checking for failed cameras to reconnect");
        
        List<EdgeCameraDTO> failedCameras = getCamerasByStatus(Camera.CameraStatus.ERROR);
        for (EdgeCameraDTO camera : failedCameras) {
            log.info("Attempting to reconnect camera: {}", camera.getName());
            testCameraConnectionAsync(camera.getId());
        }
    }

    @Override
    public String getCameraStreamUrl(Long cameraId) {
        EdgeCameraDTO camera = localCameras.get(cameraId);
        if (camera != null && camera.getLocalStreamUrl() != null) {
            return camera.getLocalStreamUrl();
        }
        return null;
    }

    @Override
    public void updateCameraCredentials(Long cameraId, String username, String password) {
        EdgeCameraDTO camera = localCameras.get(cameraId);
        if (camera != null) {
            camera.setUsername(username);
            camera.setPassword(password);
            camera.setUpdatedAt(LocalDateTime.now());
            
            // Test connection with new credentials
            testCameraConnectionAsync(cameraId);
        }
    }

    // Private helper methods
    
    private void testCameraConnectionAsync(Long cameraId) {
        // In a real implementation, this would be executed in a separate thread
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate connection test delay
                boolean connected = testCameraConnection(cameraId);
                if (connected) {
                    updateCameraStatus(cameraId, Camera.CameraStatus.ONLINE, null);
                } else {
                    updateCameraStatus(cameraId, Camera.CameraStatus.ERROR, "Connection failed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void updateCameraConnectionStatus(Long cameraId, boolean connected) {
        EdgeCameraStatusDTO status = cameraStatuses.get(cameraId);
        if (status != null) {
            status.setConnected(connected);
            status.setLastActiveTime(LocalDateTime.now());
            if (!connected) {
                status.setRetryCount(status.getRetryCount() + 1);
            } else {
                status.setRetryCount(0);
            }
        }
    }

    private void stopCameraStreams(Long cameraId) {
        // In a real implementation, stop any active streams for this camera
        log.info("Stopping streams for camera: {}", cameraId);
    }

    private void loadCamerasFromCentral() {
        try {
            String centralUrl = edgeNodeConfig.getCentralServerUrl() + "/api/cameras/edge-node/" + edgeNodeConfig.getNodeId();
            ResponseEntity<EdgeCameraDTO[]> response = restTemplate.getForEntity(centralUrl, EdgeCameraDTO[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (EdgeCameraDTO camera : response.getBody()) {
                    localCameras.put(camera.getId(), camera);
                    
                    EdgeCameraStatusDTO status = EdgeCameraStatusDTO.builder()
                            .cameraId(camera.getId())
                            .edgeNodeId(edgeNodeConfig.getNodeId())
                            .status(camera.getStatus())
                            .lastActiveTime(LocalDateTime.now())
                            .isConnected(false)
                            .retryCount(0)
                            .performanceMetrics(new HashMap<>())
                            .build();
                    
                    cameraStatuses.put(camera.getId(), status);
                }
                
                log.info("Loaded {} cameras from central server", localCameras.size());
            }
        } catch (Exception e) {
            log.error("Failed to load cameras from central server: {}", e.getMessage());
        }
    }

    private Camera convertToEntity(EdgeCameraDTO dto) {
        return modelMapper.map(dto, Camera.class);
    }
}