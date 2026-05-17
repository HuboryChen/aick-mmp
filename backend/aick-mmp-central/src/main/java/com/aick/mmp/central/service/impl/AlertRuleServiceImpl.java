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
import com.aick.mmp.shared.model.AlertCondition;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AlertRecord;
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
import java.util.stream.Collectors;

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
    public AlertRuleDTO createAlertRule(AlertRuleDTO request) {
        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .description("")
                .alertType(AlertRule.AlertType.valueOf(request.getType()))
                .level(AlertRule.AlertLevel.valueOf("MEDIUM"))
                .targetType(AlertRule.TargetType.SYSTEM)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .warningThreshold(request.getThresholdValue() != null ? request.getThresholdValue().doubleValue() : 80.0)
                .criticalThreshold(request.getThresholdValue() != null ? request.getThresholdValue().doubleValue() * 1.2 : 96.0)
                .status(AlertRule.RuleStatus.ENABLED)
                .notificationMethod(AlertRule.NotificationMethod.IN_APP)
                .build();

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: {} (ID: {})", saved.getName(), saved.getId());

        // Convert to DTO
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        dto.setType(saved.getAlertType().name());
        dto.setEnabled(saved.getEnabled());
        dto.setThresholdValue(saved.getWarningThreshold() != null ? saved.getWarningThreshold().intValue() : 80);

        return dto;
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
    public AlertRuleDTO updateAlertRule(Long id, AlertRuleDTO request) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + id));

        if (request.getName() != null) {
            rule.setName(request.getName());
        }
        if (request.getType() != null) {
            rule.setAlertType(AlertRule.AlertType.valueOf(request.getType()));
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
            rule.setStatus(request.getEnabled() ? AlertRule.RuleStatus.ENABLED : AlertRule.RuleStatus.DISABLED);
        }
        if (request.getThresholdValue() != null) {
            rule.setWarningThreshold(request.getThresholdValue().doubleValue());
            rule.setCriticalThreshold(request.getThresholdValue().doubleValue() * 1.2);
        }

        AlertRule updated = alertRuleRepository.save(rule);
        log.info("Updated alert rule: {} (ID: {})", updated.getName(), updated.getId());

        // Convert to DTO
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setId(updated.getId());
        dto.setName(updated.getName());
        dto.setType(updated.getAlertType().name());
        dto.setEnabled(updated.getEnabled());
        dto.setThresholdValue(updated.getWarningThreshold() != null ? updated.getWarningThreshold().intValue() : 80);

        return dto;
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
                    .orElseThrow(() -> new ServiceException("Rule not found: " + request.getName()));

            // 更新规则属性（略，类似于updateRule方法）
        } else {
            // 创建新规则
            rule = createRule(request, createdBy);
        }

        // 2. 处理条件
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            // 删除现有条件
            alertConditionRepository.deleteByRuleId(rule.getId());

            // 添加新条件 - convert DTO to entity
            for (AlertRuleRequest.AlertConditionDTO conditionDTO : request.getConditions()) {
                AlertCondition condition = convertToCondition(conditionDTO);
                addCondition(rule.getId(), condition);
            }
        }

        log.info("Saved rule {} with conditions", rule.getId());
        return rule;
    }

    // ==================== 冷却期管理 ====================

    @Override
    public boolean isInCooldown(Long ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + ruleId));

        if (!rule.getEnabled() || rule.getCooldownSeconds() == null || rule.getCooldownSeconds() <= 0) {
            return false;
        }

        // 获取最后触发时间
        LocalDateTime lastTriggered = getLastTriggeredTime(ruleId);
        if (lastTriggered == null) {
            return false;
        }

        // 计算冷却期是否结束
        LocalDateTime cooldownEnd = lastTriggered.plusSeconds(rule.getCooldownSeconds());
        return LocalDateTime.now().isBefore(cooldownEnd);
    }

    @Override
    public long getRemainingCooldown(Long ruleId) {
        if (!isInCooldown(ruleId)) {
            return 0;
        }

        LocalDateTime lastTriggered = getLastTriggeredTime(ruleId);
        LocalDateTime cooldownEnd = lastTriggered.plusSeconds(
                alertRuleRepository.findById(ruleId).get().getCooldownSeconds());

        return java.time.Duration.between(LocalDateTime.now(), cooldownEnd).getSeconds();
    }

    // ==================== 规则测试 ====================

    @Override
    public boolean testRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + id));

        // 简单的测试逻辑：返回true规则已启用
        return rule.getEnabled();
    }

    // ==================== 规则统计 ====================

    @Override
    public long countTriggers(Long ruleId, LocalDateTime since) {
        return alertRecordRepository.countByRuleIdAndAlertTimeAfter(ruleId, since);
    }

    @Override
    public LocalDateTime getLastTriggeredTime(Long ruleId) {
        return alertRecordRepository.findTopByRuleIdOrderByAlertTimeDesc(ruleId)
                .map(record -> record.getAlertTime())
                .orElse(null);
    }

    private AlertCondition convertToCondition(AlertRuleRequest.AlertConditionDTO dto) {
        AlertCondition condition = new AlertCondition();
        condition.setId(dto.getId());
        condition.setConditionName(dto.getConditionName());
        condition.setConditionType(AlertCondition.ConditionType.valueOf(dto.getConditionType()));
        condition.setMetricName(dto.getMetricName());
        condition.setOperator(AlertCondition.ComparisonOperator.valueOf(dto.getOperator()));
        condition.setThresholdValue(dto.getThresholdValue());
        condition.setStringValue(dto.getStringValue());
        condition.setLogicType(AlertCondition.LogicType.valueOf(dto.getLogicType()));
        condition.setParentConditionId(dto.getParentConditionId());
        condition.setSortOrder(dto.getSortOrder());
        condition.setDurationSeconds(dto.getDurationSeconds());
        condition.setIsEnabled(dto.getIsEnabled());
        return condition;
    }
}