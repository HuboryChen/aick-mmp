package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.converter.EdgeNodeConverter;
import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Region;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.EdgeNodeService;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.aick.mmp.central.service.NetworkMonitorService;

import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EdgeNodeServiceImpl implements EdgeNodeService {

    @Autowired
    private EdgeNodeConverter edgeNodeConverter;

    private final EdgeNodeRepository edgeNodeRepository;
    private final RegionRepository regionRepository;
    private final NetworkMonitorService networkMonitorService;
    private final CameraRepository cameraRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Override
    public Page<EdgeNodeDTO> getAllEdgeNodes(Pageable pageable) {
        return edgeNodeRepository.findAll(pageable)
                .map(edgeNodeConverter::convertToDTO);
    }

    @Override
    public Page<EdgeNodeDTO> getEdgeNodesByLocation(String location, Pageable pageable) {
        return edgeNodeRepository.findByLocation(location, pageable)
                .map(edgeNodeConverter::convertToDTO);
    }

    @Override
    public Page<EdgeNodeDTO> getEdgeNodesByStatus(EdgeNode.NodeStatus status, Pageable pageable) {
        return edgeNodeRepository.findByStatus(status, pageable)
                .map(edgeNodeConverter::convertToDTO);
    }

    @Override
    public Page<EdgeNodeDTO> getEdgeNodesByRegionId(Long regionId, boolean recursive, Pageable pageable) {
        if (recursive) {
            // Get all descendant region IDs
            List<Long> regionIds = getAllDescendantRegionIds(regionId);
            return edgeNodeRepository.findAll((root, query, cb) -> 
                root.get("regionId").in(regionIds), pageable)
                .map(edgeNodeConverter::convertToDTO);
        } else {
            return edgeNodeRepository.findByRegionId(regionId, pageable)
                .map(edgeNodeConverter::convertToDTO);
        }
    }

    @Override
    public EdgeNodeDTO getEdgeNodeById(Long id) {
        EdgeNode edgeNode = edgeNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + id));
        return edgeNodeConverter.convertToDTO(edgeNode);
    }

    @Override
    @Transactional
    public EdgeNodeDTO createEdgeNode(EdgeNodeDTO edgeNodeDTO) {
        // 检查IP地址是否已存在
        if (edgeNodeRepository.findByIpAddress(edgeNodeDTO.getIpAddress()).isPresent()) {
            throw new ServiceException("Edge node with this IP address already exists: " + edgeNodeDTO.getIpAddress());
        }

        EdgeNode edgeNode = edgeNodeConverter.convertToEntity(edgeNodeDTO);
        if (edgeNode.getUuid() == null) {
            edgeNode.setUuid(generateNodeUuid());
        }
        edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
        edgeNode.setLastHeartbeatTime(LocalDateTime.now());
        edgeNode.setCreatedAt(LocalDateTime.now());
        edgeNode.setUpdatedAt(LocalDateTime.now());

        EdgeNode savedNode = edgeNodeRepository.save(edgeNode);
        log.info("Created new edge node: {} ({})", savedNode.getName(), savedNode.getIpAddress());
        return edgeNodeConverter.convertToDTO(savedNode);
    }

    @Override
    @Transactional
    public EdgeNodeDTO registerEdgeNode(EdgeNodeDTO edgeNodeDTO) {
        // 检查节点是否已存在（通过名称或UUID）
        Optional<EdgeNode> existingNodeOpt = edgeNodeRepository.findByName(edgeNodeDTO.getName());
        if (!existingNodeOpt.isPresent() && StringUtils.hasText(edgeNodeDTO.getUuid())) {
            existingNodeOpt = edgeNodeRepository.findByUuid(edgeNodeDTO.getUuid());
        }

        EdgeNode edgeNode;
        if (existingNodeOpt.isPresent()) {
            // 更新现有节点
            edgeNode = existingNodeOpt.get();
            edgeNodeConverter.updateEntityFromDTO(edgeNodeDTO, edgeNode);
            edgeNode.setLastHeartbeatTime(LocalDateTime.now());
            edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
            edgeNode.setUpdatedAt(LocalDateTime.now());
            log.info("Updated existing edge node: {} ({})", edgeNode.getName(), edgeNode.getIpAddress());
        } else {
            // 创建新节点
            edgeNode = edgeNodeConverter.convertToEntity(edgeNodeDTO);
            if (edgeNode.getUuid() == null) {
                edgeNode.setUuid(StringUtils.hasText(edgeNodeDTO.getUuid()) ? edgeNodeDTO.getUuid() : generateNodeUuid());
            }
            edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
            edgeNode.setLastHeartbeatTime(LocalDateTime.now());
            edgeNode.setCreatedAt(LocalDateTime.now());
            edgeNode.setUpdatedAt(LocalDateTime.now());
            edgeNode.setEnabled(true);
            log.info("Registered new edge node: {} ({})", edgeNode.getName(), edgeNode.getIpAddress());
        }

        EdgeNode savedNode = edgeNodeRepository.save(edgeNode);
        return edgeNodeConverter.convertToDTO(savedNode);
    }

    @Override
    @Transactional
    public EdgeNodeDTO updateEdgeNode(Long id, EdgeNodeDTO edgeNodeDTO) {
        EdgeNode edgeNode = edgeNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + id));

        // 检查IP地址和端口是否已被其他节点使用
        if (!edgeNode.getIpAddress().equals(edgeNodeDTO.getIpAddress()) &&
                edgeNodeRepository.existsByIpAddressAndPort(edgeNodeDTO.getIpAddress(), edgeNodeDTO.getPort())) {
            throw new ServiceException("Edge node with this IP address and port already exists: " + edgeNodeDTO.getIpAddress() + ":" + edgeNodeDTO.getPort());
        }

        edgeNodeConverter.updateEntityFromDTO(edgeNodeDTO, edgeNode);
        edgeNode.setUpdatedAt(LocalDateTime.now());

        EdgeNode updatedNode = edgeNodeRepository.save(edgeNode);
        return edgeNodeConverter.convertToDTO(updatedNode);
    }

    @Override
    @Transactional
    public void updateEdgeNodeStatus(Long id, EdgeNodeStatusUpdateDTO statusUpdateDTO) {
        EdgeNode edgeNode = edgeNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + id));

        EdgeNode.NodeStatus newStatus = EdgeNode.NodeStatus.valueOf(statusUpdateDTO.getStatus().toUpperCase());
        edgeNode.setStatus(newStatus);
        edgeNode.setLastHeartbeatTime(LocalDateTime.now());
        edgeNode.setUpdatedAt(LocalDateTime.now());

        edgeNodeRepository.save(edgeNode);
        log.info("Updated edge node status: {} (ID: {}) - {}", edgeNode.getName(), id, newStatus);
    }

    @Override
    @Transactional
    public void updateEdgeNodeCredentials(Long id, String username, String password) {
        // Edge nodes now use AK/SK authentication instead of username/password
        // This method is deprecated and will be removed in future versions
        throw new UnsupportedOperationException(
            "Edge node credentials are now managed via SystemApp API keys. " +
            "Please use the /api-keys/system endpoint to manage authentication."
        );
    }
    

    @Override
    @Transactional
    public void deleteEdgeNode(Long id) {
        if (!edgeNodeRepository.existsById(id)) {
            throw new ServiceException("Edge node not found with id: " + id);
        }

        edgeNodeRepository.deleteById(id);
        log.info("Deleted edge node with id: {}", id);
    }

    @Override
    @Transactional
    public void registerHeartbeat(Long nodeId, Map<String, Object> metrics) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));

        updateNodeHeartbeat(edgeNode, metrics);
    }

    @Override
    @Transactional
    public void registerHeartbeatByNodeId(String nodeId, Map<String, Object> metrics) {
        try {
            // Try to find by name first (most common case)
            Optional<EdgeNode> edgeNodeOpt = edgeNodeRepository.findByName(nodeId);

            // If not found by name, try by UUID
            if (!edgeNodeOpt.isPresent()) {
                edgeNodeOpt = edgeNodeRepository.findByUuid(nodeId);
            }

            EdgeNode edgeNode = edgeNodeOpt.orElseThrow(() ->
                    new ServiceException("Edge node not found with nodeId: " + nodeId));

            updateNodeHeartbeat(edgeNode, metrics);
        } catch (Exception e) {
            log.error("Error registering heartbeat for node: " + nodeId, e);
            throw e;
        }
    }

    private void updateNodeHeartbeat(EdgeNode edgeNode, Map<String, Object> metrics) {
        // 更新心跳时间和状态
        edgeNode.setLastHeartbeatTime(LocalDateTime.now());
        edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
        edgeNode.setUpdatedAt(LocalDateTime.now());

        // 从metrics中提取顶层指标并设置到实体字段（解决前端显示null的问题）
        if (metrics.containsKey("cpuUsage")) {
            edgeNode.setCpuUsage(parseDouble(metrics.get("cpuUsage")));
        }
        if (metrics.containsKey("memoryUsage")) {
            edgeNode.setMemoryUsage(parseDouble(metrics.get("memoryUsage")));
        }
        if (metrics.containsKey("storageUsage")) {
            edgeNode.setStorageUsage(parseDouble(metrics.get("storageUsage")));
        }
        if (metrics.containsKey("softwareVersion")) {
            edgeNode.setSoftwareVersion(String.valueOf(metrics.get("softwareVersion")));
        }
        if (metrics.containsKey("hardwareInfo")) {
            edgeNode.setHardwareInfo(String.valueOf(metrics.get("hardwareInfo")));
        }
        if (metrics.containsKey("currentCameraCount")) {
            edgeNode.setCurrentCameraCount(parseInteger(metrics.get("currentCameraCount")));
        }
        if (metrics.containsKey("networkBandwidth")) {
            edgeNode.setNetworkBandwidth(String.valueOf(metrics.get("networkBandwidth")));
        }

        // 保留完整指标到systemMetrics JSON字段
        if (edgeNode.getSystemMetrics() == null) {
            edgeNode.setSystemMetrics(new HashMap<>());
        }
        edgeNode.getSystemMetrics().putAll(metrics);

        edgeNodeRepository.save(edgeNode);

        // 分析网络指标并可能触发调整
        if (metrics.containsKey("network")) {
            networkMonitorService.evaluateAndAdjust(edgeNode, (Map<String, Double>) metrics.get("network"));
        }

        log.debug("Heartbeat registered for edge node: {} ({})", edgeNode.getName(), edgeNode.getId());
    }

    /**
     * 安全解析Double类型值
     */
    private Double parseDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全解析Integer类型值
     */
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getEdgeNodeStatistics(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));

        // 在实际实现中，这应该从监控系统获取实时统计数据
        Map<String, Object> result = new HashMap<>();
        result.put("id", edgeNode.getId());
        result.put("name", edgeNode.getName());
        result.put("status", edgeNode.getStatus());
        result.put("lastHeartbeatTime", edgeNode.getLastHeartbeatTime());
        result.put("currentCameraCount", edgeNode.getCurrentCameraCount());
        result.put("maxCameraSupport", edgeNode.getMaxCameraSupport());
        result.put("cpuUsage", edgeNode.getCpuUsage());
        result.put("memoryUsage", edgeNode.getMemoryUsage());
        result.put("storageUsage", edgeNode.getStorageUsage());
        result.put("networkBandwidth", edgeNode.getNetworkBandwidth());
        result.put("systemMetrics", edgeNode.getSystemMetrics() != null ? edgeNode.getSystemMetrics() : new HashMap<>());
        result.put("networkMetrics", edgeNode.getSystemMetrics() != null ? edgeNode.getSystemMetrics().get("network") : null);
        
        // 添加基本信息字段
        result.put("uuid", edgeNode.getUuid());
        result.put("location", edgeNode.getLocation());
        result.put("ipAddress", edgeNode.getIpAddress());
        result.put("port", edgeNode.getPort());
        result.put("softwareVersion", edgeNode.getSoftwareVersion());
        result.put("hardwareInfo", edgeNode.getHardwareInfo());
        result.put("enabled", edgeNode.isEnabled());
        result.put("createdAt", edgeNode.getCreatedAt());
        result.put("updatedAt", edgeNode.getUpdatedAt());
        
        return result;
    }

    @Override
    public List<EdgeNodeDTO> getOnlineEdgeNodes() {
        return edgeNodeRepository.findByStatusAndEnabled(EdgeNode.NodeStatus.ONLINE, true)
                .stream()
                .map(edgeNodeConverter::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getEdgeNodeCountByStatus(EdgeNode.NodeStatus status) {
        if (status != null) {
            return edgeNodeRepository.countByStatus(status);
        } else {
            return edgeNodeRepository.count();
        }
    }
    
    @Override
    public long getEdgeNodeCount() {
        log.info("Counting all edge nodes");
        return edgeNodeRepository.count();
    }

    @Override
    public boolean testEdgeNodeConnection(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));

        // 简单的连接测试实现
        try {
            // 实际实现中应该有真实的网络连接测试
            log.info("Testing connection to edge node: {} ({})", edgeNode.getName(), edgeNode.getIpAddress());
            return true; // 假设连接测试成功
        } catch (Exception e) {
            log.error("Connection test failed for edge node {}: {}", nodeId, e.getMessage());
            return false;
        }
    }

    @Override
    public void restartEdgeNodeService(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));

        if (edgeNode.getStatus() != EdgeNode.NodeStatus.ONLINE) {
            throw new ServiceException("Cannot restart offline edge node. Please ensure the node is online.");
        }

        String edgeNodeUrl = String.format("http://%s:%d/api/edge/heartbeat/restart",
                edgeNode.getIpAddress(), edgeNode.getPort());

        log.info("Sending restart command to edge node: {} ({}:{})", edgeNode.getName(), edgeNode.getIpAddress(), edgeNode.getPort());

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    edgeNodeUrl,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Restart command sent successfully to edge node: {}", edgeNode.getName());
            } else {
                log.warn("Unexpected response from edge node {}: {}", edgeNode.getName(), response.getStatusCode());
                throw new ServiceException("Failed to send restart command to edge node: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error sending restart command to edge node {}: {}", edgeNode.getName(), e.getMessage());
            throw new ServiceException("Failed to send restart command: " + e.getMessage());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error to edge node {}: {}", edgeNode.getName(), e.getMessage());
            throw new ServiceException("Cannot connect to edge node: " + e.getMessage());
        }
    }
    
    @Override
    public Page<EdgeNodeDTO> searchEdgeNodes(String keyword, EdgeNode.NodeStatus status, String location, Long regionId, boolean recursive, Pageable pageable) {
        // 使用Specification进行动态查询
        return edgeNodeRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (StringUtils.hasText(keyword)) {
                Predicate namePredicate = cb.like(root.get("name"), "%" + keyword + "%");
                Predicate ipPredicate = cb.like(root.get("ipAddress"), "%" + keyword + "%");
                predicates.add(cb.or(namePredicate, ipPredicate));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (StringUtils.hasText(location)) {
                predicates.add(cb.equal(root.get("location"), location));
            }
            
            if (regionId != null) {
                if (recursive) {
                    List<Long> regionIds = getAllDescendantRegionIds(regionId);
                    predicates.add(root.get("regionId").in(regionIds));
                } else {
                    predicates.add(cb.equal(root.get("regionId"), regionId));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(edgeNodeConverter::convertToDTO);
    }
    
    @Override
    @Transactional
    public void batchDeleteEdgeNodes(List<Long> nodeIds) {
        log.info("批量删除边缘节点，节点ID: {}", nodeIds);
        
        List<EdgeNode> nodes = edgeNodeRepository.findAllById(nodeIds);
        if (nodes.isEmpty()) {
            throw new ServiceException("未找到指定的边缘节点");
        }
        
        // 获取节点名称用于日志
        List<String> nodeNames = nodes.stream()
                .map(EdgeNode::getName)
                .collect(Collectors.toList());
        
        edgeNodeRepository.deleteAllById(nodeIds);
        log.info("成功批量删除 {} 个边缘节点: {}", nodeNames.size(), nodeNames);
    }
    
    @Override
    @Transactional
    public void batchEnableEdgeNodes(List<Long> nodeIds, boolean enabled) {
        log.info("批量{}边缘节点，节点ID: {}", enabled ? "启用" : "禁用", nodeIds);
        
        List<EdgeNode> nodes = edgeNodeRepository.findAllById(nodeIds);
        if (nodes.isEmpty()) {
            throw new ServiceException("未找到指定的边缘节点");
        }
        
        nodes.forEach(node -> {
            node.setEnabled(enabled);
            node.setUpdatedAt(LocalDateTime.now());
        });
        
        edgeNodeRepository.saveAll(nodes);
        
        List<String> nodeNames = nodes.stream()
                .map(EdgeNode::getName)
                .collect(Collectors.toList());
        log.info("成功批量{} {} 个边缘节点: {}", enabled ? "启用" : "禁用", nodeNames.size(), nodeNames);
    }
    
    @Override
    @Transactional
    public void batchUpdateEdgeNodeStatus(List<Long> nodeIds, EdgeNode.NodeStatus status) {
        log.info("批量更新边缘节点状态为 {}，节点ID: {}", status, nodeIds);
        
        List<EdgeNode> nodes = edgeNodeRepository.findAllById(nodeIds);
        if (nodes.isEmpty()) {
            throw new ServiceException("未找到指定的边缘节点");
        }
        
        nodes.forEach(node -> {
            node.setStatus(status);
            node.setUpdatedAt(LocalDateTime.now());
        });
        
        edgeNodeRepository.saveAll(nodes);
        
        List<String> nodeNames = nodes.stream()
                .map(EdgeNode::getName)
                .collect(Collectors.toList());
        log.info("成功批量更新 {} 个边缘节点的状态: {}", nodeNames.size(), nodeNames);
    }
    
    @Override
    public String checkNodeHealthStatus(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));
        
        // 简单的健康检查逻辑
        if (!edgeNode.isEnabled()) {
            return "DISABLED";
        }
        
        if (edgeNode.getStatus() == EdgeNode.NodeStatus.OFFLINE) {
            return "OFFLINE";
        }
        
        if (edgeNode.getLastHeartbeatTime() == null) {
            return "NO_HEARTBEAT";
        }
        
        Duration duration = Duration.between(edgeNode.getLastHeartbeatTime(), LocalDateTime.now());
        if (duration.toMinutes() >= 3) { // 3分钟未收到心跳
            return "HEARTBEAT_TIMEOUT";
        }
        
        // 检查系统指标
        if (edgeNode.getSystemMetrics() != null && !edgeNode.getSystemMetrics().isEmpty()) {
            Map<String, Object> metrics = edgeNode.getSystemMetrics();
            
            // 检查CPU
            if (metrics.containsKey("cpu_usage")) {
                double cpuUsage = Double.parseDouble(metrics.get("cpu_usage").toString());
                if (cpuUsage > 95.0) return "HIGH_CPU";
                if (cpuUsage > 80.0) return "ELEVATED_CPU";
            }
            
            // 检查内存
            if (metrics.containsKey("memory_usage")) {
                double memoryUsage = Double.parseDouble(metrics.get("memory_usage").toString());
                if (memoryUsage > 95.0) return "HIGH_MEMORY";
                if (memoryUsage > 80.0) return "ELEVATED_MEMORY";
            }
            
            // 检查存储
            if (metrics.containsKey("storage_usage")) {
                double storageUsage = Double.parseDouble(metrics.get("storage_usage").toString());
                if (storageUsage > 95.0) return "HIGH_STORAGE";
                if (storageUsage > 80.0) return "ELEVATED_STORAGE";
            }
        }
        
        // 检查摄像头负载
        if (edgeNode.getMaxCameraSupport() != null && edgeNode.getCurrentCameraCount() != null) {
            double loadPercentage = (edgeNode.getCurrentCameraCount() * 100.0) / edgeNode.getMaxCameraSupport();
            if (loadPercentage >= 100.0) return "OVERLOADED";
            if (loadPercentage >= 80.0) return "HIGH_LOAD";
        }
        
        return "HEALTHY";
    }
    
    @Override
    public Map<String, Object> getNodeHealthDetails(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));
        
        Map<String, Object> details = new HashMap<>();
        details.put("nodeId", edgeNode.getId());
        details.put("nodeName", edgeNode.getName());
        details.put("status", edgeNode.getStatus());
        details.put("enabled", edgeNode.isEnabled());
        details.put("lastHeartbeatTime", edgeNode.getLastHeartbeatTime());
        
        // 计算距离上次心跳的时间
        if (edgeNode.getLastHeartbeatTime() != null) {
            Duration duration = Duration.between(edgeNode.getLastHeartbeatTime(), LocalDateTime.now());
            details.put("secondsSinceLastHeartbeat", duration.getSeconds());
            details.put("minutesSinceLastHeartbeat", duration.toMinutes());
        }
        
        // 系统指标
        details.put("cpuUsage", edgeNode.getCpuUsage());
        details.put("memoryUsage", edgeNode.getMemoryUsage());
        details.put("storageUsage", edgeNode.getStorageUsage());
        details.put("systemMetrics", edgeNode.getSystemMetrics() != null ? edgeNode.getSystemMetrics() : new HashMap<>());
        
        // 摄像头负载
        details.put("currentCameraCount", edgeNode.getCurrentCameraCount());
        details.put("maxCameraSupport", edgeNode.getMaxCameraSupport());
        if (edgeNode.getCurrentCameraCount() != null && edgeNode.getMaxCameraSupport() != null) {
            double loadPercentage = (edgeNode.getCurrentCameraCount() * 100.0) / edgeNode.getMaxCameraSupport();
            details.put("cameraLoadPercentage", Math.round(loadPercentage * 100.0) / 100.0);
        }
        
        // 硬件信息
        details.put("hardwareInfo", edgeNode.getHardwareInfo());
        details.put("softwareVersion", edgeNode.getSoftwareVersion());
        details.put("networkBandwidth", edgeNode.getNetworkBandwidth());
        
        // 健康评分和状态
        String healthStatus = checkNodeHealthStatus(nodeId);
        details.put("healthStatus", healthStatus);
        
        // 根据健康状态设置评分
        int healthScore;
        switch (healthStatus) {
            case "HEALTHY": healthScore = 100; break;
            case "ELEVATED_CPU":
            case "ELEVATED_MEMORY":
            case "ELEVATED_STORAGE": healthScore = 80; break;
            case "HIGH_LOAD": healthScore = 70; break;
            case "HIGH_CPU":
            case "HIGH_MEMORY":
            case "HIGH_STORAGE": healthScore = 50; break;
            case "HEARTBEAT_TIMEOUT": healthScore = 30; break;
            case "OVERLOADED": healthScore = 20; break;
            case "OFFLINE": healthScore = 10; break;
            case "NO_HEARTBEAT": healthScore = 0; break;
            case "DISABLED": healthScore = -1; break;
            default: healthScore = 60;
        }
        details.put("healthScore", healthScore);
        
        return details;
    }
    
    /**
     * 生成唯一的边缘节点UUID
     */
    private String generateNodeUuid() {
        return "edge-" + UUID.randomUUID().toString().substring(0, 8);
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
    
    @Override
    @Transactional
    public Map<String, Object> processCameraStatusReports(String nodeId, List<Map<String, Object>> cameraStatuses) {
        log.info("处理边缘节点 {} 上报的 {} 个摄像头状态", nodeId, cameraStatuses.size());
        
        // 查找边缘节点
        Optional<EdgeNode> edgeNodeOpt = edgeNodeRepository.findByName(nodeId);
        if (!edgeNodeOpt.isPresent()) {
            edgeNodeOpt = edgeNodeRepository.findByUuid(nodeId);
        }
        
        if (!edgeNodeOpt.isPresent()) {
            log.warn("边缘节点不存在，跳过摄像头状态处理: {}", nodeId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Edge node not found");
            return result;
        }
        
        EdgeNode edgeNode = edgeNodeOpt.get();
        int processedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Map<String, Object> status : cameraStatuses) {
            try {
                String cameraIdStr = String.valueOf(status.get("cameraId"));
                Long cameraId = Long.parseLong(cameraIdStr);
                
                Optional<Camera> cameraOpt = cameraRepository.findById(cameraId);
                if (cameraOpt.isEmpty()) {
                    log.warn("摄像头不存在，跳过状态更新: {}", cameraId);
                    errorCount++;
                    errors.add("Camera not found: " + cameraId);
                    continue;
                }
                
                Camera camera = cameraOpt.get();
                
                // 验证摄像头是否属于该边缘节点
                if (!edgeNode.getId().equals(camera.getEdgeNodeId())) {
                    log.warn("摄像头 {} 不属于边缘节点 {}，跳过状态更新", cameraId, edgeNode.getId());
                    continue;
                }
                
                // 更新摄像头状态
                if (status.containsKey("status")) {
                    String statusStr = String.valueOf(status.get("status"));
                    Camera.CameraStatus newStatus = Camera.CameraStatus.valueOf(statusStr.toUpperCase());
                    camera.setStatus(newStatus);
                }
                
                // 更新其他指标
                if (status.containsKey("currentBitrate")) {
                    Object bitrate = status.get("currentBitrate");
                    if (bitrate instanceof Number) {
                        camera.setCurrentBitrate(((Number) bitrate).intValue());
                    }
                }
                
                if (status.containsKey("currentFps")) {
                    Object fps = status.get("currentFps");
                    if (fps instanceof Number) {
                        camera.setCurrentFps(((Number) fps).doubleValue());
                    }
                }
                
                if (status.containsKey("errorCode")) {
                    camera.setLastErrorCode(String.valueOf(status.get("errorCode")));
                }
                
                if (status.containsKey("errorMessage")) {
                    camera.setLastErrorMessage(String.valueOf(status.get("errorMessage")));
                }
                
                // 更新最后心跳时间
                camera.setLastHeartbeatTime(LocalDateTime.now());
                
                cameraRepository.save(camera);
                processedCount++;
                
                log.debug("更新摄像头 {} 状态成功", cameraId);
                
            } catch (Exception e) {
                log.error("处理摄像头状态失败: {}", status, e);
                errorCount++;
                errors.add("Error processing: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("processedCount", processedCount);
        result.put("errorCount", errorCount);
        result.put("totalCount", cameraStatuses.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        
        log.info("边缘节点 {} 摄像头状态处理完成: 成功 {} 个, 失败 {} 个", 
                 nodeId, processedCount, errorCount);
        
        return result;
    }
}