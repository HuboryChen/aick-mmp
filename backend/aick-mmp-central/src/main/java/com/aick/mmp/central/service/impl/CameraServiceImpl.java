package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.properties.EdgeNodeProperties;
import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatisticsDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Recording;
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
import java.util.Comparator;
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
    private final RecordingRepository recordingRepository;
    private final com.aick.mmp.central.repository.RegionRepository regionRepository;
    private final StreamingService streamingService;
    private final ModelMapper modelMapper;
    private final com.aick.mmp.central.service.NodeWeightCalculator nodeWeightCalculator;
    private final EdgeNodeProperties edgeNodeProperties;

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
                Integer currentCount = node.getCurrentCameraCount();
                node.setCurrentCameraCount(currentCount == null ? 1 : currentCount + 1);
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
                    Integer currentCount = node.getCurrentCameraCount();
                    node.setCurrentCameraCount(currentCount == null ? 0 : Math.max(0, currentCount - 1));
                    edgeNodeRepository.save(node);
                });
            }

            // 增加新节点的计数
            edgeNodeRepository.findById(cameraDTO.getEdgeNodeId()).ifPresent(node -> {
                Integer currentCount = node.getCurrentCameraCount();
                node.setCurrentCameraCount(currentCount == null ? 1 : currentCount + 1);
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

        // 软删除：设置删除时间（同时设置两个软删除字段确保一致性）
        camera.setDeletedAt(LocalDateTime.now());
        camera.setIsDeleted(true);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        // ========== 级联处理关联录像 ==========
        // 将关联录像标记为孤立状态
        LocalDateTime now = LocalDateTime.now();
        int orphanedCount = recordingRepository.markOrphanedByCameraId(id, now, id);
        log.info("Marked {} recordings as orphaned for camera {}", orphanedCount, id);

        // 减少边缘节点的摄像头计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                Integer currentCount = node.getCurrentCameraCount();
                node.setCurrentCameraCount(currentCount == null ? 0 : Math.max(0, currentCount - 1));
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

        Camera camera = cameraRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));

        if (camera.getDeletedAt() == null) {
            throw new ServiceException("Camera is not deleted");
        }

        // 恢复摄像头（清除逻辑删除标记）
        camera.setIsDeleted(false);
        camera.setUpdatedAt(LocalDateTime.now());
        cameraRepository.save(camera);

        // 如果有边缘节点，恢复节点计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                Integer currentCount = node.getCurrentCameraCount();
                node.setCurrentCameraCount(currentCount == null ? 1 : currentCount + 1);
                edgeNodeRepository.save(node);
            });
        }

        return convertToDto(camera);
    }

    @Override
    @Transactional
    public void forceDeleteCamera(Long id) {
        log.warn("Force deleting camera with id: {}", id);

        Camera camera = cameraRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
        cameraRepository.delete(camera);

        // 减少边缘节点的摄像头计数
        if (camera.getEdgeNodeId() != null) {
            edgeNodeRepository.findById(camera.getEdgeNodeId()).ifPresent(node -> {
                Integer currentCount = node.getCurrentCameraCount();
                node.setCurrentCameraCount(currentCount == null ? 0 : Math.max(0, currentCount - 1));
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
            throw new ServiceException("没有可用的在线边缘节点");
        }

        Long cameraRegionId = cameraDTO.getRegionId();
        double regionBonusRate = edgeNodeProperties.getRegionBonusRate();

        EdgeNode best = onlineNodes.stream()
            .max(Comparator.comparingDouble(n ->
                nodeWeightCalculator.calculateWeightWithRegionBonus(
                    n, n.getCpuUsage(), n.getMemoryUsage(),
                    cameraRegionId, regionBonusRate
                )
            ))
            .orElse(null);

        if (best != null) {
            log.info("Selected optimal edge node {} with region bonus", best.getName());
            return best.getId();
        }

        // Fallback: return the first online node
        return onlineNodes.get(0).getId();
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
                        Integer currentCount = node.getCurrentCameraCount();
                        node.setCurrentCameraCount(currentCount == null ? 0 : Math.max(0, currentCount - 1));
                        edgeNodeRepository.save(node);
                    });
                }
                
                // 更新节点
                camera.setEdgeNodeId(edgeNodeId);
                camera.setUpdatedAt(LocalDateTime.now());
                cameraRepository.save(camera);
                
                // 增加新节点计数
                edgeNodeRepository.findById(edgeNodeId).ifPresent(node -> {
                    Integer currentCount = node.getCurrentCameraCount();
                    node.setCurrentCameraCount(currentCount == null ? 1 : currentCount + 1);
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

    // ========== 孤立录像管理 ==========

    @Override
    @Transactional(readOnly = true)
    public long getOrphanedRecordingsCount() {
        return recordingRepository.countOrphanedRecordings();
    }

    @Override
    @Transactional
    public int cleanupOrphanedRecordings(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        Page<Recording> recordingsToCleanup =
                recordingRepository.findOrphanedRecordingsForCleanup(cutoffDate, Pageable.unpaged());

        int count = 0;
        for (Recording recording : recordingsToCleanup.getContent()) {
            // 实际应该删除文件
            recordingRepository.delete(recording);
            count++;
        }

        log.info("Cleaned up {} orphaned recordings older than {} days", count, daysOld);
        return count;
    }

    // ========== 统计聚合API ==========

    @Override
    @Transactional(readOnly = true)
    public CameraStatisticsDTO getCameraStatisticsSummary(Long regionId, Long edgeNodeId, boolean forceRefresh) {
        log.info("Fetching camera statistics summary: regionId={}, edgeNodeId={}, forceRefresh={}",
                regionId, edgeNodeId, forceRefresh);

        // 按状态统计
        Map<String, Long> byStatus = new HashMap<>();
        for (Camera.CameraStatus status : Camera.CameraStatus.values()) {
            if (regionId != null) {
                byStatus.put(status.name(), cameraRepository.countByRegionIdAndStatusAndIsDeletedFalse(regionId, status));
            } else if (edgeNodeId != null) {
                byStatus.put(status.name(), cameraRepository.countByStatus(status));
            } else {
                byStatus.put(status.name(), cameraRepository.countByStatus(status));
            }
        }

        // 统计总数
        long total;
        if (regionId != null) {
            total = cameraRepository.countByRegionIdAndIsDeletedFalse(regionId);
        } else {
            total = cameraRepository.countActive();
        }

        // 按节点统计
        List<CameraStatisticsDTO.NodeStatistic> byEdgeNode = buildNodeStatistics(edgeNodeId);

        // 录像统计
        CameraStatisticsDTO.RecordingStatistics recordingStats = CameraStatisticsDTO.RecordingStatistics.builder()
                .totalRecordings(recordingRepository.count())
                .orphanedRecordings(recordingRepository.countOrphanedRecordings())
                .deletedRecordings(recordingRepository.countDeletedRecordings())
                .totalStorageSize(recordingRepository.sumTotalStorageSize())
                .build();

        return CameraStatisticsDTO.builder()
                .total(total)
                .byStatus(byStatus)
                .byEdgeNode(byEdgeNode)
                .recordingStatistics(recordingStats)
                .cachedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public void refreshStatisticsCache() {
        log.info("Refreshing camera statistics cache");
        // 在实际实现中，这里会将统计数据刷新到 Redis
        // 目前是占位实现
    }

    /**
     * 构建按节点统计信息
     */
    private List<CameraStatisticsDTO.NodeStatistic> buildNodeStatistics(Long filterEdgeNodeId) {
        List<EdgeNode> nodes;
        if (filterEdgeNodeId != null) {
            nodes = edgeNodeRepository.findAll().stream()
                    .filter(n -> n.getId().equals(filterEdgeNodeId))
                    .collect(Collectors.toList());
        } else {
            nodes = edgeNodeRepository.findAll();
        }

        return nodes.stream()
                .map(node -> {
                    List<Camera> cameras = cameraRepository.findByEdgeNodeId(node.getId());
                    long onlineCount = cameras.stream()
                            .filter(c -> c.getStatus() == Camera.CameraStatus.ONLINE)
                            .count();

                    return CameraStatisticsDTO.NodeStatistic.builder()
                            .edgeNodeId(node.getId())
                            .edgeNodeName(node.getName())
                            .cameraCount(cameras.size())
                            .onlineCount(onlineCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
