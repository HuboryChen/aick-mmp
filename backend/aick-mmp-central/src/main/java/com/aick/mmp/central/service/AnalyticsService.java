package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据分析服务接口
 */
public interface AnalyticsService {

    /**
     * 获取设备利用率统计
     */
    DeviceUsageStatsDTO getDeviceUsageStats(LocalDateTime startTime, LocalDateTime endTime,
                                            AggregationLevel level, List<Long> cameraIds);

    /**
     * 获取带宽分析统计
     */
    BandwidthStatsDTO getBandwidthStats(LocalDateTime startTime, LocalDateTime endTime,
                                        AggregationLevel level);

    /**
     * 获取存储容量统计
     */
    StorageStatsDTO getStorageStats(LocalDateTime startTime, LocalDateTime endTime,
                                    AggregationLevel level);

    /**
     * 获取告警统计
     */
    AlertStatsDTO getAlertStats(LocalDateTime startTime, LocalDateTime endTime,
                                AggregationLevel level);

    /**
     * 获取通用分析数据
     */
    AnalyticsResponseDTO getAnalyticsData(AnalyticsRequestDTO request);

    /**
     * 获取趋势数据
     */
    List<AnalyticsResponseDTO.DataPoint> getTrendData(AnalyticsType type, String dimension,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      AggregationLevel level);

    /**
     * 记录分析数据
     */
    void recordAnalyticsData(AnalyticsType type, String dimension, String dimensionValue,
                             String metricName, Double metricValue, String extraData);

    /**
     * 批量记录分析数据
     */
    void recordBatchAnalyticsData(List<AnalyticsDataRecord> records);

    /**
     * 获取汇总统计
     */
    AnalyticsResponseDTO.Summary calculateSummary(List<AnalyticsResponseDTO.DataPoint> dataPoints);

    /**
     * 数据记录
     */
    class AnalyticsDataRecord {
        private final AnalyticsType type;
        private final String dimension;
        private final String dimensionValue;
        private final String metricName;
        private final Double metricValue;
        private final String extraData;

        public AnalyticsDataRecord(AnalyticsType type, String dimension, String dimensionValue,
                                  String metricName, Double metricValue, String extraData) {
            this.type = type;
            this.dimension = dimension;
            this.dimensionValue = dimensionValue;
            this.metricName = metricName;
            this.metricValue = metricValue;
            this.extraData = extraData;
        }

        public AnalyticsType type() { return type; }
        public String dimension() { return dimension; }
        public String dimensionValue() { return dimensionValue; }
        public String metricName() { return metricName; }
        public Double metricValue() { return metricValue; }
        public String extraData() { return extraData; }
    }
}
