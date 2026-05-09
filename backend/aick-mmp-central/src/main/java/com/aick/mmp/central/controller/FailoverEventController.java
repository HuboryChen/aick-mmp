package com.aick.mmp.central.controller;

import com.aick.mmp.central.repository.CameraFailoverEventRepository;
import com.aick.mmp.shared.model.CameraFailoverEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 故障转移事件查询控制器
 * 提供故障转移历史的查看和筛选功能
 */
@RestController
@RequestMapping("/failover-events")
@RequiredArgsConstructor
public class FailoverEventController {

    private final CameraFailoverEventRepository failoverEventRepository;

    /**
     * 查询故障转移事件列表（支持筛选和分页）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CameraFailoverEvent>> getFailoverEvents(
            @RequestParam(required = false) Long sourceEdgeNodeId,
            @RequestParam(required = false) CameraFailoverEvent.FailoverTriggerType triggerType,
            @RequestParam(required = false) CameraFailoverEvent.FailoverStatus status,
            Pageable pageable) {
        Page<CameraFailoverEvent> events = failoverEventRepository.findByConditions(
                sourceEdgeNodeId, triggerType, status, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取单个故障转移事件详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CameraFailoverEvent> getFailoverEvent(@PathVariable Long id) {
        return failoverEventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
