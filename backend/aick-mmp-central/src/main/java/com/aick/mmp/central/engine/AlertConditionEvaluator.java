package com.aick.mmp.central.engine;

import com.aick.mmp.shared.model.AlertCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 告警条件评估引擎
 * 支持 AND/OR 逻辑组合和嵌套条件
 */
@Component
@Slf4j
public class AlertConditionEvaluator {

    /**
     * 评估结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationResult {
        /** 是否满足条件 */
        private boolean satisfied;
        /** 满足的条件数量 */
        private int satisfiedCount;
        /** 总条件数量 */
        private int totalCount;
        /** 满足的条件详情 */
        private List<ConditionResult> results;
        /** 评估消息 */
        private String message;
    }

    /**
     * 单个条件的评估结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionResult {
        private Long conditionId;
        private String metricName;
        private boolean satisfied;
        private Double actualValue;
        private Double thresholdValue;
        private String operator;
        private String stringValue;
        private String message;
    }

    /**
     * 评估指标数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricData {
        /** 指标名称 */
        private String metricName;
        /** 指标值 */
        private Double value;
        /** 字符串值（用于状态比较） */
        private String stringValue;
        /** 采集时间 */
        private Date timestamp;
    }

    /**
     * 评估条件是否满足
     * @param conditions 条件列表
     * @param metricData 指标数据
     * @return 评估结果
     */
    public EvaluationResult evaluate(List<AlertCondition> conditions, Map<String, MetricData> metricData) {
        if (conditions == null || conditions.isEmpty()) {
            return EvaluationResult.builder()
                    .satisfied(false)
                    .message("No conditions to evaluate")
                    .build();
        }

        // 构建条件树
        Map<Long, List<AlertCondition>> conditionTree = buildConditionTree(conditions);
        
        // 获取根条件（无父条件的顶层条件）
        List<AlertCondition> rootConditions = conditionTree.getOrDefault(null, Collections.emptyList());
        
        if (rootConditions.isEmpty()) {
            return EvaluationResult.builder()
                    .satisfied(false)
                    .message("No root conditions found")
                    .build();
        }

        List<ConditionResult> allResults = new ArrayList<>();
        List<Boolean> rootResults = new ArrayList<>();

        // 评估每个根条件及其子条件
        for (AlertCondition root : rootConditions) {
            boolean result = evaluateConditionTree(root, conditionTree, metricData, allResults);
            rootResults.add(result);
        }

        // 使用 OR 逻辑组合根条件结果
        boolean overallSatisfied = rootResults.stream().anyMatch(r -> r);
        int satisfiedCount = (int) allResults.stream().filter(ConditionResult::isSatisfied).count();

        return EvaluationResult.builder()
                .satisfied(overallSatisfied)
                .satisfiedCount(satisfiedCount)
                .totalCount(allResults.size())
                .results(allResults)
                .message(overallSatisfied ? "Conditions met" : "Conditions not met")
                .build();
    }

    /**
     * 递归评估条件树
     */
    private boolean evaluateConditionTree(AlertCondition condition, 
                                          Map<Long, List<AlertCondition>> conditionTree,
                                          Map<String, MetricData> metricData,
                                          List<ConditionResult> results) {
        if (condition.getIsEnabled() == null || !condition.getIsEnabled()) {
            return true; // 禁用条件视为满足
        }

        List<AlertCondition> children = conditionTree.getOrDefault(condition.getId(), Collections.emptyList());
        
        if (children.isEmpty()) {
            // 叶子条件，直接评估
            ConditionResult result = evaluateSingleCondition(condition, metricData);
            results.add(result);
            return result.isSatisfied();
        }

        // 组合条件，递归评估子条件
        List<Boolean> childResults = new ArrayList<>();
        for (AlertCondition child : children) {
            boolean childResult = evaluateConditionTree(child, conditionTree, metricData, results);
            childResults.add(childResult);
        }

        // 根据逻辑类型组合结果
        AlertCondition.LogicType logicType = condition.getLogicType();
        if (logicType == null) {
            logicType = AlertCondition.LogicType.AND;
        }

        boolean combined;
        if (logicType == AlertCondition.LogicType.AND) {
            combined = childResults.stream().allMatch(r -> r);
        } else {
            combined = childResults.stream().anyMatch(r -> r);
        }

        return combined;
    }

    /**
     * 评估单个条件
     */
    private ConditionResult evaluateSingleCondition(AlertCondition condition, Map<String, MetricData> metricData) {
        String metricName = condition.getMetricName();
        MetricData data = metricData.get(metricName);

        if (data == null) {
            return ConditionResult.builder()
                    .conditionId(condition.getId())
                    .metricName(metricName)
                    .satisfied(false)
                    .message("Metric not found: " + metricName)
                    .build();
        }

        // 检查持续时间
        if (condition.getDurationSeconds() != null && condition.getDurationSeconds() > 0) {
            // TODO: 实现持续时间检查，需要存储历史数据
            log.debug("Duration check skipped for condition {}, duration: {}s", 
                    condition.getId(), condition.getDurationSeconds());
        }

        boolean satisfied;
        String message;

        AlertCondition.ConditionType conditionType = condition.getConditionType();
        if (conditionType == null) {
            conditionType = AlertCondition.ConditionType.THRESHOLD;
        }

        switch (conditionType) {
            case THRESHOLD:
                satisfied = condition.evaluate(data.getValue());
                message = satisfied 
                        ? String.format("Metric %s value %.2f meets threshold", metricName, data.getValue())
                        : String.format("Metric %s value %.2f does not meet threshold %.2f", 
                                metricName, data.getValue(), condition.getThresholdValue());
                return ConditionResult.builder()
                        .conditionId(condition.getId())
                        .metricName(metricName)
                        .satisfied(satisfied)
                        .actualValue(data.getValue())
                        .thresholdValue(condition.getThresholdValue())
                        .operator(condition.getOperator() != null ? condition.getOperator().name() : null)
                        .message(message)
                        .build();

            case STATUS:
                satisfied = condition.evaluateStatus(data.getStringValue());
                message = satisfied 
                        ? String.format("Metric %s status '%s' matches", metricName, data.getStringValue())
                        : String.format("Metric %s status '%s' does not match '%s'", 
                                metricName, data.getStringValue(), condition.getStringValue());
                return ConditionResult.builder()
                        .conditionId(condition.getId())
                        .metricName(metricName)
                        .satisfied(satisfied)
                        .stringValue(data.getStringValue())
                        .message(message)
                        .build();

            case EXPRESSION:
                // 表达式评估（简化实现）
                return evaluateExpression(condition, data);

            case TIME_RANGE:
                // 时间范围评估
                return evaluateTimeRange(condition, data);

            default:
                return ConditionResult.builder()
                        .conditionId(condition.getId())
                        .metricName(metricName)
                        .satisfied(false)
                        .message("Unknown condition type: " + conditionType)
                        .build();
        }
    }

    /**
     * 评估表达式条件
     */
    private ConditionResult evaluateExpression(AlertCondition condition, MetricData data) {
        String expression = condition.getStringValue();
        if (expression == null || expression.isEmpty()) {
            return ConditionResult.builder()
                    .conditionId(condition.getId())
                    .metricName(condition.getMetricName())
                    .satisfied(false)
                    .message("Empty expression")
                    .build();
        }

        // 简单的表达式评估（支持 >, <, >=, <=, ==, !=）
        // 格式: "metricName operator value" 或 "value operator metricName"
        String[] parts = expression.split("\\s+");
        if (parts.length < 3) {
            return ConditionResult.builder()
                    .conditionId(condition.getId())
                    .metricName(condition.getMetricName())
                    .satisfied(false)
                    .message("Invalid expression format")
                    .build();
        }

        try {
            double value = data.getValue() != null ? data.getValue() : 0;
            double threshold = Double.parseDouble(parts[2]);
            String operator = parts[1];

            boolean satisfied = switch (operator) {
                case ">" -> value > threshold;
                case ">=" -> value >= threshold;
                case "<" -> value < threshold;
                case "<=" -> value <= threshold;
                case "==" -> Math.abs(value - threshold) < 0.001;
                case "!=" -> Math.abs(value - threshold) >= 0.001;
                default -> false;
            };

            return ConditionResult.builder()
                    .conditionId(condition.getId())
                    .metricName(condition.getMetricName())
                    .satisfied(satisfied)
                    .actualValue(value)
                    .thresholdValue(threshold)
                    .operator(operator)
                    .message(satisfied ? "Expression satisfied" : "Expression not satisfied")
                    .build();
        } catch (NumberFormatException e) {
            return ConditionResult.builder()
                    .conditionId(condition.getId())
                    .metricName(condition.getMetricName())
                    .satisfied(false)
                    .message("Invalid number in expression: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 评估时间范围条件
     */
    private ConditionResult evaluateTimeRange(AlertCondition condition, MetricData data) {
        // TODO: 实现时间范围评估
        return ConditionResult.builder()
                .conditionId(condition.getId())
                .metricName(condition.getMetricName())
                .satisfied(false)
                .message("Time range evaluation not implemented")
                .build();
    }

    /**
     * 构建条件树（按父ID分组）
     */
    private Map<Long, List<AlertCondition>> buildConditionTree(List<AlertCondition> conditions) {
        Map<Long, List<AlertCondition>> tree = new HashMap<>();
        // 使用 null 作为顶层条件的键
        tree.put(null, new ArrayList<>());
        
        for (AlertCondition condition : conditions) {
            Long parentId = condition.getParentConditionId();
            tree.computeIfAbsent(parentId, k -> new ArrayList<>()).add(condition);
        }
        
        // 按排序顺序排序
        tree.values().forEach(list -> 
            list.sort(Comparator.comparingInt(c -> 
                c.getSortOrder() != null ? c.getSortOrder() : 0)));
        
        return tree;
    }

    /**
     * 从 JSON 构建条件列表
     * @param json JSON格式的条件定义
     * @return 条件列表
     */
    public List<AlertCondition> parseConditionsFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 简单的 JSON 解析（实际项目中建议使用 Jackson 或 Gson）
            return parseJsonArray(json);
        } catch (Exception e) {
            log.error("Failed to parse conditions from JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<AlertCondition> parseJsonArray(String json) {
        List<AlertCondition> conditions = new ArrayList<>();
        
        // 简单的正则匹配解析
        // 实际项目中建议使用 Jackson 的 ObjectMapper
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\{\"conditionName\":\"([^\"]+)\".*?\"conditionType\":\"([^\"]+)\".*?\"metricName\":\"([^\"]+)\".*?\"operator\":\"([^\"]+)\".*?\"thresholdValue\":([0-9.]+).*?\\}");
        
        java.util.regex.Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            AlertCondition condition = AlertCondition.builder()
                    .conditionName(matcher.group(1))
                    .conditionType(AlertCondition.ConditionType.valueOf(matcher.group(2)))
                    .metricName(matcher.group(3))
                    .operator(AlertCondition.ComparisonOperator.valueOf(matcher.group(4)))
                    .thresholdValue(Double.parseDouble(matcher.group(5)))
                    .isEnabled(true)
                    .build();
            conditions.add(condition);
        }
        
        return conditions;
    }

    /**
     * 评估简单阈值条件（使用旧版 AlertRule 的阈值字段）
     */
    public boolean evaluateSimpleThreshold(Double actualValue, AlertCondition.ComparisonOperator operator, 
                                          Double threshold) {
        if (actualValue == null || operator == null || threshold == null) {
            return false;
        }
        
        return switch (operator) {
            case GT -> actualValue > threshold;
            case GTE -> actualValue >= threshold;
            case LT -> actualValue < threshold;
            case LTE -> actualValue <= threshold;
            case EQ -> Math.abs(actualValue - threshold) < 0.001;
            case NEQ -> Math.abs(actualValue - threshold) >= 0.001;
        };
    }
}
