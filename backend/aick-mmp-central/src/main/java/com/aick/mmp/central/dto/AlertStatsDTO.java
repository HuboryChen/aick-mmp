package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsDTO {

    /**
     * 告警总数
     */
    private Long totalAlerts;

    /**
     * 待处理告警数
     */
    private Long pendingAlerts;

    /**
     * 已处理告警数
     */
    private Long resolvedAlerts;

    /**
     * 处理率(%)
     */
    private Double resolutionRate;

    /**
     * 平均响应时间(分钟)
     */
    private Double avgResponseTime;

    /**
     * 告警统计
     */
    private AlertStats stats;

    /**
     * 告警趋势
     */
    private List<AlertTrend> trends;

    /**
     * 告警类型分布
     */
    private List<TypeDistribution> typeDistribution;

    /**
     * 告警级别分布
     */
    private List<LevelDistribution> levelDistribution;

    /**
     * 告警统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertStats {
        private Long total;
        private Long critical;
        private Long major;
        private Long minor;
        private Long info;
        private Double avgResolutionTime; // 平均解决时间(分钟)
    }

    /**
     * 告警趋势
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertTrend {
        private LocalDateTime timestamp;
        private Long count;
        private Long resolved;
        private Long pending;
    }

    /**
     * 类型分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeDistribution {
        private String alertType;
        private String typeName;
        private Long count;
        private Double percentage;
    }

    /**
     * 级别分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelDistribution {
        private String level;
        private Long count;
        private Double percentage;
    }
}
