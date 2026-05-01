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

import jakarta.persistence.criteria.Predicate;
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
    private final com.aick.mmp.central.repository.RegionRepository regionRepository;
    private final StreamingService streamingService;
    private final ModelMapper modelMapper;
    private final com.aick.mmp.central.service.NodeWeightCalculator nodeWeightCalculator;

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
            
            if (request.getRegionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("regionId"), request.getRegionId()));
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
        existingCamera.setProtocol(cameraDTO.getProtocol());
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
        log.info("Soft deleting camera with id: {}", id);

        Camera camera = cameraRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found or already deleted: " + id));

        // Stop streaming if it's active
        try {
            log.info("Stopping any active streams for camera: {}", camera.getId());
        } catch (Exception e) {
            log.error("Error stopping stream for camera: {}", e.getMessage());
        }

        // 执行软删除：设置 deletedAt 时间戳
        camera.setDeletedAt(LocalDateTime.now());
        cameraRepository.save(camera);
        log.info("Camera {} soft deleted successfully", id);
    }

    @Override
    @Transactional
    public void batchDeleteCameras(List<Long> cameraIds) {
        log.info("Batch soft deleting cameras with ids: {}", cameraIds);

        List<Camera> cameras = cameraRepository.findAllById(cameraIds);
        if (cameras.isEmpty()) {
            throw new ResourceNotFoundException("No cameras found with provided ids");
        }

        LocalDateTime now = LocalDateTime.now();

        // 软删除所有未被删除的摄像头
        int deletedCount = 0;
        for (Camera camera : cameras) {
            if (camera.getDeletedAt() == null) {
                camera.setDeletedAt(now);
                deletedCount++;
            }
        }

        cameraRepository.saveAll(cameras);
        log.info("Successfully soft deleted {} cameras", deletedCount);
    }

    @Override
    @Transactional
    public void batchUpdateEdgeNode(List<Long> cameraIds, Long edgeNodeId) {
        log.info("Batch updating edge node for cameras {} to {}", cameraIds, edgeNodeId);

        List<Camera> cameras = cameraRepository.findAllById(cameraIds);
        if (cameras.isEmpty()) {
            throw new ResourceNotFoundException("No cameras found with provided ids");
        }

        // 验证边缘节点是否存在并检查容量
        if (edgeNodeId != null) {
            EdgeNode edgeNode = edgeNodeRepository.findById(edgeNodeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Edge node not found with id: " + edgeNodeId));
            
            // 检查边缘节点容量
            Long currentCameraCount = cameraRepository.countByEdgeNodeId(edgeNodeId);
            if (edgeNode.getMaxCameraSupport() != null && currentCameraCount + cameraIds.size() > edgeNode.getMaxCameraSupport()) {
                throw new IllegalArgumentException(
                    String.format("边缘节点容量不足: 当前摄像头数 %d, 最大支持 %d, 无法添加 %d 个摄像头", 
                        currentCameraCount, edgeNode.getMaxCameraSupport(), cameraIds.size())
                );
            }
        }

        cameras.forEach(camera -> {
            camera.setEdgeNodeId(edgeNodeId);
            camera.setUpdatedAt(LocalDateTime.now());
        });

        cameraRepository.saveAll(cameras);
        log.info("Successfully updated {} cameras to edge node {}", cameraIds.size(), edgeNodeId);
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
        Camera camera = cameraRepository.findByIdAndDeletedAtIsNull(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found: " + cameraId));
        streamingService.stopStream(camera);
        log.info("Stream stopped for camera {}", cameraId);
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
        log.info("Fetching total active camera count");
        return cameraRepository.countActive();
    }

    /**
     * 恢复已删除的摄像头
     */
    @Override
    @Transactional
    public CameraDTO restoreCamera(Long id) {
        log.info("Restoring camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found: " + id));

        if (camera.getDeletedAt() == null) {
            throw new IllegalStateException("Camera is not deleted");
        }

        camera.setDeletedAt(null);
        Camera saved = cameraRepository.save(camera);
        log.info("Camera {} restored successfully", id);
        return convertToDto(saved);
    }

    /**
     * 强制物理删除摄像头（管理员使用）
     */
    @Override
    @Transactional
    public void forceDeleteCamera(Long id) {
        log.info("Force deleting camera with id: {}", id);

        if (!cameraRepository.existsById(id)) {
            throw new ResourceNotFoundException("Camera not found: " + id);
        }

        cameraRepository.deleteById(id);
        log.info("Camera {} force deleted successfully", id);
    }

    /**
     * 获取所有在线摄像头
     */
    @Override
    public List<CameraDTO> getAllOnlineCameras() {
        log.info("Fetching all online cameras");
        return cameraRepository.findByStatus(Camera.CameraStatus.ONLINE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取已删除的摄像头列表
     */
    @Override
    public List<CameraDTO> getDeletedCameras() {
        log.info("Fetching all deleted cameras");
        return cameraRepository.findAllDeleted().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
        // 设置区域名称
        if (camera.getRegionId() != null) {
            regionRepository.findById(camera.getRegionId())
                .ifPresent(region -> dto.setRegionName(region.getName()));
        }
        return dto;
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

    @Override
    public Long selectOptimalEdgeNode(CameraDTO cameraDTO) {
        log.info("Selecting optimal edge node for camera: {}", cameraDTO.getName());

        List<EdgeNode> onlineNodes = edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE);
        if (onlineNodes.isEmpty()) {
            throw new ResourceNotFoundException("没有在线的边缘节点可用");
        }

        // 使用 NodeWeightCalculator 过滤健康节点并计算权重
        EdgeNode optimalNode = null;
        double maxWeight = -1;

        for (EdgeNode node : onlineNodes) {
            Double cpuUsage = node.getCpuUsage();
            Double memoryUsage = node.getMemoryUsage();

            // 使用共享服务判断健康状态
            if (!nodeWeightCalculator.isNodeHealthy(cpuUsage, memoryUsage)) {
                continue;
            }

            // 检查容量
            if (node.getMaxCameraSupport() != null
                && node.getCurrentCameraCount() >= node.getMaxCameraSupport()) {
                continue;
            }

            // 计算权重
            double weight = nodeWeightCalculator.calculateWeight(node, cpuUsage, memoryUsage);

            if (weight > maxWeight) {
                maxWeight = weight;
                optimalNode = node;
            }

            log.debug("Node {} weight: {}", node.getName(), weight);
        }

        if (optimalNode == null) {
            throw new ResourceNotFoundException("没有可用的健康边缘节点");
        }

        log.info("Selected optimal edge node {} with weight {}", optimalNode.getName(), maxWeight);
        return optimalNode.getId();
    }

    @Override
    @Transactional
    public void autoAssignCamerasToEdgeNodes() {
        log.info("Starting automatic camera assignment to edge nodes");
        
        List<Camera> unassignedCameras = cameraRepository.findByEdgeNodeIdIsNull();
        if (unassignedCameras.isEmpty()) {
            log.info("No unassigned cameras found");
            return;
        }
        
        log.info("Found {} unassigned cameras", unassignedCameras.size());
        
        for (Camera camera : unassignedCameras) {
            try {
                CameraDTO cameraDTO = convertToDto(camera);
                Long optimalNodeId = selectOptimalEdgeNode(cameraDTO);
                
                camera.setEdgeNodeId(optimalNodeId);
                camera.setUpdatedAt(LocalDateTime.now());
                cameraRepository.save(camera);
                
                log.info("Assigned camera {} to edge node {}", camera.getName(), optimalNodeId);
            } catch (Exception e) {
                log.error("Failed to assign camera {} to edge node: {}", camera.getName(), e.getMessage());
            }
        }
        
        log.info("Automatic camera assignment completed");
    }
}