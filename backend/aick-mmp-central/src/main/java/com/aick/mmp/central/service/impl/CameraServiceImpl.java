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

    /**
     * 批量分配的批次大小
     */
    private static final int BATCH_SIZE = 50;

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

            // 只查询未删除的记录
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return cameraRepository.findAll(spec, request.getPageable())
                .map(this::convertToDto);
    }

    @Override
    public CameraDTO getCameraById(Long id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        return convertToDto(camera);
    }

    @Override
    @Transactional
    public CameraDTO createCamera(CameraDTO cameraDTO) {
        log.info("Creating new camera: {}", cameraDTO.getName());

        Camera camera = convertToEntity(cameraDTO);
        camera.setCreatedAt(LocalDateTime.now());
        camera.setUpdatedAt(LocalDateTime.now());
        camera.setLastActiveTime(LocalDateTime.now());

        // 如果没有指定状态，默认设置为离线
        if (camera.getStatus() == null) {
            camera.setStatus(Camera.CameraStatus.OFFLINE);
        }

        // 如果指定了边缘节点，增加该节点的摄像头计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                node.setCurrentCameraCount(node.getCurrentCameraCount() + 1);
                edgeNodeRepository.save(node);
            });
        }

        Camera savedCamera = cameraRepository.save(camera);
        return convertToDto(savedCamera);
    }

    @Override
    @Transactional
    public CameraDTO updateCamera(Long id, CameraDTO cameraDTO) {
        log.info("Updating camera with id: {}", id);

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
        existingCamera.setCompression(cameraDTO.getCompression());
        existingCamera.setAudioEnabled(cameraDTO.getAudioEnabled());
        if (cameraDTO.getStatus() != null) {
            existingCamera.setStatus(cameraDTO.getStatus());
        }
        existingCamera.setRegionId(cameraDTO.getRegionId());
        existingCamera.setUpdatedAt(LocalDateTime.now());

        // 如果更换了边缘节点
        if (cameraDTO.getEdgeNodeId() != null && !cameraDTO.getEdgeNodeId().equals(existingCamera.getEdgeNodeId())) {
            // 减少原节点的计数
            if (existingCamera.getEdgeNodeId() != null) {
                edgeNodeRepository.findById(existingCamera.getEdgeNodeId()).ifPresent(node -> {
                    node.setCurrentCameraCount(Math.max(0, node.getCurrentCameraCount() - 1));
                    edgeNodeRepository.save(node);
                });
            }

            // 增加新节点的计数
            edgeNodeRepository.findById(cameraDTO.getEdgeNodeId()).ifPresent(node -> {
                node.setCurrentCameraCount(node.getCurrentCameraCount() + 1);
                edgeNodeRepository.save(node);
            });
        }

        Camera savedCamera = cameraRepository.save(existingCamera);
        return convertToDto(savedCamera);
    }

    @Override
    @Transactional
    public void deleteCamera(Long id) {
        log.info("Deleting camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        // 软删除：设置删除时间
        camera.setDeletedAt(LocalDateTime.now());
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        // 减少边缘节点的摄像头计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                node.setCurrentCameraCount(Math.max(0, node.getCurrentCameraCount() - 1));
                edgeNodeRepository.save(node);
            });
        }
    }

    @Override
    @Transactional
    public void updateCameraStatus(Long id, CameraStatusUpdateDTO statusUpdate) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        Camera.CameraStatus newStatus = Camera.CameraStatus.valueOf(statusUpdate.getStatus());
        camera.setStatus(newStatus);
        camera.setUpdatedAt(LocalDateTime.now());

        if (newStatus == Camera.CameraStatus.ONLINE) {
            camera.setLastActiveTime(LocalDateTime.now());
        }

        cameraRepository.save(camera);
    }

    @Override
    @Transactional
    public String startCameraStream(Long id) {
        log.info("Starting stream for camera with id: {}", id);
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        // 检查摄像头是否支持流媒体
        if (camera.getConnectionUrl() == null || camera.getConnectionUrl().isEmpty()) {
            throw new ServiceException("Camera does not have a valid connection URL");
        }

        // 如果当前正在连接中，直接返回
        if (camera.getStatus() == Camera.CameraStatus.CONNECTING) {
            throw new ServiceException("Camera is already connecting");
        }

        // 设置状态为连接中
        camera.setStatus(Camera.CameraStatus.CONNECTING);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        try {
            // 调用流媒体服务获取流地址
            String streamUrl = streamingService.getStreamUrl(id);

            // 连接成功后更新为在线状态
            camera.setStatus(Camera.CameraStatus.ONLINE);
            camera.setLastActiveTime(LocalDateTime.now());
            camera.setUpdatedAt(LocalDateTime.now());
            cameraRepository.save(camera);

            log.info("Camera {} is now online", id);
            return streamUrl;
        } catch (Exception e) {
            // 连接失败，更新状态为错误
            camera.setStatus(Camera.CameraStatus.ERROR);
            camera.setUpdatedAt(LocalDateTime.now());
            cameraRepository.save(camera);
            throw new ServiceException("Failed to start stream: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void stopCameraStream(Long id) {
        log.info("Stopping stream for camera with id: {}", id);
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        streamingService.stopStream(id);

        // 停止成功后更新状态为离线
        camera.setStatus(Camera.CameraStatus.OFFLINE);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        log.info("Camera {} is now offline", id);
    }

    @Override
    public Page<CameraDTO> getCamerasByStatus(Camera.CameraStatus status, Pageable pageable) {
        log.info("Fetching cameras with status: {}", status);
        return cameraRepository.findByStatus(status, pageable)
                .map(this::convertToDto);
    }

    @Override
    public List<CameraDTO> getCamerasByEdgeNode(Long edgeNodeId) {
        log.info("Fetching cameras for edge node: {}", edgeNodeId);
        return cameraRepository.findByEdgeNodeId(edgeNodeId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CameraDTO restoreCamera(Long id) {
        log.info("Restoring camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        if (camera.getDeletedAt() == null) {
            throw new ServiceException("Camera is not deleted");
        }

        // 恢复摄像头
        camera.setDeletedAt(null);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        // 如果有边缘节点，恢复节点计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                node.setCurrentCameraCount(node.getCurrentCameraCount() + 1);
                edgeNodeRepository.save(node);
            });
        }

        return convertToDto(camera);
    }

    @Override
    @Transactional
    public void forceDeleteCamera(Long id) {
        log.warn("Force deleting camera with id: {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        // 物理删除
        cameraRepository.delete(camera);

        // 减少边缘节点的摄像头计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                node.setCurrentCameraCount(Math.max(0, node.getCurrentCameraCount() - 1));
                edgeNodeRepository.save(node);
            });
        }
    }

    @Override
    public List<CameraDTO> getDeletedCameras() {
        log.info("Fetching all deleted cameras");
        return cameraRepository.findAllDeleted().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CameraDTO> getAllOnlineCameras() {
        log.info("Fetching all online cameras");
        return cameraRepository.findByStatus(Camera.CameraStatus.ONLINE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> batchUpdateStatus(List<Long> ids, Camera.CameraStatus newStatus) {
        log.info("Batch updating status for {} cameras to {}", ids.size(), newStatus);

        List<Camera> cameras = cameraRepository.findAllById(ids);
        int successCount = 0;
        int failCount = 0;
        List<Long> failedIds = new ArrayList<>();

        for (Camera camera : cameras) {
            try {
                camera.setStatus(newStatus);
                camera.setUpdatedAt(LocalDateTime.now());
                if (newStatus == Camera.CameraStatus.ONLINE) {
                    camera.setLastActiveTime(LocalDateTime.now());
                }
                cameraRepository.save(camera);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedIds.add(camera.getId());
                log.error("Failed to update camera {}: {}", camera.getId(), e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedIds", failedIds);
        return result;
    }

    @Override
    public CameraDTO convertToDto(Camera camera) {
        CameraDTO dto = modelMapper.map(camera, CameraDTO.class);

        // 计算在线时长百分比
        if (camera.getCreatedAt() != null && camera.getLastActiveTime() != null) {
            double uptimePercentage = calculateUptimePercentage(camera);
            dto.setUptimePercentage(uptimePercentage);
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
    public Camera convertToEntity(CameraDTO dto) {
        return modelMapper.map(dto, Camera.class);
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

        List<Camera> toSave = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Camera camera : unassignedCameras) {
            try {
                CameraDTO cameraDTO = convertToDto(camera);
                Long optimalNodeId = selectOptimalEdgeNode(cameraDTO);

                camera.setEdgeNodeId(optimalNodeId);
                camera.setUpdatedAt(LocalDateTime.now());
                toSave.add(camera);

                // 达到批次大小时批量保存
                if (toSave.size() >= BATCH_SIZE) {
                    cameraRepository.saveAll(toSave);
                    successCount += toSave.size();
                    toSave.clear();
                    log.info("Batch saved {} cameras", successCount);
                }

                log.debug("Prepared to assign camera {} to edge node {}", camera.getName(), optimalNodeId);
            } catch (Exception e) {
                failCount++;
                log.error("Failed to assign camera {} to edge node: {}", camera.getName(), e.getMessage());
            }
        }

        // 保存剩余的摄像头
        if (!toSave.isEmpty()) {
            cameraRepository.saveAll(toSave);
            successCount += toSave.size();
        }

        log.info("Automatic camera assignment completed: {} succeeded, {} failed", successCount, failCount);
    }

    @Override
    public void updateCameraResolution(Long id, String resolution) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        camera.setResolution(resolution);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);
    }

    @Override
    public void updateCameraCredentials(Long id, String username, String password) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        camera.setUsername(username);
        camera.setPassword(password);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);
    }

    @Override
    public void batchDeleteCameras(List<Long> cameraIds) {
        log.info("Batch deleting {} cameras", cameraIds.size());
        for (Long id : cameraIds) {
            try {
                deleteCamera(id);
            } catch (Exception e) {
                log.error("Failed to delete camera {}: {}", id, e.getMessage());
            }
        }
    }

    @Override
    public void batchUpdateEdgeNode(List<Long> cameraIds, Long edgeNodeId) {
        log.info("Batch updating edge node for {} cameras to {}", cameraIds.size(), edgeNodeId);
        for (Long id : cameraIds) {
            try {
                Camera camera = cameraRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
                
                // 减少原节点计数
                if (camera.getEdgeNodeId() != null) {
                    edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                        node.setCurrentCameraCount(Math.max(0, node.getCurrentCameraCount() - 1));
                        edgeNodeRepository.save(node);
                    });
                }
                
                // 更新节点
                camera.setEdgeNodeId(edgeNodeId);
                camera.setUpdatedAt(LocalDateTime.now());
                cameraRepository.save(camera);
                
                // 增加新节点计数
                edgeNodeRepository.findById(edgeNodeId).ifPresent(node -> {
                    node.setCurrentCameraCount(node.getCurrentCameraCount() + 1);
                    edgeNodeRepository.save(node);
                });
            } catch (Exception e) {
                log.error("Failed to update edge node for camera {}: {}", id, e.getMessage());
            }
        }
    }

    @Override
    public List<CameraDTO> getOnlineCamerasByEdgeNode(Long edgeNodeId) {
        log.info("Fetching online cameras for edge node: {}", edgeNodeId);
        return cameraRepository.findByEdgeNodeIdAndStatus(edgeNodeId, Camera.CameraStatus.ONLINE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCameraStatistics(Long cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("id", camera.getId());
        stats.put("name", camera.getName());
        stats.put("status", camera.getStatus());
        stats.put("lastActiveTime", camera.getLastActiveTime());
        stats.put("createdAt", camera.getCreatedAt());
        
        // 计算在线时长百分比
        if (camera.getCreatedAt() != null && camera.getLastActiveTime() != null) {
            stats.put("uptimePercentage", calculateUptimePercentage(camera));
        }
        
        return stats;
    }

    @Override
    public boolean testCameraConnection(Long cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + cameraId));
        // 简单测试：检查连接URL是否有效
        return camera.getConnectionUrl() != null && !camera.getConnectionUrl().isEmpty();
    }

    @Override
    public long getCameraCountByStatus(Camera.CameraStatus status) {
        return cameraRepository.countByStatus(status);
    }
}
