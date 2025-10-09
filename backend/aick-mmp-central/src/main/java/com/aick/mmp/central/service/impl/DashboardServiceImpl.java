package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.DashboardStatsDTO;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.DashboardService;
import com.aick.mmp.central.service.EdgeNodeService;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final CameraService cameraService;
    private final EdgeNodeService edgeNodeService;
    
    @Override
    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching dashboard statistics");
        
        // 获取摄像头统计信息
        long totalCameras = cameraService.getCameraCountByStatus(null); // 获取所有摄像头数量
        long onlineCameras = cameraService.getCameraCountByStatus(Camera.CameraStatus.ONLINE);
        
        // 获取边缘节点统计信息
        long totalEdgeNodes = edgeNodeService.getEdgeNodeCountByStatus(null); // 获取所有节点数量
        long onlineEdgeNodes = edgeNodeService.getEdgeNodeCountByStatus(EdgeNode.NodeStatus.ONLINE);
        
        // 流媒体统计信息（使用在线摄像头数量作为活跃流数量）
        long totalStreams = onlineCameras;
        long activeStreams = onlineCameras;
        
        // 在线用户数量（暂时设为固定值，实际应从会话管理中获取）
        long onlineUsers = 3L;
        
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalCameras(totalCameras)
                .onlineCameras(onlineCameras)
                .totalEdgeNodes(totalEdgeNodes)
                .onlineEdgeNodes(onlineEdgeNodes)
                .totalStreams(totalStreams)
                .activeStreams(activeStreams)
                .onlineUsers(onlineUsers)
                .build();
        
        log.info("Dashboard statistics fetched successfully: {}", stats);
        return stats;
    }
}