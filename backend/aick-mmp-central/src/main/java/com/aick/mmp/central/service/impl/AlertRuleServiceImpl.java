package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.AlertRuleDTO;
import com.aick.mmp.central.dto.AlertRuleRequest;
import com.aick.mmp.central.repository.AlertConditionRepository;
import com.aick.mmp.central.repository.AlertEscalationRepository;
import com.aick.mmp.central.repository.AlertNotificationRepository;
import com.aick.mmp.central.repository.AlertRecordRepository;
import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.service.AlertRuleService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertCondition;
import com.aick.mmp.shared.model.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 告警规则服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertConditionRepository alertConditionRepository;
    private final AlertNotificationRepository alertNotificationRepository;
    private final AlertEscalationRepository alertEscalationRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final ModelMapper modelMapper;

    // ==================== 规则 CRUD ====================

    @Override
    public AlertRule createRule(AlertRuleRequest request, Long createdBy) {
        // 检查名称唯一性
        if (alertRuleRepository.existsByName(request.getName())) {
            throw new ServiceException("Rule name already exists: " + request.getName());
        }

        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .alertType(request.getAlertType())
                .level(request.getLevel())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .thresholdExpression(request.getThresholdExpression())
                .warningThreshold(request.getWarningThreshold())
                .criticalThreshold(request.getCriticalThreshold())
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 300)
                .cooldownSeconds(request.getCooldownSeconds() != null ? request.getCooldownSeconds() : 600)
                .alertSchedule(request.getAlertSchedule())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .status(AlertRule.RuleStatus.ENABLED)
                .createdBy(createdBy)
                .notificationMethod(request.getNotificationMethod() != null ? 
                        request.getNotificationMethod() : AlertRule.NotificationMethod.IN_APP)
                .notificationTarget(request.getNotificationTarget())
                .build();

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: {} (ID: {})", saved.getName(), saved.getId());
        return saved;
    }

    @Override
    public AlertRule updateRule(Long id, AlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + id));

        // 检查名称唯一性（排除自身）
        if (!rule.getName().equals(request.getName()) && 
                alertRuleRepository.existsByName(request.getName())) {
            throw new ServiceException("Rule name already exists: " + request.getName());
        }

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setAlertType(request.getAlertType());
        rule.setLevel(request.getLevel());
        rule.setTargetType(request.getTargetType());
        rule.setTargetId(request.getTargetId());
        rule.setThresholdExpression(request.getThresholdExpression());
        rule.setWarningThreshold(request.getWarningThreshold());
        rule.setCriticalThreshold(request.getCriticalThreshold());
        rule.setDurationSeconds(request.getDurationSeconds());
        rule.setCooldownSeconds(request.getCooldownSeconds());
        rule.setAlertSchedule(request.getAlertSchedule());
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
            rule.setStatus(request.getEnabled() ? AlertRule.RuleStatus.ENABLED : AlertRule.RuleStatus.DISABLED);
        }
        rule.setNotificationMethod(request.getNotificationMethod());
        rule.setNotificationTarget(request.getNotificationTarget());

        AlertRule updated = alertRuleRepository.save(rule);
        log.info("Updated alert rule: {} (ID: {})", updated.getName(), updated.getId());
        return updated;
    }

    @Override
    public void deleteRule(Long id) {
        if (!alertRuleRepository.existsById(id)) {
            throw new ServiceException("Alert rule not found: " + id);
        }
        
        // 级联删除相关数据
        alertConditionRepository.deleteByRuleId(id);
        alertNotificationRepository.deleteByRuleId(id);
        alertEscalationRepository.deleteByRuleId(id);
        // 注意：告警记录（AlertRecord）通常需要保留用于审计，不做级联删除
        
        alertRuleRepository.deleteById(id);
        log.info("Deleted alert rule: {} (with conditions, notifications, escalations)", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlertRule> getRule(Long id) {
        return alertRuleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRule> listRules(Pageable pageable) {
        return alertRuleRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRule> findByAlertType(AlertRule.AlertType alertType) {
        return alertRuleRepository.findByAlertType(alertType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRule> findEnabledByTargetType(AlertRule.TargetType targetType) {
        return alertRuleRepository.findByTargetTypeAndEnabledTrue(targetType);
    }

    @Override
    public void enableRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + id));
        rule.setEnabled(true);
        rule.setStatus(AlertRule.RuleStatus.ENABLED);
        alertRuleRepository.save(rule);
        log.info("Enabled alert rule: {}", id);
    }

    @Override
    public void disableRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + id));
        rule.setEnabled(false);
        rule.setStatus(AlertRule.RuleStatus.DISABLED);
        alertRuleRepository.save(rule);
        log.info("Disabled alert rule: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRule> getEnabledRules() {
        return alertRuleRepository.findByEnabledTrue();
    }

    // ==================== 条件管理 ====================

    @Override
    @Transactional(readOnly = true)
    public List<AlertCondition> getRuleConditions(Long ruleId) {
        if (!alertRuleRepository.existsById(ruleId)) {
            throw new ServiceException("Alert rule not found: " + ruleId);
        }
        return alertConditionRepository.findByRuleId(ruleId);
    }

    @Override
    public AlertCondition addCondition(Long ruleId, AlertCondition condition) {
        if (!alertRuleRepository.existsById(ruleId)) {
            throw new ServiceException("Alert rule not found: " + ruleId);
        }
        
        condition.setRuleId(ruleId);
        if (condition.getIsEnabled() == null) {
            condition.setIsEnabled(true);
        }
        if (condition.getSortOrder() == null) {
            condition.setSortOrder(0);
        }
        
        AlertCondition saved = alertConditionRepository.save(condition);
        log.info("Added condition {} to rule {}", saved.getId(), ruleId);
        return saved;
    }

    @Override
    public List<AlertCondition> addConditions(Long ruleId, List<AlertCondition> conditions) {
        if (!alertRuleRepository.existsById(ruleId)) {
            throw new ServiceException("Alert rule not found: " + ruleId);
        }
        
        List<AlertCondition> savedConditions = new ArrayList<>();
        for (AlertCondition condition : conditions) {
            condition.setRuleId(ruleId);
            if (condition.getIsEnabled() == null) {
                condition.setIsEnabled(true);
            }
            savedConditions.add(alertConditionRepository.save(condition));
        }
        
        log.info("Added {} conditions to rule {}", savedConditions.size(), ruleId);
        return savedConditions;
    }

    @Override
    public AlertCondition updateCondition(Long conditionId, AlertCondition condition) {
        AlertCondition existing = alertConditionRepository.findById(conditionId)
                .orElseThrow(() -> new ServiceException("Condition not found: " + conditionId));
        
        // 更新字段
        existing.setConditionName(condition.getConditionName());
        existing.setConditionType(condition.getConditionType());
        existing.setMetricName(condition.getMetricName());
        existing.setOperator(condition.getOperator());
        existing.setThresholdValue(condition.getThresholdValue());
        existing.setStringValue(condition.getStringValue());
        existing.setLogicType(condition.getLogicType());
        existing.setParentConditionId(condition.getParentConditionId());
        existing.setSortOrder(condition.getSortOrder());
        existing.setDurationSeconds(condition.getDurationSeconds());
        if (condition.getIsEnabled() != null) {
            existing.setIsEnabled(condition.getIsEnabled());
        }
        
        AlertCondition updated = alertConditionRepository.save(existing);
        log.info("Updated condition {}", updated.getId());
        return updated;
    }

    @Override
    public void deleteCondition(Long conditionId) {
        if (!alertConditionRepository.existsById(conditionId)) {
            throw new ServiceException("Condition not found: " + conditionId);
        }
        alertConditionRepository.deleteById(conditionId);
        log.info("Deleted condition {}", conditionId);
    }

    @Override
    public void deleteConditions(List<Long> conditionIds) {
        alertConditionRepository.deleteByIdIn(conditionIds);
        log.info("Deleted {} conditions", conditionIds.size());
    }

    @Override
    public AlertRule saveRuleWithConditions(AlertRuleRequest request, Long createdBy) {
        // 1. 创建或更新规则
        AlertRule rule;
        if (request.getName() != null && alertRuleRepository.existsByName(request.getName())) {
            // 如果名称已存在，更新
            rule = alertRuleRepository.findByName(request.getName())
                    .orElseThrow(() -> new ServiceException("Rule not found"));
        } else {
            rule = createRule(request, createdBy);
        }

        // 2. 删除现有条件
        alertConditionRepository.deleteByRuleId(rule.getId());

        // 3. 添加新条件
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            for (AlertRuleRequest.AlertConditionDTO dto : request.getConditions()) {
                AlertCondition condition = AlertCondition.builder()
                        .conditionName(dto.getConditionName())
                        .conditionType(dto.getConditionType() != null 
                                ? AlertCondition.ConditionType.valueOf(dto.getConditionType()) 
                                : AlertCondition.ConditionType.THRESHOLD)
                        .metricName(dto.getMetricName())
                        .operator(dto.getOperator() != null 
                                ? AlertCondition.ComparisonOperator.valueOf(dto.getOperator()) 
                                : null)
                        .thresholdValue(dto.getThresholdValue())
                        .stringValue(dto.getStringValue())
                        .logicType(dto.getLogicType() != null 
                                ? AlertCondition.LogicType.valueOf(dto.getLogicType()) 
                                : AlertCondition.LogicType.AND)
                        .parentConditionId(dto.getParentConditionId())
                        .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                        .durationSeconds(dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 60)
                        .isEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true)
                        .build();
                condition.setRuleId(rule.getId());
                alertConditionRepository.save(condition);
            }
        }

        log.info("Saved rule {} with conditions", rule.getId());
        return alertRuleRepository.findById(rule.getId()).orElse(rule);
    }

    // ==================== 冷却期管理 ====================

    @Override
    public boolean isInCooldown(Long ruleId) {
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty()) {
            return false;
        }

        AlertRule rule = ruleOpt.get();
        if (rule.getCooldownSeconds() == null || rule.getCooldownSeconds() == 0) {
            return false;
        }

        LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(rule.getCooldownSeconds());

        // 查找规则在冷却期内的未解决告警
        List<com.aick.mmp.shared.model.AlertRecord> recentAlerts = 
                alertRecordRepository.findByRuleIdAndAlertTimeAfter(ruleId, cooldownStart);

        // 过滤出未解决的告警
        return recentAlerts.stream()
                .anyMatch(alert -> alert.getStatus() == com.aick.mmp.shared.model.AlertRecord.AlertStatus.UNRESOLVED);
    }

    @Override
    public long getRemainingCooldown(Long ruleId) {
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty()) {
            return 0;
        }

        AlertRule rule = ruleOpt.get();
        if (rule.getCooldownSeconds() == null || rule.getCooldownSeconds() == 0) {
            return 0;
        }

        LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(rule.getCooldownSeconds());
        List<com.aick.mmp.shared.model.AlertRecord> recentAlerts = 
                alertRecordRepository.findByRuleIdAndAlertTimeAfter(ruleId, cooldownStart);

        if (recentAlerts.isEmpty()) {
            return 0;
        }

        // 找到最近的未解决告警
        LocalDateTime lastAlertTime = recentAlerts.stream()
                .filter(alert -> alert.getStatus() == com.aick.mmp.shared.model.AlertRecord.AlertStatus.UNRESOLVED)
                .map(com.aick.mmp.shared.model.AlertRecord::getAlertTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (lastAlertTime == null) {
            return 0;
        }

        LocalDateTime cooldownEnd = lastAlertTime.plusSeconds(rule.getCooldownSeconds());
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), cooldownEnd).getSeconds();
        return Math.max(0, remainingSeconds);
    }

    // ==================== 规则测试 ====================

    @Override
    public boolean testRule(Long id) {
        if (!alertRuleRepository.existsById(id)) {
            throw new ServiceException("Alert rule not found: " + id);
        }
        
        // 简单测试：验证规则配置是否完整
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return false;
        }

        AlertRule rule = ruleOpt.get();
        
        // 基本验证
        if (rule.getName() == null || rule.getName().isBlank()) {
            return false;
        }
        if (rule.getAlertType() == null) {
            return false;
        }
        if (rule.getLevel() == null) {
            return false;
        }

        log.info("Rule {} passed basic validation", id);
        return true;
    }

    // ==================== 规则统计 ====================

    @Override
    public long countTriggers(Long ruleId, LocalDateTime since) {
        if (!alertRuleRepository.existsById(ruleId)) {
            throw new ServiceException("Alert rule not found: " + ruleId);
        }
        return alertRecordRepository.findByRuleIdAndAlertTimeAfter(ruleId, since).size();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getLastTriggeredTime(Long ruleId) {
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);
        return ruleOpt.map(AlertRule::getLastTriggeredAt).orElse(null);
    }

    // ==================== DTO Methods ====================

    @Override
    public AlertRuleDTO createAlertRule(AlertRuleDTO request) {
        // Convert DTO to Request
        AlertRuleRequest alertRuleRequest = convertToRequest(request);

        // Create the rule
        AlertRule rule = createRule(alertRuleRequest, null);

        // Convert back to DTO and return
        return convertToDTO(rule);
    }

    @Override
    public AlertRuleDTO updateAlertRule(Long id, AlertRuleDTO request) {
        // Convert DTO to Request
        AlertRuleRequest alertRuleRequest = convertToRequest(request);

        // Update the rule
        AlertRule rule = updateRule(id, alertRuleRequest);

        // Convert back to DTO and return
        return convertToDTO(rule);
    }

    // ==================== Helper Methods ====================

    private AlertRuleRequest convertToRequest(AlertRuleDTO dto) {
        AlertRuleRequest request = new AlertRuleRequest();
        request.setName(dto.getName());
        request.setEnabled(dto.getEnabled());
        request.setWarningThreshold((double) dto.getThresholdValue());
        // Add other mappings as needed
        return request;
    }

    private AlertRuleDTO convertToDTO(AlertRule rule) {
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setType(rule.getAlertType().name());
        dto.setEnabled(rule.getEnabled());
        dto.setThresholdValue(rule.getWarningThreshold() != null ? rule.getWarningThreshold().intValue() : null);
        // Add other mappings as needed
        return dto;
    }
}
