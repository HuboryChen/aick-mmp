package com.aick.mmp.shared.model;

import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 分析数据实体
 */
@Entity
@Table(name = "analytics_data", indexes = {
    @Index(name = "idx_analytics_type", columnList = "analytics_type"),
    @Index(name = "idx_analytics_dimension", columnList = "dimension"),
    @Index(name = "idx_analytics_period", columnList = "period_start")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 统计类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "analytics_type", nullable = false)
    private AnalyticsType analyticsType;

    /**
     * 聚合粒度
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_level", nullable = false)
    private AggregationLevel aggregationLevel;

    /**
     * 统计维度 - 如设备ID、区域ID、CDN节点ID等
     */
    @Column(length = 50)
    private String dimension;

    /**
     * 维度值 - 如具体的设备ID值
     */
    @Column(name = "dimension_value", length = 100)
    private String dimensionValue;

    /**
     * 统计周期开始时间
     */
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    /**
     * 统计周期结束时间
     */
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    /**
     * 指标名称
     */
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    /**
     * 指标值
     */
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    /**
     * 额外数据(JSON格式)
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
