package com.aick.mmp.service.impl;

import com.aick.mmp.dto.EdgeNodeDTO;
import com.aick.mmp.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.exception.ServiceException;
import com.aick.mmp.model.EdgeNode;
import com.aick.mmp.model.EdgeNode.NodeStatus;
import com.aick.mmp.repository.EdgeNodeRepository;
import com.aick.mmp.service.EdgeNodeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.aick.mmp.service.NetworkMonitorService;

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


    private final EdgeNodeRepository edgeNodeRepository;
    private final NetworkMonitorService networkMonitorService;

    @Override
    public Page<EdgeNodeDTO> getAllEdgeNodes(Pageable pageable) {
        return edgeNodeRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<EdgeNodeDTO> getEdgeNodesByLocation(String location, Pageable pageable) {
        return edgeNodeRepository.findByLocation(location, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<EdgeNodeDTO> getEdgeNodesByStatus(EdgeNode.NodeStatus status, Pageable pageable) {
        return edgeNodeRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public EdgeNodeDTO getEdgeNodeById(Long id) {
        EdgeNode edgeNode = edgeNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + id));
        return convertToDTO(edgeNode);
    }

    @Override
    @Transactional
    public EdgeNodeDTO createEdgeNode(EdgeNodeDTO edgeNodeDTO) {
        // 检查IP地址是否已存在
        if (edgeNodeRepository.findByIpAddress(edgeNodeDTO.getIpAddress()).isPresent()) {
            throw new ServiceException("Edge node with this IP address already exists: " + edgeNodeDTO.getIpAddress());
        }

        EdgeNode edgeNode = EdgeNode.builder()
                .uuid(generateNodeUuid())
                .name(edgeNodeDTO.getName())
                .ipAddress(edgeNodeDTO.getIpAddress())
                .location(edgeNodeDTO.getLocation())
                .status(EdgeNode.NodeStatus.ONLINE)
                .maxCameraSupport(edgeNodeDTO.getMaxCameraSupport())
                .currentCameraCount(edgeNodeDTO.getCurrentCameraCount())
                .port(edgeNodeDTO.getPort())
                .softwareVersion(edgeNodeDTO.getSoftwareVersion())
                .hardwareInfo(edgeNodeDTO.getHardwareInfo())
                .networkBandwidth(edgeNodeDTO.getNetworkBandwidth())
                .enabled(edgeNodeDTO.isEnabled())
                .lastHeartbeatTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        EdgeNode savedNode = edgeNodeRepository.save(edgeNode);
        log.info("Created new edge node: {} ({})", savedNode.getName(), savedNode.getIpAddress());
        return convertToDTO(savedNode);
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

        edgeNode.setName(edgeNodeDTO.getName());
        edgeNode.setIpAddress(edgeNodeDTO.getIpAddress());
        edgeNode.setLocation(edgeNodeDTO.getLocation());
        edgeNode.setMaxCameraSupport(edgeNodeDTO.getMaxCameraSupport());
        edgeNode.setCurrentCameraCount(edgeNodeDTO.getCurrentCameraCount());
        edgeNode.setUpdatedAt(LocalDateTime.now());

        EdgeNode updatedNode = edgeNodeRepository.save(edgeNode);
        return convertToDTO(updatedNode);
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
    }

    @Override
    public Map<String, Object> getEdgeNodeStatistics(Long nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("Edge node not found with id: " + nodeId));

        // 在实际实现中，这应该从监控系统获取实时统计数据
        return new HashMap<String, Object>() {{
            put("id", edgeNode.getId());
            put("name", edgeNode.getName());
            put("status", edgeNode.getStatus());
            put("lastHeartbeatTime", edgeNode.getLastHeartbeatTime());
            put("currentCameraCount", edgeNode.getCurrentCameraCount());
            put("maxCameraSupport", edgeNode.getMaxCameraSupport());
            put("cpuUsage", edgeNode.getCpuUsage());
            put("memoryUsage", edgeNode.getMemoryUsage());
            put("storageUsage", edgeNode.getStorageUsage());
            put("networkBandwidth", edgeNode.getNetworkBandwidth());
            put("systemMetrics", edgeNode.getSystemMetrics() != null ? edgeNode.getSystemMetrics() : new HashMap<>());
            put("networkMetrics", edgeNode.getSystemMetrics() != null ? edgeNode.getSystemMetrics().get("network") : null);
        }};
    }

    @Override
    public List<EdgeNodeDTO> getOnlineEdgeNodes() {
        return edgeNodeRepository.findByStatusAndEnabled(EdgeNode.NodeStatus.ONLINE, true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getEdgeNodeCountByStatus(EdgeNode.NodeStatus status) {
        return edgeNodeRepository.countByStatus(status);
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

    /**
     * 将实体转换为DTO
     */
    private EdgeNodeDTO convertToDTO(EdgeNode edgeNode) {
        return EdgeNodeDTO.builder()
                .id(edgeNode.getId())
                .uuid(edgeNode.getUuid())
                .name(edgeNode.getName())
                .ipAddress(edgeNode.getIpAddress())
                .location(edgeNode.getLocation())
                .status(edgeNode.getStatus())
                .lastHeartbeatTime(edgeNode.getLastHeartbeatTime())
                .cpuUsage(edgeNode.getCpuUsage())
                .memoryUsage(edgeNode.getMemoryUsage())
                .storageUsage(edgeNode.getStorageUsage())
                .maxCameraSupport(edgeNode.getMaxCameraSupport())
                .currentCameraCount(edgeNode.getCurrentCameraCount())
                .softwareVersion(edgeNode.getSoftwareVersion())
                .hardwareInfo(edgeNode.getHardwareInfo())
                .networkBandwidth(edgeNode.getNetworkBandwidth())
                .enabled(edgeNode.isEnabled())
                .createdAt(edgeNode.getCreatedAt())
                .updatedAt(edgeNode.getUpdatedAt())
                .build();
    }
}