package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.AlertRuleRequest;
import com.aick.mmp.central.service.AlertRuleService;
import com.aick.mmp.central.service.AlertRecordService;
import com.aick.mmp.central.service.AlertRuleTemplateService;
import com.aick.mmp.central.service.EscalationService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertEscalation;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AlertRuleTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警规则管理控制器
 */
@RestController
@RequestMapping("/v1/alert-rules")
@RequiredArgsConstructor
@Slf4j
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final AlertRecordService alertRecordService;
    private final AlertRuleTemplateService alertRuleTemplateService;
    private final EscalationService escalationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createRule(
            @Valid @RequestBody AlertRuleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            Long userId = extractUserId(user);
            AlertRule rule = alertRuleService.createRule(request, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", rule.getId());
            response.put("name", rule.getName());
            response.put("message", "Alert rule created successfully");
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create alert rule: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create alert rule: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleRequest request) {
        try {
            AlertRule rule = alertRuleService.updateRule(id, request);
            return ResponseEntity.ok(Map.of(
                    "id", rule.getId(),
                    "name", rule.getName(),
                    "message", "Alert rule updated successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update alert rule: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        try {
            alertRuleService.deleteRule(id);
            return ResponseEntity.ok(Map.of("message", "Alert rule deleted successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete alert rule: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<AlertRule> getRule(@PathVariable Long id) {
        return alertRuleService.getRule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRule>> listRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(alertRuleService.listRules(pageable));
    }

    @GetMapping("/by-type/{alertType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRule>> getRulesByType(@PathVariable String alertType) {
        try {
            AlertRule.AlertType type = AlertRule.AlertType.valueOf(alertType.toUpperCase());
            return ResponseEntity.ok(alertRuleService.findByAlertType(type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enableRule(@PathVariable Long id) {
        try {
            alertRuleService.enableRule(id);
            return ResponseEntity.ok(Map.of("message", "Alert rule enabled successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to enable alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to enable alert rule: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disableRule(@PathVariable Long id) {
        try {
            alertRuleService.disableRule(id);
            return ResponseEntity.ok(Map.of("message", "Alert rule disabled successfully"));
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to disable alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to disable alert rule: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testRule(@PathVariable Long id) {
        try {
            boolean success = alertRuleService.testRule(id);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Test passed" : "Test failed"));
        } catch (Exception e) {
            log.error("Failed to test alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test failed: " + e.getMessage()));
        }
    }

    // ==================== 规则模板 API ====================

    /**
     * 获取所有启用的模板列表
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRuleTemplate>> listTemplates() {
        return ResponseEntity.ok(alertRuleTemplateService.getEnabledTemplates());
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/templates/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<AlertRuleTemplate> getTemplate(@PathVariable Long templateId) {
        AlertRuleTemplate template = alertRuleTemplateService.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 根据告警类型获取模板
     */
    @GetMapping("/templates/by-type/{alertType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRuleTemplate>> getTemplatesByAlertType(@PathVariable String alertType) {
        try {
            AlertRule.AlertType type = AlertRule.AlertType.valueOf(alertType.toUpperCase());
            return ResponseEntity.ok(alertRuleTemplateService.getTemplatesByAlertType(type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取推荐模板
     */
    @GetMapping("/templates/recommended")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRuleTemplate>> getRecommendedTemplates() {
        return ResponseEntity.ok(alertRuleTemplateService.getRecommendedTemplates());
    }

    /**
     * 搜索模板
     */
    @GetMapping("/templates/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertRuleTemplate>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(alertRuleTemplateService.searchTemplates(keyword));
    }

    // ==================== 规则历史 API ====================

    /**
     * 获取规则的历史告警记录
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<AlertRecord>> getRuleHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "alertTime");
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(alertRecordService.findByRuleId(id, pageable));
    }

    /**
     * 获取规则的统计信息
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getRuleStats(
            @PathVariable Long id,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            java.time.LocalDateTime start = startTime != null ? 
                    java.time.LocalDateTime.parse(startTime) : java.time.LocalDateTime.now().minusDays(7);
            java.time.LocalDateTime end = endTime != null ? 
                    java.time.LocalDateTime.parse(endTime) : java.time.LocalDateTime.now();

            Map<String, Object> stats = new HashMap<>();
            stats.put("triggerCount", alertRuleService.countTriggers(id, start));
            stats.put("lastTriggeredTime", alertRuleService.getLastTriggeredTime(id));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get rule stats {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    /**
     * 获取告警的升级历史
     */
    @GetMapping("/alerts/{alertId}/escalations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AlertEscalation>> getAlertEscalations(@PathVariable Long alertId) {
        return ResponseEntity.ok(escalationService.getEscalationHistory(alertId));
    }

    /**
     * 手动触发告警升级
     */
    @PostMapping("/alerts/{alertId}/escalate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerEscalation(@PathVariable Long alertId) {
        try {
            escalationService.triggerAllEscalations(alertId);
            return ResponseEntity.ok(Map.of("message", "Escalation triggered successfully"));
        } catch (Exception e) {
            log.error("Failed to trigger escalation for alert {}: {}", alertId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to trigger escalation: " + e.getMessage()));
        }
    }

    private Long extractUserId(UserDetails user) {
        // 从UserDetails中提取用户ID
        // 这里需要根据实际的用户信息结构来实现
        return null;
    }
}
