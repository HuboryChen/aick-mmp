package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.AlertRule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 告警规则创建/更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Alert type is required")
    private AlertRule.AlertType alertType;

    @NotNull(message = "Alert level is required")
    private AlertRule.AlertLevel level;

    @NotNull(message = "Target type is required")
    private AlertRule.TargetType targetType;

    private Long targetId;

    private String thresholdExpression;

    private Double warningThreshold;

    private Double criticalThreshold;

    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 3600, message = "Duration cannot exceed 1 hour")
    private Integer durationSeconds;

    @Min(value = 0, message = "Cooldown must be non-negative")
    @Max(value = 86400, message = "Cooldown cannot exceed 24 hours")
    private Integer cooldownSeconds;

    private String alertSchedule;

    private Boolean enabled;

    private AlertRule.NotificationMethod notificationMethod;

    private String notificationTarget;

    /**
     * 告警条件列表（支持 AND/OR 组合）
     */
    private List<AlertConditionDTO> conditions;

    /**
     * 告警条件DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertConditionDTO {
        private Long id;
        private String conditionName;
        private String conditionType;
        private String metricName;
        private String operator;
        private Double thresholdValue;
        private String stringValue;
        private String logicType;
        private Long parentConditionId;
        private Integer sortOrder;
        private Integer durationSeconds;
        private Boolean isEnabled;
    }
}
