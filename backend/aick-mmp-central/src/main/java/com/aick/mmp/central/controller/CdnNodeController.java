package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CdnNodeDTO;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.central.service.CdnNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cdn-nodes")
public class CdnNodeController {

    private final CdnNodeService cdnNodeService;

    @Autowired
    public CdnNodeController(CdnNodeService cdnNodeService) {
        this.cdnNodeService = cdnNodeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getAllCdnNodes(Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getAllCdnNodes(pageable));
    }

    @GetMapping("/region/{region}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByRegion(
            @PathVariable String region, 
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByRegion(region, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByStatus(
            @PathVariable CdnNode.NodeStatus status, 
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeDTO> getCdnNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CdnNodeDTO> createCdnNode(@RequestBody CdnNodeDTO cdnNodeDTO) {
        CdnNodeDTO created = cdnNodeService.createCdnNode(cdnNodeDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CdnNodeDTO> updateCdnNode(
            @PathVariable Long id, 
            @RequestBody CdnNodeDTO cdnNodeDTO) {
        return ResponseEntity.ok(cdnNodeService.updateCdnNode(id, cdnNodeDTO));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> updateCdnNodeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        String message = statusUpdate.get("message");
        cdnNodeService.updateCdnNodeStatus(id, status, message);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getBestCdnNodesForRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(cdnNodeService.getBestCdnNodesForRegion(region, count));
    }

    @GetMapping("/{nodeId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getCdnNodeStatistics(@PathVariable Long nodeId) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeStatistics(nodeId));
    }
}