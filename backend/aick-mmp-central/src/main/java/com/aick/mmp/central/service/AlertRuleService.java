package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.AlertRuleRequest;
import com.aick.mmp.shared.model.AlertCondition;
import com.aick.mmp.shared.model.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import com.aick.mmp.central.dto.AlertRuleDTO;
import com.aick.mmp.central.dto.AlertRuleRequest;

/**
 * 告警规则服务接口
 */
public interface AlertRuleService {

    // ==================== 规则 CRUD ====================

    /**
     * 创建告警规则
     */
    AlertRule createRule(AlertRuleRequest request, Long createdBy);

    /**
     * 创建告警规则（DTO版本）
     */
    AlertRuleDTO createAlertRule(AlertRuleDTO request);

    /**
     * 更新告警规则
     */
    AlertRule updateRule(Long id, AlertRuleRequest request);

    /**
     * 更新告警规则（DTO版本）
     */
    AlertRuleDTO updateAlertRule(Long id, AlertRuleDTO request);

    /**
     * 更新告警规则
     */
    AlertRule updateRule(Long id, AlertRuleRequest request);

    /**
     * 删除告警规则（级联删除条件和通知配置）
     */
    void deleteRule(Long id);

    /**
     * 获取告警规则详情
     */
    Optional<AlertRule> getRule(Long id);

    /**
     * 分页查询告警规则
     */
    Page<AlertRule> listRules(Pageable pageable);

    /**
     * 根据告警类型查询规则
     */
    List<AlertRule> findByAlertType(AlertRule.AlertType alertType);

    /**
     * 根据目标类型查询启用的规则
     */
    List<AlertRule> findEnabledByTargetType(AlertRule.TargetType targetType);

    /**
     * 启用规则
     */
    void enableRule(Long id);

    /**
     * 禁用规则
     */
    void disableRule(Long id);

    /**
     * 获取所有启用的规则
     */
    List<AlertRule> getEnabledRules();

    // ==================== 条件管理 ====================

    /**
     * 获取规则的所有条件
     */
    List<AlertCondition> getRuleConditions(Long ruleId);

    /**
     * 添加条件到规则
     */
    AlertCondition addCondition(Long ruleId, AlertCondition condition);

    /**
     * 批量添加条件到规则
     */
    List<AlertCondition> addConditions(Long ruleId, List<AlertCondition> conditions);

    /**
     * 更新条件
     */
    AlertCondition updateCondition(Long conditionId, AlertCondition condition);

    /**
     * 删除条件
     */
    void deleteCondition(Long conditionId);

    /**
     * 批量删除条件
     */
    void deleteConditions(List<Long> conditionIds);

    /**
     * 保存规则及其条件
     */
    AlertRule saveRuleWithConditions(AlertRuleRequest request, Long createdBy);

    // ==================== 冷却期管理 ====================

    /**
     * 检查规则是否处于冷却期
     */
    boolean isInCooldown(Long ruleId);

    /**
     * 获取规则剩余冷却时间（秒）
     */
    long getRemainingCooldown(Long ruleId);

    // ==================== 规则测试 ====================

    /**
     * 测试规则
     */
    boolean testRule(Long id);

    // ==================== 规则统计 ====================

    /**
     * 统计规则的触发次数（指定时间内）
     */
    long countTriggers(Long ruleId, java.time.LocalDateTime since);

    /**
     * 获取规则的最后触发时间
     */
    java.time.LocalDateTime getLastTriggeredTime(Long ruleId);
}
