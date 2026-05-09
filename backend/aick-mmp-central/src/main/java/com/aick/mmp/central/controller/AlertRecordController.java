package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.AlertRecordRequest;
import com.aick.mmp.central.dto.AlertStatistics;
import com.aick.mmp.central.service.AlertRecordService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警记录控制器
 */
@RestController
@RequestMapping("/alerts/records")
@RequiredArgsConstructor
@Slf4j
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<AlertRecord> getAlert(@PathVariable Long id) {
        return alertRecordService.getAlert(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> listAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "alertTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(alertRecordService.listAlerts(pageable));
    }

    @GetMapping("/by-rule/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> getAlertsByRule(
            @PathVariable Long ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "alertTime"));
        return ResponseEntity.ok(alertRecordService.findByRuleId(ruleId, pageable));
    }

    @GetMapping("/by-level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> getAlertsByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            AlertRecord.AlertStatus status = AlertRecord.AlertStatus.valueOf(level.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "alertTime"));
            return ResponseEntity.ok(alertRecordService.findByStatus(status, pageable));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/by-camera/{cameraId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> getAlertsByCamera(
            @PathVariable Long cameraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "alertTime"));
        return ResponseEntity.ok(alertRecordService.findByCameraId(cameraId, pageable));
    }

    @GetMapping("/by-time-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> getAlertsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(alertRecordService.findByTimeRange(startTime, endTime, pageable));
    }

    @GetMapping("/unresolved")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRecord>> getUnresolvedAlerts() {
        return ResponseEntity.ok(alertRecordService.getUnresolvedAlerts());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRecord>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(alertRecordService.getRecentAlerts(limit));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRecord>> getTodayAlerts() {
        return ResponseEntity.ok(alertRecordService.getTodayAlerts());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<AlertStatistics> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        AlertStatistics stats;
        if (startTime != null && endTime != null) {
            stats = alertRecordService.getStatistics(startTime, endTime);
        } else {
            stats = alertRecordService.getStatistics();
        }
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Long userId = extractUserId(user);
            String username = user.getUsername();
            alertRecordService.acknowledgeAlert(id, userId, username);
            return ResponseEntity.ok(Map.of("message", "Alert acknowledged successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to acknowledge alert {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to acknowledge alert: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long id,
            @RequestBody(required = false) AlertRecordRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Long userId = extractUserId(user);
            String username = user.getUsername();
            String resolutionNote = request != null ? request.getResolutionNote() : null;
            alertRecordService.resolveAlert(id, userId, username, resolutionNote);
            return ResponseEntity.ok(Map.of("message", "Alert resolved successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to resolve alert {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to resolve alert: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> batchResolveAlerts(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            
            List<Long> ids = (List<Long>) request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No alert IDs provided"));
            }
            
            Long userId = extractUserId(user);
            String username = user.getUsername();
            String resolutionNote = (String) request.get("resolutionNote");
            
            alertRecordService.batchResolveAlerts(ids, userId, username, resolutionNote);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alerts resolved successfully");
            response.put("count", ids.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to batch resolve alerts: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to batch resolve alerts: " + e.getMessage()));
        }
    }

    private Long extractUserId(UserDetails user) {
        return null;
    }
}
