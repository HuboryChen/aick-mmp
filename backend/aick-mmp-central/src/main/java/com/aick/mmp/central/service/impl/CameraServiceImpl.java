package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public Page<CameraDTO> getCameras(GetCamerasRequestDTO request) {
        log.info("Fetching cameras with request: {}", request);
        
        // 构建动态查询条件
        Specification<Camera> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (request.getLocation() != null && !request.getLocation().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("location"), request.getLocation()));
            }
            
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }
            
            if (request.getEdgeNodeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("edgeNodeId"), request.getEdgeNodeId()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return cameraRepository.findAll(spec, request.getPageable())
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
        log.info("Creating camera with data: {}", cameraDTO);
        
        Camera camera = modelMapper.map(cameraDTO, Camera.class);
        camera.setStatus(Camera.CameraStatus.OFFLINE);
        camera.setCreatedAt(LocalDateTime.now());
        camera.setUpdatedAt(LocalDateTime.now());
        
        // 关联边缘节点
        if (cameraDTO.getEdgeNodeId() != null) {
            EdgeNode edgeNode = edgeNodeRepository.findById(cameraDTO.getEdgeNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + cameraDTO.getEdgeNodeId()));
            camera.setEdgeNodeId(edgeNode.getId());
        }
        
        Camera savedCamera = cameraRepository.save(camera);
        return convertToDto(savedCamera);
    }

    @Override
    @Transactional
    public CameraDTO updateCamera(Long id, CameraDTO cameraDTO) {
        log.info("Updating camera with id: {} and data: {}", id, cameraDTO);
        
        Camera existingCamera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        
        // 更新基本信息
        existingCamera.setName(cameraDTO.getName());
        existingCamera.setLocation(cameraDTO.getLocation());
        existingCamera.setConnectionUrl(cameraDTO.getConnectionUrl());
        existingCamera.setResolution(cameraDTO.getResolution());
        existingCamera.setFrameRate(cameraDTO.getFrameRate());
        existingCamera.setBitrate(cameraDTO.getBitrate());
        existingCamera.setUpdatedAt(LocalDateTime.now());
        
        // 更新边缘节点
        if (cameraDTO.getEdgeNodeId() != null) {
            EdgeNode edgeNode = edgeNodeRepository.findById(cameraDTO.getEdgeNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + cameraDTO.getEdgeNodeId()));
            existingCamera.setEdgeNodeId(edgeNode.getId());
        } else {
            existingCamera.setEdgeNodeId(null);
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
    public String startCameraStream(Long cameraId) {
        log.info("Starting stream for camera id: {}", cameraId);
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));
        return streamingService.startStream(camera);
    }

    @Override
    public void stopCameraStream(Long cameraId) {
        log.info("Stopping stream for camera id: {}", cameraId);
        // 这里需要实现停止流的具体逻辑
        // 由于缺少会话ID信息，暂时只是记录日志
        throw new UnsupportedOperationException("Stop stream by camera ID not implemented yet");
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
        log.info("Fetching online cameras by edge node id: {}", edgeNodeId);
        
        List<Camera> onlineCameras = cameraRepository.findByEdgeNodeIdAndStatus(edgeNodeId, Camera.CameraStatus.ONLINE);
        return onlineCameras.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public long getCameraCountByStatus(Camera.CameraStatus status) {
        log.info("Fetching camera count by status: {}", status);
        return cameraRepository.countByStatus(status);
    }
    
    @Override
    public long getCameraCount() {
        log.info("Fetching total camera count");
        return cameraRepository.count();
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
        if (camera.getEdgeNodeId() != null) {
            EdgeNode edgeNode = edgeNodeRepository.findById(camera.getEdgeNodeId()).orElse(null);
            if (edgeNode != null) {
                dto.setEdgeNodeId(edgeNode.getId());
                dto.setEdgeNodeName(edgeNode.getName());
            }
        }
        return dto;
    }

    private Camera convertToEntity(CameraDTO dto) {
        return modelMapper.map(dto, Camera.class);
    }

    private double calculateUptimePercentage(Camera camera) {
        // 简化的在线时间计算逻辑
        if (camera.getCreatedAt() == null || camera.getLastActiveTime() == null) {
            return 0.0;
        }
        
        long totalDuration = java.time.Duration.between(camera.getCreatedAt(), LocalDateTime.now()).getSeconds();
        long activeDuration = java.time.Duration.between(camera.getLastActiveTime(), LocalDateTime.now()).getSeconds();
        
        if (totalDuration <= 0) {
            return 0.0;
        }
        
        return Math.min(100.0, (activeDuration * 100.0) / totalDuration);
    }
}