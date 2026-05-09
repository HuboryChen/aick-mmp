package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.service.EdgeNodeFailoverService;
import com.aick.mmp.central.service.EdgeNodeService;
import com.aick.mmp.shared.model.CameraFailoverEvent;
import com.aick.mmp.shared.model.EdgeNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/edge-nodes")
public class EdgeNodeController {

    private final EdgeNodeService edgeNodeService;
    private final EdgeNodeFailoverService edgeNodeFailoverService;

    @Autowired
    public EdgeNodeController(EdgeNodeService edgeNodeService, EdgeNodeFailoverService edgeNodeFailoverService) {
        this.edgeNodeService = edgeNodeService;
        this.edgeNodeFailoverService = edgeNodeFailoverService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<EdgeNodeDTO>> getAllEdgeNodes(Pageable pageable) {
        return ResponseEntity.ok(edgeNodeService.getAllEdgeNodes(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<EdgeNodeDTO> getEdgeNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(edgeNodeService.getEdgeNodeById(id));
    }

    /**
     * 根据区域获取边缘节点
     */
    @GetMapping("/region/{regionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<EdgeNodeDTO>> getEdgeNodesByRegion(
            @PathVariable Long regionId,
            @RequestParam(required = false, defaultValue = "false") boolean recursive,
            Pageable pageable) {
        return ResponseEntity.ok(edgeNodeService.getEdgeNodesByRegionId(regionId, recursive, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EdgeNodeDTO> createEdgeNode(@RequestBody EdgeNodeDTO edgeNodeDTO) {
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(edgeNodeDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/register")
    public ResponseEntity<EdgeNodeDTO> registerEdgeNode(@RequestBody EdgeNodeDTO edgeNodeDTO) {
        EdgeNodeDTO registered = edgeNodeService.registerEdgeNode(edgeNodeDTO);
        return new ResponseEntity<>(registered, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EdgeNodeDTO> updateEdgeNode(@PathVariable Long id, @RequestBody EdgeNodeDTO edgeNodeDTO) {
        return ResponseEntity.ok(edgeNodeService.updateEdgeNode(id, edgeNodeDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEdgeNode(@PathVariable Long id) {
        edgeNodeService.deleteEdgeNode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<Void> registerHeartbeat(
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> metrics) {
        try {
            edgeNodeService.registerHeartbeatByNodeId(nodeId, metrics);
            
            // 处理摄像头状态上报
            if (metrics.containsKey("cameraStatuses")) {
                Object cameraStatusesObj = metrics.get("cameraStatuses");
                if (cameraStatusesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cameraStatuses = (List<Map<String, Object>>) cameraStatusesObj;
                    edgeNodeService.processCameraStatusReports(nodeId, cameraStatuses);
                }
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 单独处理摄像头状态上报接口
     */
    @PostMapping("/{nodeId}/camera-statuses")
    public ResponseEntity<Map<String, Object>> reportCameraStatuses(
            @PathVariable String nodeId,
            @RequestBody List<Map<String, Object>> cameraStatuses) {
        try {
            Map<String, Object> result = edgeNodeService.processCameraStatusReports(nodeId, cameraStatuses);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> updateEdgeNodeStatus(@PathVariable Long id, @RequestBody EdgeNodeStatusUpdateDTO statusUpdateDTO) {
        edgeNodeService.updateEdgeNodeStatus(id, statusUpdateDTO);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取边缘节点详细信息和统计信息
     */
    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getEdgeNodeDetails(@PathVariable Long id) {
        Map<String, Object> details = edgeNodeService.getEdgeNodeStatistics(id);
        return ResponseEntity.ok(details);
    }
    
    /**
     * 测试边缘节点连接
     */
    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> testEdgeNodeConnection(@PathVariable Long id) {
        boolean isConnected = edgeNodeService.testEdgeNodeConnection(id);
        Map<String, Object> response = new HashMap<>();
        response.put("connected", isConnected);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重启边缘节点服务
     */
    @PostMapping("/{id}/restart")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> restartEdgeNodeService(@PathVariable Long id) {
        edgeNodeService.restartEdgeNodeService(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Restart command sent successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新边缘节点认证信息
     */
    @PutMapping("/{id}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateEdgeNodeCredentials(
            @PathVariable Long id,
            @RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        edgeNodeService.updateEdgeNodeCredentials(id, username, password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Credentials updated successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量删除边缘节点
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchDeleteEdgeNodes(@RequestBody List<Long> nodeIds) {
        edgeNodeService.batchDeleteEdgeNodes(nodeIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量删除成功");
        response.put("deletedCount", nodeIds.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量启用/禁用边缘节点
     */
    @PostMapping("/batch-enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchEnableEdgeNodes(
            @RequestBody List<Long> nodeIds,
            @RequestParam boolean enabled) {
        edgeNodeService.batchEnableEdgeNodes(nodeIds, enabled);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量" + (enabled ? "启用" : "禁用") + "成功");
        response.put("affectedCount", nodeIds.size());
        response.put("enabled", enabled);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量更新边缘节点状态
     */
    @PostMapping("/batch-update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchUpdateEdgeNodeStatus(
            @RequestBody List<Long> nodeIds,
            @RequestParam EdgeNode.NodeStatus status) {
        edgeNodeService.batchUpdateEdgeNodeStatus(nodeIds, status);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量更新状态成功");
        response.put("affectedCount", nodeIds.size());
        response.put("status", status);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 搜索边缘节点
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<EdgeNodeDTO>> searchEdgeNodes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EdgeNode.NodeStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false, defaultValue = "false") boolean recursive,
            Pageable pageable) {
        Page<EdgeNodeDTO> nodes = edgeNodeService.searchEdgeNodes(keyword, status, location, regionId, recursive, pageable);
        return ResponseEntity.ok(nodes);
    }
    
    /**
     * 检查节点健康状态
     */
    @GetMapping("/{id}/health-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getNodeHealthStatus(@PathVariable Long id) {
        String healthStatus = edgeNodeService.checkNodeHealthStatus(id);
        Map<String, Object> response = new HashMap<>();
        response.put("nodeId", id);
        response.put("healthStatus", healthStatus);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取节点健康详情
     */
    @GetMapping("/{id}/health-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getNodeHealthDetails(@PathVariable Long id) {
        Map<String, Object> healthDetails = edgeNodeService.getNodeHealthDetails(id);
        return ResponseEntity.ok(healthDetails);
    }

    /**
     * 手动触发指定节点的故障转移
     */
    @PostMapping("/{id}/trigger-failover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerFailover(@PathVariable Long id) {
        Long eventId = edgeNodeFailoverService.triggerFailover(id, CameraFailoverEvent.FailoverTriggerType.MANUAL);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "故障转移已触发");
        response.put("nodeId", id);
        response.put("eventId", eventId);
        return ResponseEntity.accepted().body(response); // 202 Accepted for async processing
    }
}