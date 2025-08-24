package com.aick.mmp.service;

import com.aick.mmp.dto.EdgeNodeDTO;
import com.aick.mmp.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.model.EdgeNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface EdgeNodeService {
    Page<EdgeNodeDTO> getAllEdgeNodes(Pageable pageable);
    Page<EdgeNodeDTO> getEdgeNodesByLocation(String location, Pageable pageable);
    Page<EdgeNodeDTO> getEdgeNodesByStatus(EdgeNode.NodeStatus status, Pageable pageable);
    EdgeNodeDTO getEdgeNodeById(Long id);
    EdgeNodeDTO createEdgeNode(EdgeNodeDTO edgeNodeDTO);
    EdgeNodeDTO updateEdgeNode(Long id, EdgeNodeDTO edgeNodeDTO);
    void updateEdgeNodeStatus(Long id, EdgeNodeStatusUpdateDTO statusUpdateDTO);
    void updateEdgeNodeCredentials(Long id, String username, String password);
    void deleteEdgeNode(Long id);
    void registerHeartbeat(Long nodeId, Map<String, Object> metrics);
    Map<String, Object> getEdgeNodeStatistics(Long nodeId);
    List<EdgeNodeDTO> getOnlineEdgeNodes();
    long getEdgeNodeCountByStatus(EdgeNode.NodeStatus status);
    boolean testEdgeNodeConnection(Long nodeId);
    void restartEdgeNodeService(Long nodeId);
}