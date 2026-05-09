package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析数据响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDTO {

    /**
     * 统计类型
     */
    private AnalyticsType type;

    /**
     * 聚合粒度
     */
    private AggregationLevel level;

    /**
     * 数据点列表
     */
    private List<DataPoint> dataPoints;

    /**
     * 汇总统计
     */
    private Summary summary;

    /**
     * 统计维度信息
     */
    private Map<String, Object> dimensions;

    /**
     * 查询时间范围
     */
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private LocalDateTime timestamp;
        private Double value;
        private String dimension;
        private String dimensionValue;
        private Map<String, Object> extraData;
    }

    /**
     * 汇总统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Double total;
        private Double average;
        private Double max;
        private Double min;
        private Double median;
        private Long count;
        private Double stdDev; // 标准差
        private Double growthRate; // 增长率
    }
}
