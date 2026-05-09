package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.shared.model.EdgeNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface EdgeNodeService {
    Page<EdgeNodeDTO> getAllEdgeNodes(Pageable pageable);
    Page<EdgeNodeDTO> getEdgeNodesByLocation(String location, Pageable pageable);
    Page<EdgeNodeDTO> getEdgeNodesByStatus(EdgeNode.NodeStatus status, Pageable pageable);
    
    // Region-based queries
    Page<EdgeNodeDTO> getEdgeNodesByRegionId(Long regionId, boolean recursive, Pageable pageable);
    
    // 搜索边缘节点
    Page<EdgeNodeDTO> searchEdgeNodes(String keyword, EdgeNode.NodeStatus status, String location, Long regionId, boolean recursive, Pageable pageable);
    
    EdgeNodeDTO getEdgeNodeById(Long id);
    EdgeNodeDTO createEdgeNode(EdgeNodeDTO edgeNodeDTO);
    EdgeNodeDTO registerEdgeNode(EdgeNodeDTO edgeNodeDTO);
    EdgeNodeDTO updateEdgeNode(Long id, EdgeNodeDTO edgeNodeDTO);
    void updateEdgeNodeStatus(Long id, EdgeNodeStatusUpdateDTO statusUpdateDTO);
    void updateEdgeNodeCredentials(Long id, String username, String password);
    void deleteEdgeNode(Long id);
    
    // 批量操作
    void batchDeleteEdgeNodes(List<Long> nodeIds);
    void batchEnableEdgeNodes(List<Long> nodeIds, boolean enabled);
    void batchUpdateEdgeNodeStatus(List<Long> nodeIds, EdgeNode.NodeStatus status);
    
    void registerHeartbeat(Long nodeId, Map<String, Object> metrics);
    void registerHeartbeatByNodeId(String nodeId, Map<String, Object> metrics);
    Map<String, Object> getEdgeNodeStatistics(Long nodeId);
    List<EdgeNodeDTO> getOnlineEdgeNodes();
    long getEdgeNodeCountByStatus(EdgeNode.NodeStatus status);
    long getEdgeNodeCount(); // 添加获取所有边缘节点数量的方法
    boolean testEdgeNodeConnection(Long nodeId);
    void restartEdgeNodeService(Long nodeId);
    
    // 健康检查相关
    String checkNodeHealthStatus(Long nodeId);
    Map<String, Object> getNodeHealthDetails(Long nodeId);
    
    // 摄像头状态同步相关
    /**
     * 处理边缘节点上报的摄像头状态
     * @param nodeId 边缘节点名称或UUID
     * @param cameraStatuses 摄像头状态列表
     * @return 处理结果统计
     */
    Map<String, Object> processCameraStatusReports(String nodeId, List<Map<String, Object>> cameraStatuses);
}