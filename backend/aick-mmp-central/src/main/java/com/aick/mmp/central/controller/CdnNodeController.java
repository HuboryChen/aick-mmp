package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CdnNodeDTO;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.central.service.CdnNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cdn-nodes")
public class CdnNodeController {

    private final CdnNodeService cdnNodeService;

    @Autowired
    public CdnNodeController(CdnNodeService cdnNodeService) {
        this.cdnNodeService = cdnNodeService;
    }

    @GetMapping
    public ResponseEntity<Page<CdnNodeDTO>> getAllCdnNodes(Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getAllCdnNodes(pageable));
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByRegion(
            @PathVariable String region, 
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByRegion(region, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByStatus(
            @PathVariable CdnNode.NodeStatus status, 
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CdnNodeDTO> getCdnNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeById(id));
    }

    @PostMapping
    public ResponseEntity<CdnNodeDTO> createCdnNode(@RequestBody CdnNodeDTO cdnNodeDTO) {
        CdnNodeDTO created = cdnNodeService.createCdnNode(cdnNodeDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CdnNodeDTO> updateCdnNode(
            @PathVariable Long id, 
            @RequestBody CdnNodeDTO cdnNodeDTO) {
        return ResponseEntity.ok(cdnNodeService.updateCdnNode(id, cdnNodeDTO));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateCdnNodeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        String message = statusUpdate.get("message");
        cdnNodeService.updateCdnNodeStatus(id, status, message);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCdnNode(@PathVariable Long id) {
        cdnNodeService.deleteCdnNode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/heartbeat/{nodeId}")
    public ResponseEntity<Void> registerHeartbeat(
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> metrics) {
        cdnNodeService.registerHeartbeat(nodeId, metrics);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/best/{region}")
    public ResponseEntity<List<CdnNodeDTO>> getBestCdnNodesForRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(cdnNodeService.getBestCdnNodesForRegion(region, count));
    }

    @GetMapping("/{nodeId}/statistics")
    public ResponseEntity<Map<String, Object>> getCdnNodeStatistics(@PathVariable Long nodeId) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeStatistics(nodeId));
    }
}