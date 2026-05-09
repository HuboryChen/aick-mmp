package com.aick.mmp.central.service;

import com.aick.mmp.central.engine.AlertConditionEvaluator;
import com.aick.mmp.central.repository.AlertConditionRepository;
import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertCondition;
import com.aick.mmp.shared.model.AlertRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警规则测试服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRuleTestService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertConditionRepository alertConditionRepository;
    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final AlertConditionEvaluator evaluator;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private Long ruleId;
        private String ruleName;
        private boolean success;
        private String message;
        private List<AlertConditionEvaluator.ConditionResult> conditionResults;
        private Map<String, Object> testData;
    }

    /**
     * 测试告警规则
     */
    public TestResult testRule(Long ruleId, Map<String, Object> testData) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + ruleId));

        TestResult.TestResultBuilder resultBuilder = TestResult.builder()
                .ruleId(ruleId)
                .ruleName(rule.getName());

        List<AlertConditionEvaluator.ConditionResult> conditionResults = new ArrayList<>();

        try {
            // 获取规则的条件列表
            List<AlertCondition> conditions = alertConditionRepository.findByRuleIdAndIsEnabledTrue(ruleId);

            // 转换为评估器需要的格式
            Map<String, AlertConditionEvaluator.MetricData> metricData = convertTestData(testData, rule);

            // 评估条件
            AlertConditionEvaluator.EvaluationResult evalResult = evaluator.evaluate(conditions, metricData);

            if (conditions.isEmpty()) {
                // 如果没有条件，使用简单的阈值评估
                boolean thresholdMet = testSimpleThreshold(rule, testData);
                return resultBuilder
                        .success(thresholdMet)
                        .message(thresholdMet ? "Threshold condition met" : "Threshold condition not met")
                        .testData(testData)
                        .build();
            }

            return resultBuilder
                    .success(evalResult.isSatisfied())
                    .message(evalResult.getMessage())
                    .conditionResults(evalResult.getResults())
                    .testData(testData)
                    .build();

        } catch (Exception e) {
            log.error("Error testing rule {}: {}", ruleId, e.getMessage(), e);
            return resultBuilder
                    .success(false)
                    .message("Test failed: " + e.getMessage())
                    .testData(testData)
                    .build();
        }
    }

    /**
     * 使用示例数据测试规则
     */
    public TestResult testRuleWithSampleData(Long ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + ruleId));

        // 根据规则类型生成示例数据
        Map<String, Object> sampleData = generateSampleData(rule);

        return testRule(ruleId, sampleData);
    }

    /**
     * 根据规则类型生成示例数据
     */
    private Map<String, Object> generateSampleData(AlertRule rule) {
        Map<String, Object> data = new HashMap<>();

        switch (rule.getAlertType()) {
            case CPU_USAGE:
                data.put("cpu_usage", 85.0);
                break;
            case MEMORY_USAGE:
                data.put("memory_usage", 90.0);
                break;
            case DISK_USAGE:
                data.put("disk_usage", 80.0);
                break;
            case NETWORK_LATENCY:
                data.put("network_latency", 150.0);
                break;
            case CAMERA_OFFLINE:
                data.put("status", "OFFLINE");
                break;
            case CAMERA_ERROR:
                data.put("error_code", "CONNECTION_FAILED");
                break;
            case EDGE_NODE_OFFLINE:
                data.put("status", "OFFLINE");
                break;
            case STREAM_INTERRUPTED:
                data.put("interrupted", true);
                break;
            case MOTION_DETECTED:
                data.put("motion_detected", true);
                break;
            case RECORDING_FAILED:
                data.put("recording_status", "FAILED");
                break;
            default:
                data.put("value", 100.0);
        }

        // 添加通用指标
        data.put("timestamp", System.currentTimeMillis());

        return data;
    }

    /**
     * 转换测试数据为评估器需要的格式
     */
    private Map<String, AlertConditionEvaluator.MetricData> convertTestData(
            Map<String, Object> testData, AlertRule rule) {
        
        Map<String, AlertConditionEvaluator.MetricData> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : testData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            AlertConditionEvaluator.MetricData.MetricDataBuilder dataBuilder = AlertConditionEvaluator.MetricData.builder()
                    .metricName(key)
                    .timestamp(new Date());

            if (value instanceof Number) {
                dataBuilder.value(((Number) value).doubleValue());
            } else if (value instanceof String) {
                dataBuilder.stringValue((String) value);
            } else if (value instanceof Boolean) {
                dataBuilder.stringValue(value.toString());
                dataBuilder.value((Boolean) value ? 1.0 : 0.0);
            }

            result.put(key, dataBuilder.build());
        }

        return result;
    }

    /**
     * 测试简单阈值条件（旧版 AlertRule）
     */
    private boolean testSimpleThreshold(AlertRule rule, Map<String, Object> testData) {
        String metricName = getMetricNameForAlertType(rule.getAlertType());
        Object value = testData.get(metricName);

        if (value == null) {
            return false;
        }

        double numValue = value instanceof Number ? ((Number) value).doubleValue() : 0;

        // 根据级别判断使用哪个阈值
        Double threshold = rule.getCriticalThreshold() != null 
                ? rule.getCriticalThreshold() 
                : rule.getWarningThreshold();

        if (threshold == null) {
            return false;
        }

        // 判断是否超过阈值
        return numValue > threshold;
    }

    /**
     * 获取告警类型对应的指标名称
     */
    private String getMetricNameForAlertType(AlertRule.AlertType alertType) {
        return switch (alertType) {
            case CPU_USAGE -> "cpu_usage";
            case MEMORY_USAGE -> "memory_usage";
            case DISK_USAGE -> "disk_usage";
            case NETWORK_LATENCY -> "network_latency";
            case CAMERA_OFFLINE, CAMERA_ERROR -> "status";
            case EDGE_NODE_OFFLINE -> "status";
            case STREAM_INTERRUPTED -> "interrupted";
            case MOTION_DETECTED -> "motion_detected";
            case RECORDING_FAILED -> "recording_status";
            case SYSTEM_ERROR -> "error_code";
            default -> "value";
        };
    }

    /**
     * 验证规则配置
     */
    public List<String> validateRule(Long ruleId) {
        List<String> errors = new ArrayList<>();

        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ServiceException("Alert rule not found: " + ruleId));

        // 验证基本配置
        if (rule.getName() == null || rule.getName().isBlank()) {
            errors.add("Rule name is required");
        }

        if (rule.getAlertType() == null) {
            errors.add("Alert type is required");
        }

        if (rule.getLevel() == null) {
            errors.add("Alert level is required");
        }

        if (rule.getTargetType() == null) {
            errors.add("Target type is required");
        }

        // 验证阈值配置
        if (rule.getWarningThreshold() != null && rule.getCriticalThreshold() != null) {
            if (rule.getWarningThreshold() >= rule.getCriticalThreshold()) {
                errors.add("Warning threshold should be less than critical threshold");
            }
        }

        // 验证时间配置
        if (rule.getDurationSeconds() != null && rule.getDurationSeconds() < 1) {
            errors.add("Duration must be at least 1 second");
        }

        if (rule.getCooldownSeconds() != null && rule.getCooldownSeconds() < 0) {
            errors.add("Cooldown must be non-negative");
        }

        // 验证目标配置
        if (rule.getTargetId() != null && rule.getTargetType() == AlertRule.TargetType.CAMERA) {
            if (!cameraRepository.existsById(rule.getTargetId())) {
                errors.add("Target camera does not exist");
            }
        }

        if (rule.getTargetId() != null && rule.getTargetType() == AlertRule.TargetType.EDGE_NODE) {
            if (!edgeNodeRepository.existsById(rule.getTargetId())) {
                errors.add("Target edge node does not exist");
            }
        }

        // 验证通知配置
        if (rule.getNotificationMethod() != null) {
            switch (rule.getNotificationMethod()) {
                case EMAIL:
                case SMS:
                    if (rule.getNotificationTarget() == null || rule.getNotificationTarget().isBlank()) {
                        errors.add("Notification target is required for " + rule.getNotificationMethod());
                    }
                    break;
                case WEBHOOK:
                    if (rule.getNotificationTarget() != null && !rule.getNotificationTarget().startsWith("http")) {
                        errors.add("Webhook URL must start with http:// or https://");
                    }
                    break;
            }
        }

        // 验证条件配置
        List<AlertCondition> conditions = alertConditionRepository.findByRuleId(ruleId);
        errors.addAll(validateConditions(conditions));

        return errors;
    }

    /**
     * 验证条件配置
     */
    private List<String> validateConditions(List<AlertCondition> conditions) {
        List<String> errors = new ArrayList<>();

        // 检查循环引用
        Set<Long> rootIds = conditions.stream()
                .filter(c -> c.getParentConditionId() == null)
                .map(AlertCondition::getId)
                .collect(Collectors.toSet());

        for (AlertCondition condition : conditions) {
            if (condition.getParentConditionId() != null) {
                Long parentId = condition.getParentConditionId();
                if (!conditions.stream().anyMatch(c -> c.getId().equals(parentId))) {
                    errors.add("Condition " + condition.getId() + " has invalid parent ID: " + parentId);
                }
            }
        }

        // 检查条件配置
        for (AlertCondition condition : conditions) {
            if (condition.getConditionType() == AlertCondition.ConditionType.THRESHOLD) {
                if (condition.getThresholdValue() == null) {
                    errors.add("Threshold value is required for condition: " + condition.getConditionName());
                }
                if (condition.getOperator() == null) {
                    errors.add("Operator is required for threshold condition: " + condition.getConditionName());
                }
            }

            if (condition.getMetricName() == null || condition.getMetricName().isBlank()) {
                errors.add("Metric name is required for condition: " + condition.getConditionName());
            }
        }

        return errors;
    }
}
