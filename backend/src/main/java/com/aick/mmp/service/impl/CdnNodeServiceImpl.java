package com.aick.mmp.service.impl;

import com.aick.mmp.dto.CdnNodeDTO;
import com.aick.mmp.exception.ServiceException;
import com.aick.mmp.model.CdnNode;
import com.aick.mmp.repository.CdnNodeRepository;
import com.aick.mmp.service.CdnNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CdnNodeServiceImpl implements CdnNodeService {


    private final CdnNodeRepository cdnNodeRepository;



    @Override
    public Page<CdnNodeDTO> getAllCdnNodes(Pageable pageable) {
        return cdnNodeRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<CdnNodeDTO> getCdnNodesByRegion(String region, Pageable pageable) {
        return cdnNodeRepository.findByRegion(region, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<CdnNodeDTO> getCdnNodesByStatus(CdnNode.NodeStatus status, Pageable pageable) {
        return cdnNodeRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public CdnNodeDTO getCdnNodeById(Long id) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));
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
                .name(cdnNodeDTO.getName())
                .ipAddress(cdnNodeDTO.getIpAddress())
                .port(cdnNodeDTO.getPort())
                .location(cdnNodeDTO.getLocation())
                .status(CdnNode.NodeStatus.ONLINE)
                .capacity(cdnNodeDTO.getCapacity())
                .currentLoad(0)
                .lastHeartbeat(LocalDateTime.now())
                .build();

        CdnNode savedNode = cdnNodeRepository.save(cdnNode);
        log.info("Created new CDN node: {} ({}) in region {}",
                savedNode.getName(), savedNode.getIpAddress(), savedNode.getLocation());
        return convertToDTO(savedNode);
    }

    @Override
    @Transactional
    public CdnNodeDTO updateCdnNode(Long id, CdnNodeDTO cdnNodeDTO) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        // 检查IP地址是否已被其他节点使用
        if (!cdnNode.getIpAddress().equals(cdnNodeDTO.getIpAddress()) &&
                cdnNodeRepository.existsByIpAddress(cdnNodeDTO.getIpAddress())) {
            throw new ServiceException("CDN node with this IP address already exists: " + cdnNodeDTO.getIpAddress());
        }

        cdnNode.setName(cdnNodeDTO.getName());
        cdnNode.setIpAddress(cdnNodeDTO.getIpAddress());
        cdnNode.setLocation(cdnNodeDTO.getLocation());
        cdnNode.setCapacity(cdnNodeDTO.getCapacity());
        cdnNode.setLastHeartbeat(LocalDateTime.now());

        CdnNode updatedNode = cdnNodeRepository.save(cdnNode);
        return convertToDTO(updatedNode);
    }

    @Override
    @Transactional
    public void updateCdnNodeStatus(Long id, String status, String message) {
        CdnNode cdnNode = cdnNodeRepository.findById(id)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + id));

        CdnNode.NodeStatus newStatus = CdnNode.NodeStatus.valueOf(status.toUpperCase());
        cdnNode.setStatus(newStatus);

        cdnNode.setLastHeartbeat(LocalDateTime.now());
        cdnNode.setLastHeartbeat(LocalDateTime.now());

        cdnNodeRepository.save(cdnNode);
        log.info("Updated CDN node status: {} (ID: {}) - {}", cdnNode.getName(), id, newStatus);
    }

    @Override
    @Transactional
    public void deleteCdnNode(Long id) {
        if (!cdnNodeRepository.existsById(id)) {
            throw new ServiceException("CDN node not found with id: " + id);
        }

        cdnNodeRepository.deleteById(id);
        log.info("Deleted CDN node with id: {}", id);
    }

    @Override
    @Transactional
    public void registerHeartbeat(String nodeId, Map<String, Object> metrics) {
        CdnNode cdnNode = cdnNodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + nodeId));

        // 更新心跳时间和指标
        cdnNode.setLastHeartbeat(LocalDateTime.now());

        cdnNode.setStatus(CdnNode.NodeStatus.ONLINE);
        cdnNode.setLastHeartbeat(LocalDateTime.now());

        // 更新当前负载
        if (metrics.containsKey("currentLoad")) {
            cdnNode.setCurrentLoad(((Number) metrics.get("currentLoad")).intValue());
        }

        cdnNodeRepository.save(cdnNode);

        // 分析网络指标

    }

    @Override
    public List<CdnNodeDTO> getBestCdnNodesForRegion(String region, int count) {
        // 获取指定区域内健康的CDN节点
        List<CdnNode> candidates = cdnNodeRepository.findByRegionAndStatusOrderByCurrentLoadAsc(region, CdnNode.NodeStatus.ONLINE);

        if (candidates.isEmpty()) {
            // 如果指定区域没有可用节点，获取全局健康节点
            log.warn("No healthy CDN nodes in region {}. Using global nodes.", region);
            candidates = cdnNodeRepository.findByStatusOrderByCurrentLoadAsc(CdnNode.NodeStatus.ONLINE);
        }

        // 返回指定数量的最佳节点（负载最低的）
        return candidates.stream()
                .limit(count)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCdnNodeStatistics(Long nodeId) {
        CdnNode cdnNode = cdnNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ServiceException("CDN node not found with id: " + nodeId));

        return new HashMap<String, Object>() {{
                put("id",   cdnNode.getId());
                put("name", cdnNode.getName());
                put("status", cdnNode.getStatus());
                put("location", cdnNode.getLocation());
                put("capacity", cdnNode.getCapacity());
                put("currentLoad", cdnNode.getCurrentLoad());
                put("loadPercentage", (cdnNode.getCurrentLoad() * 100.0) / cdnNode.getCapacity());
                put("lastHeartbeat", cdnNode.getLastHeartbeat());
        }};
    }

    /**
     * 生成唯一的CDN节点ID
     * Converts CdnNode entity to DTO
     * @param cdnNode The CdnNode entity
     * @return CdnNodeDTO object
     *
     * 
     */
    private String generateNodeId() {
        return "cdn-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 将实体转换为DTO
     */
    private CdnNodeDTO convertToDTO(CdnNode cdnNode) {
        return CdnNodeDTO.builder()
                .id(cdnNode.getId())
                .name(cdnNode.getName())
                .ipAddress(cdnNode.getIpAddress())
                .location(cdnNode.getLocation())
                .status(cdnNode.getStatus().toString())
                .capacity(cdnNode.getCapacity())
                .currentLoad(cdnNode.getCurrentLoad())
                .lastHeartbeat(cdnNode.getLastHeartbeat().toString())
                .build();
    }
}