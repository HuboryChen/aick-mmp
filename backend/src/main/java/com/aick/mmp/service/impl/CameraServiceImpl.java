package com.aick.mmp.service.impl;

import com.aick.mmp.dto.CameraDTO;
import com.aick.mmp.dto.CameraStatusUpdateDTO;
import com.aick.mmp.exception.ResourceNotFoundException;
import com.aick.mmp.exception.ServiceException;
import com.aick.mmp.model.Camera;
import com.aick.mmp.model.EdgeNode;
import com.aick.mmp.repository.CameraRepository;
import com.aick.mmp.repository.EdgeNodeRepository;
import com.aick.mmp.service.CameraService;
import com.aick.mmp.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CameraServiceImpl implements CameraService {
    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final StreamingService streamingService;
    private final ModelMapper modelMapper;

    @Override
    public Page<CameraDTO> getAllCameras(Pageable pageable) {
        log.info("Fetching all cameras with pagination: {}", pageable);
        return cameraRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    @Override
    public Page<CameraDTO> getCamerasByLocation(String location, Pageable pageable) {
        log.info("Fetching cameras by location: {} with pagination: {}", location, pageable);
        return cameraRepository.findByLocation(location, pageable)
                .map(this::convertToDto);
    }

    @Override
    public Page<CameraDTO> getCamerasByEdgeNodeId(Long edgeNodeId, Pageable pageable) {
        log.info("Fetching cameras by edge node id: {} with pagination: {}", edgeNodeId, pageable);
        return cameraRepository.findByEdgeNodeId(edgeNodeId, pageable)
                .map(this::convertToDto);
    }

    @Override
    public Page<CameraDTO> getCamerasByStatus(Camera.CameraStatus status, Pageable pageable) {
        log.info("Fetching cameras by status: {} with pagination: {}", status, pageable);
        return cameraRepository.findByStatus(status, pageable)
                .map(this::convertToDto);
    }

    @Override
    public CameraDTO getCameraById(Long id) {
        log.info("Fetching camera with id: {}", id);
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        return convertToDto(camera);
    }

    @Override
    @Transactional
    public CameraDTO createCamera(CameraDTO cameraDTO) {
        log.info("Creating new camera: {}", cameraDTO.getName());

        // Validate edge node exists
        if (cameraDTO.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(cameraDTO.getEdgeNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + cameraDTO.getEdgeNodeId()));
        }

        // Check if camera with this URL already exists
        if (cameraRepository.existsByConnectionUrl(cameraDTO.getConnectionUrl())) {
            throw new ServiceException("Camera with this connection URL already exists");
        }

        Camera camera = convertToEntity(cameraDTO);
        camera.setStatus(Camera.CameraStatus.CONNECTING);
        camera.setEnabled(true);
        camera.setCreatedAt(LocalDateTime.now());
        camera.setUpdatedAt(LocalDateTime.now());

        Camera savedCamera = cameraRepository.save(camera);

        // Attempt initial connection
        try {
            boolean connectionSuccessful = testCameraConnection(savedCamera.getId());
            if (connectionSuccessful) {
                savedCamera.setStatus(Camera.CameraStatus.ONLINE);
                savedCamera.setLastActiveTime(LocalDateTime.now());
                cameraRepository.save(savedCamera);
            } else {
                savedCamera.setStatus(Camera.CameraStatus.OFFLINE);
                cameraRepository.save(savedCamera);
                log.warn("Initial connection to camera failed: {}", savedCamera.getName());
            }
        } catch (Exception e) {
            log.error("Error testing camera connection: {}", e.getMessage());
            savedCamera.setStatus(Camera.CameraStatus.ERROR);
            cameraRepository.save(savedCamera);
        }

        return convertToDto(savedCamera);
    }

    @Override
    @Transactional
    public CameraDTO updateCamera(Long id, CameraDTO cameraDTO) {
        log.info("Updating camera with id: {}", id);

        Camera existingCamera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        // Validate edge node if provided
        if (cameraDTO.getEdgeNodeId() != null && !cameraDTO.getEdgeNodeId().equals(existingCamera.getEdgeNodeId())) {
            edgeNodeRepository.findById(cameraDTO.getEdgeNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + cameraDTO.getEdgeNodeId()));
        }

        // Update fields
        existingCamera.setName(cameraDTO.getName());
        existingCamera.setLocation(cameraDTO.getLocation());
        existingCamera.setEdgeNodeId(cameraDTO.getEdgeNodeId());
        existingCamera.setProtocol(cameraDTO.getProtocol());
        existingCamera.setResolution(cameraDTO.getResolution());
        existingCamera.setFrameRate(cameraDTO.getFrameRate());
        existingCamera.setBitrate(cameraDTO.getBitrate());
        existingCamera.setEnabled(cameraDTO.isEnabled());
        existingCamera.setUpdatedAt(LocalDateTime.now());

        // If URL changed, check for duplicates
        if (!existingCamera.getConnectionUrl().equals(cameraDTO.getConnectionUrl())) {
            if (cameraRepository.existsByConnectionUrl(cameraDTO.getConnectionUrl())) {
                throw new ServiceException("Camera with this connection URL already exists");
            }
            existingCamera.setConnectionUrl(cameraDTO.getConnectionUrl());
            existingCamera.setStatus(Camera.CameraStatus.CONNECTING);
        }

        // Update credentials if provided
        if (cameraDTO.getUsername() != null) {
            existingCamera.setUsername(cameraDTO.getUsername());
        }
        if (cameraDTO.getPassword() != null) {
            existingCamera.setPassword(cameraDTO.getPassword());
        }

        Camera updatedCamera = cameraRepository.save(existingCamera);
        return convertToDto(updatedCamera);
    }

    @Override
    @Transactional
    public void updateCameraStatus(Long id, CameraStatusUpdateDTO statusUpdateDTO) {
        log.info("Updating camera status for id: {} to: {}", id, statusUpdateDTO.getStatus());

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        Camera.CameraStatus newStatus = Camera.CameraStatus.valueOf(statusUpdateDTO.getStatus());
        camera.setStatus(newStatus);
        if (newStatus == Camera.CameraStatus.ONLINE) {
            camera.setLastActiveTime(LocalDateTime.now());
        }
        camera.setUpdatedAt(LocalDateTime.now());

        cameraRepository.save(camera);
    }

    @Override
    @Transactional
    public void updateCameraResolution(Long id, String resolution) {
        log.info("Updating camera resolution for id: {} to: {}", id, resolution);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        camera.setResolution(resolution);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);
    }

    @Override
    @Transactional
    public void updateCameraCredentials(Long id, String username, String password) {
        log.info("Updating credentials for camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        camera.setUsername(username);
        camera.setPassword(password);
        camera.setStatus(Camera.CameraStatus.CONNECTING);
        camera.setUpdatedAt(LocalDateTime.now());

        cameraRepository.save(camera);
    }

    @Override
    @Transactional
    public void deleteCamera(Long id) {
        log.info("Deleting camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        // Stop streaming if it's active
        try {
            // Note: In a real implementation, we would need to look up active sessions for this camera
            // For now, we'll just log the attempt
            log.info("Stopping any active streams for camera: {}", camera.getId());
            // streamingService.stopStream(sessionId); // Would need actual session ID
        } catch (Exception e) {
            log.error("Error stopping stream for camera: {}", e.getMessage());
        }

        cameraRepository.delete(camera);
    }

    @Override
    public String getCameraStreamUrl(Long cameraId) {
        log.info("Generating stream URL for camera id: {}", cameraId);

        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));

        if (camera.getStatus() != Camera.CameraStatus.ONLINE) {
            throw new ServiceException("Camera is not online");
        }

        return streamingService.getStreamUrl(cameraId);
    }

    @Override
    public Map<String, Object> getCameraStatistics(Long cameraId) {
        log.info("Fetching statistics for camera id: {}", cameraId);

        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("cameraId", camera.getId());
        stats.put("name", camera.getName());
        stats.put("status", camera.getStatus());
        stats.put("resolution", camera.getResolution());
        stats.put("frameRate", camera.getFrameRate());
        stats.put("bitrate", camera.getBitrate());
        stats.put("lastActiveTime", camera.getLastActiveTime());
        stats.put("uptimePercentage", calculateUptimePercentage(camera));

        return stats;
    }

    @Override
    public List<CameraDTO> getOnlineCamerasByEdgeNode(Long edgeNodeId) {
        log.info("Fetching online cameras for edge node id: {}", edgeNodeId);

        // Verify edge node exists
        edgeNodeRepository.findById(edgeNodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + edgeNodeId));

        List<Camera> onlineCameras = cameraRepository.findByEdgeNodeIdAndStatus(edgeNodeId, Camera.CameraStatus.ONLINE);
        return onlineCameras.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public long getCameraCountByStatus(Camera.CameraStatus status) {
        log.info("Counting cameras with status: {}", status);
        return cameraRepository.countByStatus(status);
    }

    @Override
    public boolean testCameraConnection(Long cameraId) {
        log.info("Testing connection for camera id: {}", cameraId);

        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));

        try {
            return streamingService.testCameraConnection(camera);
        } catch (Exception e) {
            log.error("Connection test failed for camera {}: {}", cameraId, e.getMessage());
            return false;
        }
    }

    // Helper methods
    private CameraDTO convertToDto(Camera camera) {
        CameraDTO dto = modelMapper.map(camera, CameraDTO.class);
        // Set edge node name if available
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId())
                    .ifPresent(edgeNode -> dto.setEdgeNodeName(edgeNode.getName()));
        }
        return dto;
    }

    private Camera convertToEntity(CameraDTO dto) {
        return modelMapper.map(dto, Camera.class);
    }

    private double calculateUptimePercentage(Camera camera) {
        // In a real implementation, this would calculate based on historical data
        // For now, return a placeholder value
        return camera.getStatus() == Camera.CameraStatus.ONLINE ? 98.5 : 65.0;
    }
}