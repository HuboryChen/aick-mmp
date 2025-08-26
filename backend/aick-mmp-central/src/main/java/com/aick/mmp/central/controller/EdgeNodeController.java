package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.service.EdgeNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/edge-nodes")
public class EdgeNodeController {

    private final EdgeNodeService edgeNodeService;

    @Autowired
    public EdgeNodeController(EdgeNodeService edgeNodeService) {
        this.edgeNodeService = edgeNodeService;
    }

    @GetMapping
    public ResponseEntity<Page<EdgeNodeDTO>> getAllEdgeNodes(Pageable pageable) {
        return ResponseEntity.ok(edgeNodeService.getAllEdgeNodes(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EdgeNodeDTO> getEdgeNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(edgeNodeService.getEdgeNodeById(id));
    }

    @PostMapping
    public ResponseEntity<EdgeNodeDTO> createEdgeNode(@RequestBody EdgeNodeDTO edgeNodeDTO) {
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(edgeNodeDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EdgeNodeDTO> updateEdgeNode(@PathVariable Long id, @RequestBody EdgeNodeDTO edgeNodeDTO) {
        return ResponseEntity.ok(edgeNodeService.updateEdgeNode(id, edgeNodeDTO));
    }

    @DeleteMapping("/{id}")
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
    public ResponseEntity<Void> updateEdgeNodeStatus(@PathVariable Long id, @RequestBody EdgeNodeStatusUpdateDTO statusUpdateDTO) {
        edgeNodeService.updateEdgeNodeStatus(id, statusUpdateDTO);
        return ResponseEntity.ok().build();
    }
}