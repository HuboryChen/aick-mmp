package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.service.CdnNodeService;
import com.aick.mmp.shared.model.CdnNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CDN节点管理REST控制器
 */
@RestController
@RequestMapping("/v1/cdn-nodes")
@RequiredArgsConstructor
@Slf4j
public class CdnNodeController {

    private final CdnNodeService cdnNodeService;

    // ==================== 基础CRUD接口 ====================

    /**
     * 获取所有CDN节点（分页）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getAllCdnNodes(Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getAllCdnNodes(pageable));
    }

    /**
     * 获取所有在线CDN节点
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getActiveCdnNodes() {
        return ResponseEntity.ok(cdnNodeService.getAllActiveCdnNodes());
    }

    /**
     * 根据ID获取CDN节点详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeDTO> getCdnNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeById(id));
    }

    /**
     * 根据节点标识符获取CDN节点
     */
    @GetMapping("/node/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeDTO> getCdnNodeByNodeId(@PathVariable String nodeId) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeByNodeId(nodeId));
    }

    /**
     * 根据区域获取CDN节点
     */
    @GetMapping("/region/{region}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByRegion(
            @PathVariable String region,
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByRegion(region, pageable));
    }

    /**
     * 根据区域ID获取CDN节点（支持递归查询）
     */
    @GetMapping("/region-id/{regionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByRegionId(
            @PathVariable Long regionId,
            @RequestParam(required = false, defaultValue = "false") boolean recursive,
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByRegionId(regionId, recursive, pageable));
    }

    /**
     * 根据状态获取CDN节点
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CdnNodeDTO>> getCdnNodesByStatus(
            @PathVariable CdnNode.NodeStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodesByStatus(status, pageable));
    }

    // ==================== 节点管理接口 ====================

    /**
     * 创建CDN节点
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CdnNodeDTO> createCdnNode(@RequestBody CdnNodeDTO cdnNodeDTO) {
        CdnNodeDTO created = cdnNodeService.createCdnNode(cdnNodeDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * 更新CDN节点
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CdnNodeDTO> updateCdnNode(
            @PathVariable Long id,
            @RequestBody CdnNodeDTO cdnNodeDTO) {
        return ResponseEntity.ok(cdnNodeService.updateCdnNode(id, cdnNodeDTO));
    }

    /**
     * 更新CDN节点状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> updateCdnNodeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        String message = statusUpdate.getOrDefault("message", "");
        cdnNodeService.updateCdnNodeStatus(id, status, message);
        return ResponseEntity.ok().build();
    }

    /**
     * 启用CDN节点
     */
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> enableCdnNode(@PathVariable Long id) {
        cdnNodeService.enableCdnNode(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 禁用CDN节点
     */
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableCdnNode(@PathVariable Long id) {
        cdnNodeService.disableCdnNode(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除CDN节点（软删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCdnNode(@PathVariable Long id) {
        cdnNodeService.deleteCdnNode(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 恢复已删除的CDN节点
     */
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CdnNodeDTO> restoreCdnNode(@PathVariable Long id) {
        return ResponseEntity.ok(cdnNodeService.restoreCdnNode(id));
    }

    // ==================== 负载均衡接口 ====================

    /**
     * 获取最佳CDN节点（基础）
     */
    @GetMapping("/best/{region}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getBestCdnNodesForRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(cdnNodeService.getBestCdnNodesForRegion(region, count));
    }

    /**
     * 使用WLC算法获取最佳CDN节点
     */
    @GetMapping("/best/wlc")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getBestCdnNodesByWlc(
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(cdnNodeService.getBestCdnNodesByWlc(count));
    }

    /**
     * 使用地理邻近性+WLC算法获取最佳CDN节点
     */
    @GetMapping("/best/geo-wlc")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getBestCdnNodesByGeoAndWlc(
            @RequestParam String regionCode,
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(cdnNodeService.getBestCdnNodesByGeoAndWlc(regionCode, count));
    }

    /**
     * 选择单个最优CDN节点
     */
    @GetMapping("/optimal")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeDTO> selectOptimalNode() {
        return ResponseEntity.ok(cdnNodeService.selectOptimalNode());
    }

    /**
     * 获取健康节点列表
     */
    @GetMapping("/healthy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeDTO>> getHealthyNodes() {
        return ResponseEntity.ok(cdnNodeService.getHealthyNodes());
    }

    // ==================== 负载上报接口 ====================

    /**
     * 心跳上报
     */
    @PostMapping("/heartbeat/{nodeId}")
    public ResponseEntity<Void> registerHeartbeat(
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> metrics) {
        cdnNodeService.registerHeartbeat(nodeId, metrics);
        return ResponseEntity.ok().build();
    }

    /**
     * 负载上报
     */
    @PostMapping("/{nodeId}/report")
    public ResponseEntity<Void> reportLoad(
            @PathVariable String nodeId,
            @RequestBody CdnNodeReportDTO report) {
        report.setNodeId(nodeId);
        cdnNodeService.reportLoad(report);
        return ResponseEntity.ok().build();
    }

    // ==================== 统计接口 ====================

    /**
     * 获取节点统计信息
     */
    @GetMapping("/{nodeId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeStatsDTO> getCdnNodeStatistics(@PathVariable Long nodeId) {
        return ResponseEntity.ok(cdnNodeService.getCdnNodeStatistics(nodeId));
    }

    /**
     * 获取节点负载历史
     */
    @GetMapping("/{nodeId}/load")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<CdnNodeLoadDTO>> getLoadHistory(
            @PathVariable Long nodeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        // 默认查询最近24小时
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        return ResponseEntity.ok(cdnNodeService.getLoadHistory(nodeId, startTime, endTime));
    }

    /**
     * 获取节点最新负载
     */
    @GetMapping("/{nodeId}/load/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CdnNodeLoadDTO> getLatestLoad(@PathVariable Long nodeId) {
        CdnNodeLoadDTO load = cdnNodeService.getLatestLoad(nodeId);
        if (load == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(load);
    }

    /**
     * 获取全局CDN统计
     */
    @GetMapping("/stats/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getGlobalCdnStats() {
        return ResponseEntity.ok(cdnNodeService.getGlobalCdnStats());
    }

    /**
     * 获取区域CDN统计
     */
    @GetMapping("/stats/region/{region}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getRegionCdnStats(@PathVariable String region) {
        return ResponseEntity.ok(cdnNodeService.getRegionCdnStats(region));
    }

    // ==================== 健康检查接口 ====================

    /**
     * 测试节点连通性
     */
    @GetMapping("/{nodeId}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CdnNodeConnectivityTestDTO> testConnectivity(@PathVariable Long nodeId) {
        return ResponseEntity.ok(cdnNodeService.testConnectivity(nodeId));
    }

    /**
     * 批量健康检查
     */
    @PostMapping("/health-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<Long, CdnNodeConnectivityTestDTO>> batchHealthCheck() {
        return ResponseEntity.ok(cdnNodeService.batchHealthCheck());
    }
}
