package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.service.EdgeNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/edge-nodes")
public class EdgeNodeController {

    private final EdgeNodeService edgeNodeService;

    @Autowired
    public EdgeNodeController(EdgeNodeService edgeNodeService) {
        this.edgeNodeService = edgeNodeService;
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
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}