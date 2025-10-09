package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.converter.EdgeNodeConverter;
import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.EdgeNode.NodeStatus;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.EdgeNodeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.aick.mmp.central.service.NetworkMonitorService;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EdgeNodeConverter edgeNodeConverter;

    private final EdgeNodeRepository edgeNodeRepository;
    private final NetworkMonitorService networkMonitorService;

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
        // 验证输入参数
        if (StrUtil.isBlank(username)) {
            throw new ServiceException("Username cannot be blank");
        }
        if (StrUtil.isBlank(password)) {
            throw new ServiceException("Password cannot be blank");
        }

        EdgeNode edgeNode = edgeNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + id));

        // 加密存储密码
        String encodedPassword = passwordEncoder.encode(password);
        edgeNode.setAuthUsername(username);
        edgeNode.setAuthPassword(encodedPassword);
        edgeNode.setUpdatedAt(LocalDateTime.now());

        edgeNodeRepository.save(edgeNode);
        log.info("Updated credentials for edge node: {} (ID: {})", edgeNode.getName(), id);
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
        // 更新心跳时间和指标
        edgeNode.setLastHeartbeatTime(LocalDateTime.now());
        if (edgeNode.getSystemMetrics() == null) {
            edgeNode.setSystemMetrics(new HashMap<>());
        }
        edgeNode.getSystemMetrics().putAll(metrics);
        edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE); // 心跳意味着节点在线
        edgeNode.setUpdatedAt(LocalDateTime.now());

        edgeNodeRepository.save(edgeNode);

        // 分析网络指标并可能触发调整
        if (metrics.containsKey("network")) {
            networkMonitorService.evaluateAndAdjust(edgeNode, (Map<String, Double>) metrics.get("network"));
        }
        
        log.debug("Heartbeat registered for edge node: {} ({})", edgeNode.getName(), edgeNode.getId());
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

        // 在实际实现中，这应该发送重启命令到边缘节点
        log.info("Sending restart command to edge node: {} ({})", edgeNode.getName(), edgeNode.getIpAddress());
        // 这里应该有实际的服务重启逻辑
    }

    /**
     * 生成唯一的边缘节点UUID
     */
    private String generateNodeUuid() {
        return "edge-" + UUID.randomUUID().toString().substring(0, 8);
    }

}