package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警条件实体
 * 用于定义复杂的告警条件，支持 AND/OR 逻辑组合
 */
@Entity
@Table(name = "alert_conditions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的告警规则ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 条件名称
     */
    @Column(name = "condition_name", length = 100)
    private String conditionName;

    /**
     * 条件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private ConditionType conditionType;

    /**
     * 监控指标名称（如 cpu_usage, memory_usage, status）
     */
    @Column(name = "metric_name", length = 100)
    private String metricName;

    /**
     * 比较操作符（GT, LT, EQ, GTE, LTE, NEQ）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operator")
    private ComparisonOperator operator;

    /**
     * 阈值
     */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /**
     * 字符串值（用于状态比较等）
     */
    @Column(name = "string_value", length = 255)
    private String stringValue;

    /**
     * 条件逻辑组合类型（AND, OR）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "logic_type")
    @Builder.Default
    private LogicType logicType = LogicType.AND;

    /**
     * 父条件ID（用于嵌套条件）
     */
    @Column(name = "parent_condition_id")
    private Long parentConditionId;

    /**
     * 条件在组内的排序顺序
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 持续时间（秒），指标持续满足条件多久才触发
     */
    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 60;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 条件类型枚举
     */
    public enum ConditionType {
        THRESHOLD,      // 阈值条件（数值比较）
        STATUS,         // 状态条件（状态比较）
        EXPRESSION,     // 表达式条件（复杂表达式）
        TIME_RANGE,     // 时间范围条件
        COMPOSITE       // 组合条件（包含子条件）
    }

    /**
     * 比较操作符枚举
     */
    public enum ComparisonOperator {
        GT,     // 大于
        GTE,    // 大于等于
        LT,     // 小于
        LTE,    // 小于等于
        EQ,     // 等于
        NEQ     // 不等于
    }

    /**
     * 逻辑组合类型枚举
     */
    public enum LogicType {
        AND,    // 与
        OR      // 或
    }

    /**
     * 评估条件是否满足
     */
    public boolean evaluate(Double actualValue) {
        if (actualValue == null || operator == null) {
            return false;
        }
        
        return switch (operator) {
            case GT -> actualValue > thresholdValue;
            case GTE -> actualValue >= thresholdValue;
            case LT -> actualValue < thresholdValue;
            case LTE -> actualValue <= thresholdValue;
            case EQ -> Math.abs(actualValue - thresholdValue) < 0.001;
            case NEQ -> Math.abs(actualValue - thresholdValue) >= 0.001;
        };
    }

    /**
     * 评估状态条件是否满足
     */
    public boolean evaluateStatus(String actualStatus) {
        if (actualStatus == null || stringValue == null) {
            return false;
        }
        
        return switch (operator) {
            case EQ -> actualStatus.equals(stringValue);
            case NEQ -> !actualStatus.equals(stringValue);
            default -> false;
        };
    }
}
