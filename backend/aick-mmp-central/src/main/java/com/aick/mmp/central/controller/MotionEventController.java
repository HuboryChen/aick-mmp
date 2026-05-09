package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.MotionEventDTO;
import com.aick.mmp.central.entity.MotionEvent;
import com.aick.mmp.central.service.MotionEventService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动侦测事件控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/motion-events")
@RequiredArgsConstructor
public class MotionEventController {

    private final MotionEventService motionEventService;

    /**
     * 上报移动侦测事件（供边缘节点调用）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<MotionEvent> reportMotionEvent(@RequestBody MotionEventDTO eventDTO) {
        MotionEvent event = convertToEntity(eventDTO);
        MotionEvent created = motionEventService.createMotionEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 批量上报移动侦测事件
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<List<MotionEvent>> reportMotionEventsBatch(@RequestBody List<MotionEventDTO> events) {
        List<MotionEvent> entities = events.stream()
                .map(this::convertToEntity)
                .toList();
        List<MotionEvent> created = motionEventService.createMotionEventsBatch(entities);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 获取移动事件详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<MotionEvent> getMotionEvent(@PathVariable Long id) {
        return motionEventService.getMotionEvent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 查询指定摄像头的移动事件（分页）
     */
    @GetMapping("/camera/{cameraId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<MotionEvent>> getMotionEventsByCamera(
            @PathVariable Long cameraId,
            Pageable pageable) {
        Page<MotionEvent> events = motionEventService.getMotionEventsByCamera(cameraId, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * 查询指定时间范围内的移动事件
     */
    @GetMapping("/camera/{cameraId}/time-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<MotionEvent>> getMotionEventsByTimeRange(
            @PathVariable Long cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<MotionEvent> events = motionEventService.getMotionEventsByTimeRange(cameraId, startTime, endTime);
        return ResponseEntity.ok(events);
    }

    /**
     * 分页查询指定时间范围内的移动事件
     */
    @GetMapping("/camera/{cameraId}/time-range/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<MotionEvent>> getMotionEventsByTimeRangePaged(
            @PathVariable Long cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Pageable pageable) {
        Page<MotionEvent> events = motionEventService.getMotionEventsByTimeRangePaged(
                cameraId, startTime, endTime, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取触发了录像的移动事件
     */
    @GetMapping("/triggered")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<MotionEvent>> getTriggeredRecordingEvents(Pageable pageable) {
        Page<MotionEvent> events = motionEventService.getTriggeredRecordingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * 统计指定摄像头的移动事件数量
     */
    @GetMapping("/camera/{cameraId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> countByCamera(@PathVariable Long cameraId) {
        long count = motionEventService.countByCamera(cameraId);
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", cameraId);
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    /**
     * 统计指定时间范围内的移动事件数量
     */
    @GetMapping("/camera/{cameraId}/count/time-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> countByCameraAndTimeRange(
            @PathVariable Long cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        long count = motionEventService.countByCameraAndTimeRange(cameraId, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", cameraId);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    /**
     * 清理旧事件（仅管理员）
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldEvents(
            @RequestParam(defaultValue = "30") int daysOld) {
        int cleanedCount = motionEventService.cleanupOldEvents(daysOld);
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("daysOld", daysOld);
        return ResponseEntity.ok(result);
    }

    /**
     * 关联录像到移动事件
     */
    @PostMapping("/{eventId}/link-recording")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<MotionEvent> linkRecording(
            @PathVariable Long eventId,
            @RequestParam Long recordingId) {
        MotionEvent updated = motionEventService.linkRecording(eventId, recordingId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 将DTO转换为实体
     */
    private MotionEvent convertToEntity(MotionEventDTO dto) {
        return MotionEvent.builder()
                .cameraId(dto.getCameraId())
                .eventTime(dto.getEventTime())
                .durationSeconds(dto.getDurationSeconds())
                .detectionArea(dto.getDetectionArea())
                .intensity(dto.getIntensity())
                .eventType(dto.getEventType())
                .triggeredRecording(dto.getTriggeredRecording())
                .recordingId(dto.getRecordingId())
                .metadata(dto.getMetadata())
                .edgeNodeId(dto.getEdgeNodeId())
                .build();
    }
}
