package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.DashboardStatsDTO;
import com.aick.mmp.central.repository.AlertRecordRepository;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.CdnNodeRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.DashboardService;
import com.aick.mmp.central.service.EdgeNodeService;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final CameraService cameraService;
    private final EdgeNodeService edgeNodeService;
    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final RegionRepository regionRepository;
    private final CdnNodeRepository cdnNodeRepository;
    private final AlertRecordRepository alertRecordRepository;
    
    @Override
    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching dashboard statistics");
        
        // 获取摄像头统计信息
        long totalCameras = cameraService.getCameraCountByStatus(null);
        long onlineCameras = cameraService.getCameraCountByStatus(Camera.CameraStatus.ONLINE);
        
        // 获取边缘节点统计信息
        long totalEdgeNodes = edgeNodeService.getEdgeNodeCountByStatus(null);
        long onlineEdgeNodes = edgeNodeService.getEdgeNodeCountByStatus(EdgeNode.NodeStatus.ONLINE);
        
        // 流媒体统计信息
        long totalStreams = onlineCameras;
        long activeStreams = onlineCameras;
        
        // 在线用户数量
        long onlineUsers = 3L;
        
        // 获取区域统计
        List<DashboardStatsDTO.RegionStatsSummaryDTO> regionStats = getRegionStatsSummary();
        long totalRegions = regionRepository.findByIsDeletedFalseOrderBySortOrderAsc().size();
        
        // 获取CDN节点统计
        DashboardStatsDTO.CdnNodeStatsSummaryDTO cdnNodeStats = getCdnNodeStatsSummary();
        
        // 获取告警统计
        DashboardStatsDTO.AlertStatsSummaryDTO alertStats = getAlertStatsSummary();
        
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalCameras(totalCameras)
                .onlineCameras(onlineCameras)
                .totalEdgeNodes(totalEdgeNodes)
                .onlineEdgeNodes(onlineEdgeNodes)
                .totalStreams(totalStreams)
                .activeStreams(activeStreams)
                .onlineUsers(onlineUsers)
                .regionStats(regionStats)
                .totalRegions(totalRegions)
                .cdnNodeStats(cdnNodeStats)
                .alertStats(alertStats)
                .build();
        
        log.info("Dashboard statistics fetched successfully: {}", stats);
        return stats;
    }
    
    /**
     * 获取区域统计摘要
     */
    private List<DashboardStatsDTO.RegionStatsSummaryDTO> getRegionStatsSummary() {
        List<Region> regions = regionRepository.findByIsDeletedFalseOrderBySortOrderAsc();
        
        return regions.stream().map(region -> {
            // 获取该区域及其所有子区域
            List<Region> allRegionsInTree = getAllRegionsInTree(region);
            List<Long> regionIds = allRegionsInTree.stream()
                    .map(Region::getId)
                    .collect(Collectors.toList());
            
            // 统计摄像头
            List<Camera> cameras = regionIds.isEmpty() 
                    ? new ArrayList<>() 
                    : cameraRepository.findByRegionIdIn(regionIds);
            long totalCamerasInRegion = cameras.size();
            long onlineCamerasInRegion = cameras.stream()
                    .filter(c -> c.getStatus() == Camera.CameraStatus.ONLINE)
                    .count();
            
            // 统计边缘节点（根据 regionId 匹配区域树）
            List<EdgeNode> edges = edgeNodeRepository.findAll();
            long totalEdgesInRegion = edges.stream()
                    .filter(e -> matchesRegion(e, region, regionIds))
                    .count();
            long onlineEdgesInRegion = edges.stream()
                    .filter(e -> matchesRegion(e, region, regionIds) && e.getStatus() == EdgeNode.NodeStatus.ONLINE)
                    .count();
            
            return DashboardStatsDTO.RegionStatsSummaryDTO.builder()
                    .regionId(region.getId())
                    .regionName(region.getName())
                    .regionCode(region.getCode())
                    .totalCameras(totalCamerasInRegion)
                    .onlineCameras(onlineCamerasInRegion)
                    .totalEdges(totalEdgesInRegion)
                    .onlineEdges(onlineEdgesInRegion)
                    .build();
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取区域及其所有子区域
     */
    private List<Region> getAllRegionsInTree(Region rootRegion) {
        List<Region> result = new ArrayList<>();
        result.add(rootRegion);
        
        // 使用递归查找子区域
        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(rootRegion.getId());
        for (Region child : children) {
            result.addAll(getAllRegionsInTree(child));
        }
        
        return result;
    }
    
    /**
     * 检查边缘节点是否属于该区域
     * 优先使用 regionId 匹配，其次使用 regionIds 列表（包含子区域）
     */
    private boolean matchesRegion(EdgeNode edgeNode, Region region, List<Long> regionIds) {
        // 首先检查 regionId 是否在区域树中
        if (edgeNode.getRegionId() != null && regionIds.contains(edgeNode.getRegionId())) {
            return true;
        }
        
        // 备选方案：使用 location 字段匹配区域名称或代码（兼容旧数据）
        String location = edgeNode.getLocation();
        if (location != null) {
            String regionName = region.getName();
            String regionCode = region.getCode();
            return location.equals(regionName) || 
                   location.equals(regionCode) ||
                   location.contains(regionName) ||
                   location.contains(regionCode);
        }
        
        return false;
    }
    
    /**
     * 获取CDN节点统计摘要
     */
    private DashboardStatsDTO.CdnNodeStatsSummaryDTO getCdnNodeStatsSummary() {
        List<CdnNode> allNodes = cdnNodeRepository.findByIsDeletedFalse();
        List<CdnNode> onlineNodes = cdnNodeRepository.findByStatusAndIsDeletedFalseOrderByCurrentLoadAsc(CdnNode.NodeStatus.ONLINE);
        List<CdnNode> offlineNodes = cdnNodeRepository.findByStatusAndIsDeletedFalseOrderByCurrentLoadAsc(CdnNode.NodeStatus.OFFLINE);
        List<CdnNode> enabledNodes = cdnNodeRepository.findByIsDeletedFalseAndIsEnabledTrue();
        
        // 计算平均负载
        double avgLoad = 0;
        long highLoadNodes = 0;
        long lowLoadNodes = 0;
        if (!onlineNodes.isEmpty()) {
            double totalLoad = onlineNodes.stream()
                    .mapToDouble(n -> n.getCapacity() > 0 ? (double) n.getCurrentLoad() / n.getCapacity() * 100 : 0)
                    .sum();
            avgLoad = totalLoad / onlineNodes.size();
            
            highLoadNodes = onlineNodes.stream()
                    .filter(n -> n.getCapacity() > 0 && (double) n.getCurrentLoad() / n.getCapacity() * 100 > 80)
                    .count();
            
            lowLoadNodes = onlineNodes.stream()
                    .filter(n -> n.getCapacity() > 0 && (double) n.getCurrentLoad() / n.getCapacity() * 100 < 30)
                    .count();
        }
        
        return DashboardStatsDTO.CdnNodeStatsSummaryDTO.builder()
                .totalNodes(allNodes.size())
                .onlineNodes(onlineNodes.size())
                .offlineNodes(offlineNodes.size())
                .enabledNodes(enabledNodes.size())
                .avgLoad(Math.round(avgLoad * 100.0) / 100.0)
                .highLoadNodes(highLoadNodes)
                .lowLoadNodes(lowLoadNodes)
                .build();
    }
    
    /**
     * 获取告警统计摘要
     */
    private DashboardStatsDTO.AlertStatsSummaryDTO getAlertStatsSummary() {
        // 活跃告警（未处理和已确认）
        long unresolved = alertRecordRepository.countByStatus(AlertRecord.AlertStatus.UNRESOLVED);
        long acknowledged = alertRecordRepository.countByStatus(AlertRecord.AlertStatus.ACKNOWLEDGED);
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(LocalTime.MAX);
        
        List<AlertRecord> todayAlerts = alertRecordRepository.findByAlertTimeBetween(todayStart, todayEnd);
        long resolvedToday = todayAlerts.stream()
                .filter(a -> a.getStatus() == AlertRecord.AlertStatus.RESOLVED || 
                            a.getStatus() == AlertRecord.AlertStatus.AUTO_RESOLVED ||
                            a.getStatus() == AlertRecord.AlertStatus.IGNORED)
                .count();
        long triggeredToday = todayAlerts.size();
        
        // 按级别统计
        long critical = todayAlerts.stream()
                .filter(a -> a.getLevel() == com.aick.mmp.shared.model.AlertRule.AlertLevel.CRITICAL)
                .count();
        long warning = todayAlerts.stream()
                .filter(a -> a.getLevel() == com.aick.mmp.shared.model.AlertRule.AlertLevel.WARNING)
                .count();
        long error = todayAlerts.stream()
                .filter(a -> a.getLevel() == com.aick.mmp.shared.model.AlertRule.AlertLevel.ERROR)
                .count();
        long info = todayAlerts.stream()
                .filter(a -> a.getLevel() == com.aick.mmp.shared.model.AlertRule.AlertLevel.INFO)
                .count();
        
        return DashboardStatsDTO.AlertStatsSummaryDTO.builder()
                .totalActive(unresolved + acknowledged)
                .pending(unresolved)
                .acknowledged(acknowledged)
                .critical(critical)
                .warning(warning)
                .error(error)
                .info(info)
                .resolvedToday(resolvedToday)
                .triggeredToday(triggeredToday)
                .build();
    }
}