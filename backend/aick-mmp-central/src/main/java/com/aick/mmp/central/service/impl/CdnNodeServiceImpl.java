package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.repository.CdnNodeLoadRepository;
import com.aick.mmp.central.repository.CdnNodeRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.CdnNodeService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.shared.model.CdnNodeLoad;
import com.aick.mmp.shared.model.Region;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CDN节点服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CdnNodeServiceImpl implements CdnNodeService {

    private final CdnNodeRepository cdnNodeRepository;
    private final CdnNodeLoadRepository cdnNodeLoadRepository;
    private final RegionRepository regionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 心跳超时阈值（秒）
     */
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    /**
     * 最大负载阈值（百分比）
     */
    private static final double MAX_LOAD_THRESHOLD = 90.0;

    @Override
    public Page<CdnNodeDTO> getAllCdnNodes(Pageable pageable) {
        return cdnNodeRepository.findByIsDeletedFalse(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public List<CdnNodeDTO> getAllActiveCdnNodes() {
        return cdnNodeRepository.findByIsDeletedFalseAndIsEnabledTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CdnNodeDTO> getCdnNodesByRegion(String region, Pageable pageable) {
        // For backward compatibility, region is now treated as regionId
        // If region is a numeric string, parse it as regionId
        try {
            Long regionId = Long.parseLong(region);
            return cdnNodeRepository.findByRegionIdAndIsDeletedFalse(regionId, pageable)
                    .map(this::convertToDTO);
        } catch (NumberFormatException e) {
            // If not a valid number, return empty page
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<CdnNodeDTO> getCdnNodesByRegionId(Long regionId, boolean recursive, Pageable pageable) {
        if (recursive) {
            // Get all descendant region IDs
            List<Long> regionIds = getAllDescendantRegionIds(regionId);
            return cdnNodeRepository.findAll((root, query, cb) -> 
                root.get("regionId").in(regionIds), pageable)
                .map(this::convertToDTO);
        } else {
            return cdnNodeRepository.findByRegionIdAndIsDeletedFalse(regionId, pageable)
                .map(this::convertToDTO);
        }
    }

    @Override
    public Page<CdnNodeDTO> getCdnNodesByStatus(CdnNode.NodeStatus status, Pageable pageable) {
        return cdnNodeRepository.findByStatusAndIsDeletedFalse(status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public CdnNodeDTO getCdnNodeById(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));
        return convertToDTO(cdnNode);
    }

    @Override
    public CdnNodeDTO getCdnNodeByNodeId(String nodeId) {
        CdnNode cdnNode = cdnNodeRepository.findByNodeId(nodeId)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found: " + nodeId));
        return convertToDTO(cdnNode);
    }

    @Override
    @Transactional
    public CdnNodeDTO createCdnNode(CdnNodeDTO cdnNodeDTO) {
        // 检查节点是否已存在
        if (cdnNodeRepository.existsByIpAddress(cdnNodeDTO.getIpAddress())) {
            throw new ServiceException("CDN node with this IP address already exists: " + cdnNodeDTO.getIpAddress());
        }

        CdnNode cdnNode = CdnNode.builder()
                .nodeId(generateNodeId())
                .name(cdnNodeDTO.getName())
                .ipAddress(cdnNodeDTO.getIpAddress())
                .port(cdnNodeDTO.getPort())
                .location(cdnNodeDTO.getLocation())
                .regionId(cdnNodeDTO.getRegionId())
                .status(CdnNode.NodeStatus.ONLINE)
                .capacity(cdnNodeDTO.getCapacity())
                .currentLoad(0)
                .weight(cdnNodeDTO.getWeight() != null ? cdnNodeDTO.getWeight() : 100)
                .priority(cdnNodeDTO.getPriority() != null ? cdnNodeDTO.getPriority() : 100)
                .healthCheckUrl(cdnNodeDTO.getHealthCheckUrl())
                .connectTimeout(5000)
                .readTimeout(10000)
                .isEnabled(true)
                .isDeleted(false)
                .lastHeartbeat(LocalDateTime.now())
                .build();

        CdnNode savedNode = cdnNodeRepository.save(cdnNode);
        log.info("Created new CDN node: {} ({}) in region {}",
                savedNode.getName(), savedNode.getIpAddress(), savedNode.getRegionId());
        return convertToDTO(savedNode);
    }

    @Override
    @Transactional
    public CdnNodeDTO updateCdnNode(Long id, CdnNodeDTO cdnNodeDTO) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        // 检查IP地址是否已被其他节点使用
        if (!cdnNode.getIpAddress().equals(cdnNodeDTO.getIpAddress()) &&
                cdnNodeRepository.existsByIpAddress(cdnNodeDTO.getIpAddress())) {
            throw new ServiceException("CDN node with this IP address already exists: " + cdnNodeDTO.getIpAddress());
        }

        cdnNode.setName(cdnNodeDTO.getName());
        cdnNode.setIpAddress(cdnNodeDTO.getIpAddress());
        cdnNode.setPort(cdnNodeDTO.getPort());
        cdnNode.setLocation(cdnNodeDTO.getLocation());
        cdnNode.setRegionId(cdnNodeDTO.getRegionId());
        cdnNode.setCapacity(cdnNodeDTO.getCapacity());
        cdnNode.setWeight(cdnNodeDTO.getWeight());
        cdnNode.setPriority(cdnNodeDTO.getPriority());
        cdnNode.setHealthCheckUrl(cdnNodeDTO.getHealthCheckUrl());
        cdnNode.setIsEnabled(cdnNodeDTO.getIsEnabled());

        CdnNode updatedNode = cdnNodeRepository.save(cdnNode);
        return convertToDTO(updatedNode);
    }

    @Override
    @Transactional
    public void updateCdnNodeStatus(Long id, String status, String message) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        CdnNode.NodeStatus newStatus = CdnNode.NodeStatus.valueOf(status.toUpperCase());
        cdnNode.setStatus(newStatus);
        cdnNode.setLastHeartbeat(LocalDateTime.now());

        cdnNodeRepository.save(cdnNode);
        log.info("Updated CDN node status: {} (ID: {}) - {}", cdnNode.getName(), id, newStatus);
    }

    @Override
    @Transactional
    public void deleteCdnNode(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        cdnNode.setIsDeleted(true);
        cdnNode.setDeletedAt(LocalDateTime.now());
        cdnNode.setStatus(CdnNode.NodeStatus.OFFLINE);
        cdnNodeRepository.save(cdnNode);

        log.info("Soft deleted CDN node: {} (ID: {})", cdnNode.getName(), id);
    }

    @Override
    @Transactional
    public CdnNodeDTO restoreCdnNode(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        cdnNode.setIsDeleted(false);
        cdnNode.setDeletedAt(null);
        cdnNode.setStatus(CdnNode.NodeStatus.ONLINE);

        CdnNode restoredNode = cdnNodeRepository.save(cdnNode);
        log.info("Restored CDN node: {} (ID: {})", restoredNode.getName(), id);
        return convertToDTO(restoredNode);
    }

    @Override
    @Transactional
    public void enableCdnNode(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        cdnNode.setIsEnabled(true);
        cdnNode.setStatus(CdnNode.NodeStatus.ONLINE);
        cdnNodeRepository.save(cdnNode);

        log.info("Enabled CDN node: {} (ID: {})", cdnNode.getName(), id);
    }

    @Override
    @Transactional
    public void disableCdnNode(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        cdnNode.setIsEnabled(false);
        cdnNode.setStatus(CdnNode.NodeStatus.OFFLINE);
        cdnNodeRepository.save(cdnNode);

        log.info("Disabled CDN node: {} (ID: {})", cdnNode.getName(), id);
    }

    @Override
    @Transactional
    public void registerHeartbeat(String nodeId, Map<String, Object> metrics) {
        CdnNode cdnNode = cdnNodeRepository.findByNodeId(nodeId)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found: " + nodeId));

        // 更新心跳时间
        cdnNode.setLastHeartbeat(LocalDateTime.now());
        cdnNode.setStatus(CdnNode.NodeStatus.ONLINE);

        // 更新负载
        if (metrics.containsKey("currentLoad")) {
            cdnNode.setCurrentLoad(((Number) metrics.get("currentLoad")).intValue());
        }

        // 更新CPU
        if (metrics.containsKey("cpuUsage")) {
            cdnNode.setCpuUsage(((Number) metrics.get("cpuUsage")).doubleValue());
        }

        // 更新内存
        if (metrics.containsKey("memoryUsage")) {
            cdnNode.setMemoryUsage(((Number) metrics.get("memoryUsage")).doubleValue());
        }

        // 更新带宽
        if (metrics.containsKey("bandwidthUsage")) {
            cdnNode.setBandwidthUsage(((Number) metrics.get("bandwidthUsage")).doubleValue());
        }

        cdnNodeRepository.save(cdnNode);
    }

    @Override
    @Transactional
    public void reportLoad(CdnNodeReportDTO report) {
        CdnNode cdnNode = cdnNodeRepository.findByNodeId(report.getNodeId())
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found: " + report.getNodeId()));

        // 更新节点负载
        cdnNode.setCurrentLoad(report.getCurrentLoad());
        cdnNode.setCpuUsage(report.getCpuUsage());
        cdnNode.setMemoryUsage(report.getMemoryUsage());
        cdnNode.setBandwidthUsage(report.getBandwidthUsage());
        cdnNode.setStorageUsage(report.getStorageUsage());
        cdnNode.setUpBandwidth(report.getUpBandwidth());
        cdnNode.setDownBandwidth(report.getDownBandwidth());
        cdnNode.setLastHeartbeat(LocalDateTime.now());
        cdnNode.setStatus(CdnNode.NodeStatus.ONLINE);

        cdnNodeRepository.save(cdnNode);

        // 保存负载历史记录
        CdnNodeLoad loadRecord = CdnNodeLoad.builder()
                .cdnNodeId(cdnNode.getId())
                .recordedAt(LocalDateTime.now())
                .currentLoad(report.getCurrentLoad())
                .cpuUsage(report.getCpuUsage())
                .memoryUsage(report.getMemoryUsage())
                .bandwidthUsage(report.getBandwidthUsage())
                .storageUsage(report.getStorageUsage())
                .upBandwidth(report.getUpBandwidth())
                .downBandwidth(report.getDownBandwidth())
                .activeConnections(report.getActiveConnections())
                .requestRate(report.getRequestRate())
                .bandwidthThroughput(report.getBandwidthThroughput())
                .cacheHitRate(report.getCacheHitRate())
                .avgResponseTime(report.getAvgResponseTime())
                .errorRate(report.getErrorRate())
                .status(CdnNode.NodeStatus.ONLINE)
                .loadPercentage(calculateLoadPercentage(cdnNode))
                .extraData(serializeExtraData(report.getExtraData()))
                .build();

        cdnNodeLoadRepository.save(loadRecord);
    }

    @Override
    public List<CdnNodeLoadDTO> getLoadHistory(Long nodeId, LocalDateTime startTime, LocalDateTime endTime) {
        List<CdnNodeLoad> loads = cdnNodeLoadRepository.findByCdnNodeIdAndTimeRange(nodeId, startTime, endTime);
        return loads.stream()
                .map(this::convertLoadToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CdnNodeLoadDTO getLatestLoad(Long nodeId) {
        CdnNodeLoad load = cdnNodeLoadRepository.findLatestByCdnNodeId(nodeId);
        return load != null ? convertLoadToDTO(load) : null;
    }

    @Override
    public CdnNodeStatsDTO getCdnNodeStatistics(Long nodeId) {
        CdnNode cdnNode = cdnNodeRepository.findById(nodeId)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + nodeId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(24);

        // 获取最近24小时的历史记录
        List<CdnNodeLoad> recentLoads = cdnNodeLoadRepository.findByCdnNodeIdAndTimeRange(
                nodeId, startTime, now);

        // 计算统计数据
        Double avgLoad = recentLoads.stream()
                .filter(l -> l.getCurrentLoad() != null)
                .mapToInt(CdnNodeLoad::getCurrentLoad)
                .average()
                .orElse(0.0);

        Integer maxLoad = recentLoads.stream()
                .filter(l -> l.getCurrentLoad() != null)
                .mapToInt(CdnNodeLoad::getCurrentLoad)
                .max()
                .orElse(0);

        // 计算在线时长
        Long uptimeSeconds = cdnNode.getLastHeartbeat() != null ?
                Duration.between(cdnNode.getLastHeartbeat(), now).getSeconds() : 0L;

        // 判断健康状态
        String healthStatus = determineHealthStatus(cdnNode);

        return CdnNodeStatsDTO.builder()
                .node(convertToDTO(cdnNode))
                .loadPercentage(calculateLoadPercentage(cdnNode))
                .cpuUsage(cdnNode.getCpuUsage())
                .memoryUsage(cdnNode.getMemoryUsage())
                .bandwidthUsage(cdnNode.getBandwidthUsage())
                .storageUsage(cdnNode.getStorageUsage())
                .uptimeSeconds(uptimeSeconds)
                .lastHeartbeat(cdnNode.getLastHeartbeat())
                .healthStatus(healthStatus)
                .suggestion(generateSuggestion(cdnNode, healthStatus))
                .recentLoads(recentLoads.stream().limit(100).map(this::convertLoadToDTO).collect(Collectors.toList()))
                .avgLoad(avgLoad)
                .maxLoad(maxLoad)
                .statsStartTime(startTime)
                .statsEndTime(now)
                .build();
    }

    @Override
    public List<CdnNodeDTO> getBestCdnNodesForRegion(String region, int count) {
        // region参数现在作为regionId使用
        try {
            Long regionId = Long.parseLong(region);
            // 获取指定区域内健康的CDN节点
            List<CdnNode> candidates = cdnNodeRepository.findByRegionIdAndStatusAndIsDeletedFalseOrderByCurrentLoadAsc(
                    regionId, CdnNode.NodeStatus.ONLINE);

            if (candidates.isEmpty()) {
                log.warn("No healthy CDN nodes in region {}. Using global nodes.", region);
                candidates = cdnNodeRepository.findByStatusAndIsDeletedFalseOrderByCurrentLoadAsc(CdnNode.NodeStatus.ONLINE);
            }

            return candidates.stream()
                    .limit(count)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("Invalid region ID: {}. Using global nodes.", region);
            List<CdnNode> candidates = cdnNodeRepository.findByStatusAndIsDeletedFalseOrderByCurrentLoadAsc(CdnNode.NodeStatus.ONLINE);
            return candidates.stream()
                    .limit(count)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<CdnNodeDTO> getBestCdnNodesByWlc(int count) {
        // 使用WLC算法选择最佳节点
        List<CdnNode> candidates = cdnNodeRepository.findAllAvailableNodesForWlc();

        if (candidates.isEmpty()) {
            log.warn("No available CDN nodes for WLC selection");
            return Collections.emptyList();
        }

        // 按WLC分数排序
        return candidates.stream()
                .sorted((n1, n2) -> Double.compare(n2.calculateWlcScore(), n1.calculateWlcScore()))
                .limit(count)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CdnNodeDTO> getBestCdnNodesByGeoAndWlc(String regionCode, int count) {
        // 地理邻近性 + WLC混合算法
        // regionCode参数现在作为regionId使用
        try {
            Long regionId = Long.parseLong(regionCode);
            // 1. 首先尝试获取同区域节点
            List<CdnNode> regionCandidates = cdnNodeRepository.findAvailableNodesByRegionIdForWlc(regionId);

            if (regionCandidates.isEmpty()) {
                // 如果没有同区域节点，扩大到相邻区域或全局
                log.info("No available nodes in region {}, falling back to global WLC", regionCode);
                return getBestCdnNodesByWlc(count);
            }

            // 2. 按WLC分数排序同区域节点
            return regionCandidates.stream()
                    .sorted((n1, n2) -> Double.compare(n2.calculateWlcScore(), n1.calculateWlcScore()))
                    .limit(count)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("Invalid region ID: {}. Using global WLC.", regionCode);
            return getBestCdnNodesByWlc(count);
        }
    }

    @Override
    public CdnNodeDTO selectOptimalNode() {
        List<CdnNodeDTO> candidates = getBestCdnNodesByWlc(1);
        if (candidates.isEmpty()) {
            throw new ServiceException("No available CDN nodes");
        }
        return candidates.get(0);
    }

    @Override
    public CdnNodeConnectivityTestDTO testConnectivity(Long nodeId) {
        CdnNode cdnNode = cdnNodeRepository.findById(nodeId)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + nodeId));

        return performConnectivityTest(cdnNode);
    }

    @Override
    public Map<Long, CdnNodeConnectivityTestDTO> batchHealthCheck() {
        List<CdnNode> nodes = cdnNodeRepository.findNodesNeedingHealthCheck();
        Map<Long, CdnNodeConnectivityTestDTO> results = new HashMap<>();

        for (CdnNode node : nodes) {
            try {
                CdnNodeConnectivityTestDTO result = performConnectivityTest(node);
                results.put(node.getId(), result);

                // 根据测试结果更新节点状态
                if (!result.isSuccess()) {
                    node.setStatus(CdnNode.NodeStatus.DEGRADED);
                    cdnNodeRepository.save(node);
                }
            } catch (Exception e) {
                log.error("Health check failed for node {}: {}", node.getId(), e.getMessage());
                results.put(node.getId(), CdnNodeConnectivityTestDTO.builder()
                        .nodeId(node.getId())
                        .nodeName(node.getName())
                        .ipAddress(node.getIpAddress())
                        .success(false)
                        .status("ERROR")
                        .errorMessage(e.getMessage())
                        .testTimestamp(System.currentTimeMillis())
                        .suggestion("Manual inspection required")
                        .build());
            }
        }

        return results;
    }

    @Override
    @Transactional
    public void checkHeartbeatTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        List<CdnNode> timeoutNodes = cdnNodeRepository.findNodesWithHeartbeatTimeout(threshold);

        for (CdnNode node : timeoutNodes) {
            node.setStatus(CdnNode.NodeStatus.OFFLINE);
            cdnNodeRepository.save(node);
            log.warn("CDN node {} marked as OFFLINE due to heartbeat timeout", node.getName());
        }
    }

    @Override
    public Map<String, Object> getGlobalCdnStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalNodes = cdnNodeRepository.findByIsDeletedFalse().size();
        long onlineNodes = cdnNodeRepository.countOnlineNodes();
        long offlineNodes = cdnNodeRepository.countOfflineNodes();
        long healthyNodes = cdnNodeRepository.countByStatusAndIsDeletedFalse(CdnNode.NodeStatus.ONLINE);

        stats.put("totalNodes", totalNodes);
        stats.put("onlineNodes", onlineNodes);
        stats.put("offlineNodes", offlineNodes);
        stats.put("healthyNodes", healthyNodes);
        stats.put("healthRate", totalNodes > 0 ? (healthyNodes * 100.0 / totalNodes) : 0);

        // 计算总容量和负载
        List<CdnNode> allNodes = cdnNodeRepository.findByIsDeletedFalse();
        int totalCapacity = allNodes.stream()
                .filter(n -> n.getCapacity() != null)
                .mapToInt(CdnNode::getCapacity)
                .sum();
        int totalLoad = allNodes.stream()
                .filter(n -> n.getCurrentLoad() != null)
                .mapToInt(CdnNode::getCurrentLoad)
                .sum();

        stats.put("totalCapacity", totalCapacity);
        stats.put("totalLoad", totalLoad);
        stats.put("overallLoadPercentage", totalCapacity > 0 ? (totalLoad * 100.0 / totalCapacity) : 0);

        return stats;
    }

    @Override
    public Map<String, Object> getRegionCdnStats(String region) {
        Map<String, Object> stats = new HashMap<>();

        // region参数现在作为regionId使用
        try {
            Long regionId = Long.parseLong(region);
            List<CdnNode> regionNodes = cdnNodeRepository.findByRegionIdAndIsDeletedFalse(regionId, Pageable.unpaged()).getContent();

            long totalNodes = regionNodes.size();
            long onlineNodes = regionNodes.stream()
                    .filter(n -> n.getStatus() == CdnNode.NodeStatus.ONLINE)
                    .count();

            stats.put("regionId", regionId);
            stats.put("totalNodes", totalNodes);
            stats.put("onlineNodes", onlineNodes);
            stats.put("healthRate", totalNodes > 0 ? (onlineNodes * 100.0 / totalNodes) : 0);

            int totalCapacity = regionNodes.stream()
                    .filter(n -> n.getCapacity() != null)
                    .mapToInt(CdnNode::getCapacity)
                    .sum();
            int totalLoad = regionNodes.stream()
                    .filter(n -> n.getCurrentLoad() != null)
                    .mapToInt(CdnNode::getCurrentLoad)
                    .sum();

            stats.put("totalCapacity", totalCapacity);
            stats.put("totalLoad", totalLoad);
            stats.put("overallLoadPercentage", totalCapacity > 0 ? (totalLoad * 100.0 / totalCapacity) : 0);
        } catch (NumberFormatException e) {
            stats.put("region", region);
            stats.put("error", "Invalid region ID");
        }

        return stats;
    }

    @Override
    public List<CdnNodeDTO> getHealthyNodes() {
        return cdnNodeRepository.findByStatusAndIsDeletedFalseAndIsEnabledTrueOrderByWeightDesc(CdnNode.NodeStatus.ONLINE)
                .stream()
                .filter(CdnNode::isHealthy)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CdnNode> getAllNodes() {
        return cdnNodeRepository.findByIsDeletedFalse();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成唯一节点标识符
     */
    private String generateNodeId() {
        return "cdn-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 将实体转换为DTO
     */
    private CdnNodeDTO convertToDTO(CdnNode cdnNode) {
        CdnNodeDTO.CdnNodeDTOBuilder builder = CdnNodeDTO.builder()
                .id(cdnNode.getId())
                .nodeId(cdnNode.getNodeId())
                .name(cdnNode.getName())
                .ipAddress(cdnNode.getIpAddress())
                .port(cdnNode.getPort())
                .location(cdnNode.getLocation())
                .regionId(cdnNode.getRegionId())
                .status(cdnNode.getStatus().toString())
                .capacity(cdnNode.getCapacity())
                .currentLoad(cdnNode.getCurrentLoad())
                .lastHeartbeat(cdnNode.getLastHeartbeat() != null ? cdnNode.getLastHeartbeat().toString() : null)
                .cpuUsage(cdnNode.getCpuUsage())
                .memoryUsage(cdnNode.getMemoryUsage())
                .bandwidthUsage(cdnNode.getBandwidthUsage())
                .storageUsage(cdnNode.getStorageUsage())
                .upBandwidth(cdnNode.getUpBandwidth())
                .downBandwidth(cdnNode.getDownBandwidth())
                .weight(cdnNode.getWeight())
                .priority(cdnNode.getPriority())
                .healthCheckUrl(cdnNode.getHealthCheckUrl())
                .isEnabled(cdnNode.getIsEnabled())
                .loadPercentage(calculateLoadPercentage(cdnNode))
                .wlcScore(cdnNode.calculateWlcScore())
                .createdAt(cdnNode.getCreatedAt())
                .updatedAt(cdnNode.getUpdatedAt());
        
        // 设置区域名称
        if (cdnNode.getRegionId() != null) {
            regionRepository.findById(cdnNode.getRegionId())
                .ifPresent(region -> builder.regionName(region.getName()));
        }
        
        return builder.build();
    }

    /**
     * 将负载历史实体转换为DTO
     */
    private CdnNodeLoadDTO convertLoadToDTO(CdnNodeLoad load) {
        return CdnNodeLoadDTO.builder()
                .id(load.getId())
                .cdnNodeId(load.getCdnNodeId())
                .recordedAt(load.getRecordedAt())
                .currentLoad(load.getCurrentLoad())
                .cpuUsage(load.getCpuUsage())
                .memoryUsage(load.getMemoryUsage())
                .bandwidthUsage(load.getBandwidthUsage())
                .storageUsage(load.getStorageUsage())
                .upBandwidth(load.getUpBandwidth())
                .downBandwidth(load.getDownBandwidth())
                .activeConnections(load.getActiveConnections())
                .requestRate(load.getRequestRate())
                .bandwidthThroughput(load.getBandwidthThroughput())
                .cacheHitRate(load.getCacheHitRate())
                .avgResponseTime(load.getAvgResponseTime())
                .errorRate(load.getErrorRate())
                .status(load.getStatus() != null ? load.getStatus().toString() : null)
                .loadPercentage(load.getLoadPercentage())
                .createdAt(load.getCreatedAt())
                .build();
    }

    /**
     * 计算负载百分比
     */
    private double calculateLoadPercentage(CdnNode node) {
        if (node.getCapacity() == null || node.getCapacity() == 0) {
            return 0.0;
        }
        return (node.getCurrentLoad() != null ? node.getCurrentLoad() : 0) * 100.0 / node.getCapacity();
    }

    /**
     * 执行连通性测试
     */
    private CdnNodeConnectivityTestDTO performConnectivityTest(CdnNode node) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String status = "UNKNOWN";
        String errorMessage = null;
        CdnNodeConnectivityTestDTO.TestDetails.TestDetailsBuilder detailsBuilder = CdnNodeConnectivityTestDTO.TestDetails.builder();

        try {
            URL url = new URL("http://" + node.getIpAddress() + ":" + node.getPort());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(node.getConnectTimeout() != null ? node.getConnectTimeout() : 5000);
            connection.setReadTimeout(node.getReadTimeout() != null ? node.getReadTimeout() : 10000);
            connection.setRequestMethod("GET");

            long ttfb = System.currentTimeMillis() - startTime;
            int responseCode = connection.getResponseCode();

            detailsBuilder
                    .httpStatusCode(responseCode)
                    .ttfbMs(ttfb);

            if (responseCode >= 200 && responseCode < 400) {
                success = true;
                status = "HEALTHY";
            } else if (responseCode >= 400 && responseCode < 500) {
                status = "DEGRADED";
                errorMessage = "Client error: " + responseCode;
            } else {
                status = "DEGRADED";
                errorMessage = "Server error: " + responseCode;
            }

            connection.disconnect();
        } catch (java.net.SocketTimeoutException e) {
            success = false;
            status = "TIMEOUT";
            errorMessage = "Connection timeout";
        } catch (java.net.UnknownHostException e) {
            success = false;
            status = "DNS_ERROR";
            errorMessage = "Unknown host: " + e.getMessage();
        } catch (java.net.ConnectException e) {
            success = false;
            status = "UNREACHABLE";
            errorMessage = "Connection refused";
        } catch (Exception e) {
            success = false;
            status = "ERROR";
            errorMessage = e.getMessage();
        }

        long responseTime = System.currentTimeMillis() - startTime;

        return CdnNodeConnectivityTestDTO.builder()
                .nodeId(node.getId())
                .nodeName(node.getName())
                .ipAddress(node.getIpAddress())
                .success(success)
                .status(status)
                .responseTimeMs(responseTime)
                .errorMessage(errorMessage)
                .testTimestamp(System.currentTimeMillis())
                .suggestion(generateTestSuggestion(success, status))
                .details(detailsBuilder.build())
                .build();
    }

    /**
     * 判断健康状态
     */
    private String determineHealthStatus(CdnNode node) {
        double loadPercentage = calculateLoadPercentage(node);

        if (node.getStatus() != CdnNode.NodeStatus.ONLINE) {
            return "UNHEALTHY";
        }
        if (!Boolean.TRUE.equals(node.getIsEnabled())) {
            return "DISABLED";
        }
        if (loadPercentage > MAX_LOAD_THRESHOLD) {
            return "OVERLOADED";
        }
        if (loadPercentage > 70) {
            return "WARNING";
        }
        if (node.getCpuUsage() != null && node.getCpuUsage() > 90) {
            return "WARNING";
        }
        if (node.getMemoryUsage() != null && node.getMemoryUsage() > 90) {
            return "WARNING";
        }
        return "HEALTHY";
    }

    /**
     * 生成建议
     */
    private String generateSuggestion(CdnNode node, String healthStatus) {
        double loadPercentage = calculateLoadPercentage(node);

        switch (healthStatus) {
            case "OVERLOADED":
                return "节点负载过高，建议将部分流量迁移到其他节点";
            case "WARNING":
                if (loadPercentage > 70) {
                    return "节点负载接近阈值，建议关注并准备扩容";
                }
                if (node.getCpuUsage() != null && node.getCpuUsage() > 80) {
                    return "CPU使用率较高，建议检查节点运行状态";
                }
                if (node.getMemoryUsage() != null && node.getMemoryUsage() > 80) {
                    return "内存使用率较高，建议检查是否存在内存泄漏";
                }
                return "节点状态一般，建议持续监控";
            case "UNHEALTHY":
                return "节点不可用，建议检查网络连接和节点服务状态";
            case "DISABLED":
                return "节点已禁用，如需启用请手动开启";
            default:
                return "节点运行正常";
        }
    }

    /**
     * 生成测试建议
     */
    private String generateTestSuggestion(boolean success, String status) {
        if (success) {
            return "连接正常";
        }
        switch (status) {
            case "TIMEOUT":
                return "网络延迟过高，建议检查网络连接";
            case "DNS_ERROR":
                return "DNS解析失败，检查域名配置";
            case "UNREACHABLE":
                return "节点不可达，检查防火墙和网络配置";
            case "ERROR":
                return "连接异常，需要进一步诊断";
            default:
                return "连接失败，建议手动检查";
        }
    }

    /**
     * 序列化额外数据
     */
    private String serializeExtraData(Map<String, Object> extraData) {
        if (extraData == null || extraData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extraData);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize extra data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有子区域ID（递归）
     */
    private List<Long> getAllDescendantRegionIds(Long regionId) {
        List<Long> result = new ArrayList<>();
        result.add(regionId);

        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(regionId);
        for (Region child : children) {
            result.addAll(getAllDescendantRegionIds(child.getId()));
        }

        return result;
    }
}
