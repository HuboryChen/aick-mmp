package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.repository.*;
import com.aick.mmp.central.service.AnalyticsService;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AnalyticsData;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.Camera.CameraStatus;
import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsDataRepository analyticsDataRepository;
    private final CameraService cameraService;
    private final CameraRepository cameraRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final RecordingRepository recordingRepository;

    @Override
    public DeviceUsageStatsDTO getDeviceUsageStats(LocalDateTime startTime, LocalDateTime endTime,
                                                    AggregationLevel level, List<Long> cameraIds) {
        // 获取设备统计
        List<Camera> cameras = cameraIds != null && !cameraIds.isEmpty()
                ? cameraRepository.findByIdIn(cameraIds)
                : cameraRepository.findAll();

        long total = cameras.size();
        long online = cameras.stream().filter(c -> c.getStatus() == CameraStatus.ONLINE).count();
        long offline = cameras.stream().filter(c -> c.getStatus() == CameraStatus.OFFLINE).count();
        long error = cameras.stream().filter(c -> c.getStatus() == CameraStatus.ERROR).count();
        long maintenance = cameras.stream().filter(c -> c.getStatus() == CameraStatus.MAINTENANCE).count();

        double onlineRate = total > 0 ? (double) online / total * 100 : 0;
        double offlineRate = total > 0 ? (double) offline / total * 100 : 0;
        double failureRate = total > 0 ? (double) error / total * 100 : 0;

        // 计算MTBF和MTTR (简化版)
        double mtbf = calculateMTBF(cameras);
        double mttr = calculateMTTR(cameras);

        // 获取趋势数据
        List<DeviceUsageStatsDTO.TrendData> trends = getDeviceUsageTrends(startTime, endTime, level);

        // 获取设备详情
        List<DeviceUsageStatsDTO.DeviceDetail> deviceDetails = cameras.stream()
                .map(this::convertToDeviceDetail)
                .collect(Collectors.toList());

        return DeviceUsageStatsDTO.builder()
                .onlineRate(onlineRate)
                .offlineRate(offlineRate)
                .failureRate(failureRate)
                .mtbf(mtbf)
                .mttr(mttr)
                .stats(DeviceUsageStatsDTO.DeviceStats.builder()
                        .total(total)
                        .online(online)
                        .offline(offline)
                        .abnormal(error)
                        .maintenance(maintenance)
                        .build())
                .trends(trends)
                .deviceDetails(deviceDetails)
                .build();
    }

    @Override
    public BandwidthStatsDTO getBandwidthStats(LocalDateTime startTime, LocalDateTime endTime,
                                                AggregationLevel level) {
        // 从analytics_data获取带宽数据
        List<AnalyticsData> bandwidthData = analyticsDataRepository
                .findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(
                        AnalyticsType.NETWORK_BANDWIDTH, level, startTime, endTime);

        if (bandwidthData.isEmpty()) {
            return BandwidthStatsDTO.builder()
                    .currentBandwidth(0.0)
                    .averageBandwidth(0.0)
                    .peakBandwidth(0.0)
                    .usageRate(0.0)
                    .totalTraffic(0.0)
                    .build();
        }

        List<Double> values = bandwidthData.stream()
                .map(AnalyticsData::getMetricValue)
                .collect(Collectors.toList());

        double current = values.get(values.size() - 1);
        double average = values.stream().mapToDouble(v -> v).average().orElse(0);
        double peak = values.stream().mapToDouble(v -> v).max().orElse(0);
        double min = values.stream().mapToDouble(v -> v).min().orElse(0);
        double median = calculateMedian(values);
        double stdDev = calculateStdDev(values, average);

        // 带宽趋势
        List<BandwidthStatsDTO.BandwidthTrend> trends = bandwidthData.stream()
                .map(data -> BandwidthStatsDTO.BandwidthTrend.builder()
                        .timestamp(data.getPeriodStart())
                        .total(data.getMetricValue())
                        .build())
                .collect(Collectors.toList());

        return BandwidthStatsDTO.builder()
                .currentBandwidth(current)
                .averageBandwidth(average)
                .peakBandwidth(peak)
                .usageRate(calculateUsageRate(peak))
                .totalTraffic(calculateTraffic(values, startTime, endTime))
                .stats(BandwidthStatsDTO.BandwidthStats.builder()
                        .minBandwidth(min)
                        .maxBandwidth(peak)
                        .medianBandwidth(median)
                        .stdDev(stdDev)
                        .totalUpstream(0L)
                        .totalDownstream(0L)
                        .build())
                .trends(trends)
                .build();
    }

    @Override
    public StorageStatsDTO getStorageStats(LocalDateTime startTime, LocalDateTime endTime,
                                            AggregationLevel level) {
        // 从analytics_data获取存储数据
        List<AnalyticsData> storageData = analyticsDataRepository
                .findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(
                        AnalyticsType.STORAGE_CAPACITY, level, startTime, endTime);

        // 获取录像统计
        List<Recording> recordings = recordingRepository.findByStartTimeBetween(startTime, endTime);
        long totalRecordingCount = recordings.size();
        double totalRecordingSize = recordings.stream()
                .mapToDouble(r -> r.getFileSize() != null ? r.getFileSize() / (1024.0 * 1024.0 * 1024.0) : 0)
                .sum();
        long totalDuration = recordings.stream()
                .mapToLong(r -> {
                    if (r.getStartTime() != null && r.getEndTime() != null) {
                        return Duration.between(r.getStartTime(), r.getEndTime()).getSeconds();
                    }
                    return 0L;
                })
                .sum();

        // 从analytics_data获取最新存储使用情况
        double usedStorage = 0;
        double totalCapacity = 1000.0; // 默认容量
        if (!storageData.isEmpty()) {
            AnalyticsData latest = storageData.get(storageData.size() - 1);
            usedStorage = latest.getMetricValue();
        }

        double usageRate = totalCapacity > 0 ? (usedStorage / totalCapacity) * 100 : 0;

        // 存储趋势
        List<StorageStatsDTO.StorageTrend> trends = storageData.stream()
                .map(data -> StorageStatsDTO.StorageTrend.builder()
                        .timestamp(data.getPeriodStart())
                        .usedStorage(data.getMetricValue())
                        .availableStorage(totalCapacity - data.getMetricValue())
                        .usageRate((data.getMetricValue() / totalCapacity) * 100)
                        .build())
                .collect(Collectors.toList());

        return StorageStatsDTO.builder()
                .totalCapacity(totalCapacity)
                .usedStorage(usedStorage)
                .availableStorage(totalCapacity - usedStorage)
                .usageRate(usageRate)
                .recordingStorage(StorageStatsDTO.RecordingStorage.builder()
                        .totalRecordingCount(totalRecordingCount)
                        .totalRecordingSize(totalRecordingSize)
                        .averageRecordingSize(totalRecordingCount > 0 ? totalRecordingSize / totalRecordingCount : 0)
                        .totalRecordingDuration(totalDuration)
                        .averageRecordingDuration(totalRecordingCount > 0 ? (long) ((double) totalDuration / totalRecordingCount) : 0L)
                        .retentionDays(30)
                        .build())
                .trends(trends)
                .build();
    }

    @Override
    public AlertStatsDTO getAlertStats(LocalDateTime startTime, LocalDateTime endTime,
                                        AggregationLevel level) {
        List<AlertRecord> alerts = alertRecordRepository.findByAlertTimeBetween(startTime, endTime);

        long total = alerts.size();
        long pending = alerts.stream().filter(a -> a.getStatus() == AlertRecord.AlertStatus.UNRESOLVED).count();
        long resolved = alerts.stream().filter(a -> a.getStatus() == AlertRecord.AlertStatus.RESOLVED).count();
        long critical = alerts.stream().filter(a -> a.getLevel() == AlertRule.AlertLevel.CRITICAL).count();
        long error = alerts.stream().filter(a -> a.getLevel() == AlertRule.AlertLevel.ERROR).count();
        long warning = alerts.stream().filter(a -> a.getLevel() == AlertRule.AlertLevel.WARNING).count();
        long info = alerts.stream().filter(a -> a.getLevel() == AlertRule.AlertLevel.INFO).count();

        double resolutionRate = total > 0 ? (double) resolved / total * 100 : 0;
        double avgResponseTime = calculateAvgResponseTime(alerts);

        // 类型分布
        Map<String, Long> typeCountMap = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.getAlertType().name(), Collectors.counting()));

        List<AlertStatsDTO.TypeDistribution> typeDistribution = typeCountMap.entrySet().stream()
                .map(e -> AlertStatsDTO.TypeDistribution.builder()
                        .alertType(e.getKey())
                        .typeName(e.getKey())
                        .count(e.getValue())
                        .percentage(total > 0 ? (double) e.getValue() / total * 100 : 0)
                        .build())
                .collect(Collectors.toList());

        // 级别分布
        List<AlertStatsDTO.LevelDistribution> levelDistribution = Arrays.asList(
                AlertStatsDTO.LevelDistribution.builder().level("CRITICAL").count(critical).percentage(total > 0 ? (double) critical / total * 100 : 0).build(),
                AlertStatsDTO.LevelDistribution.builder().level("ERROR").count(error).percentage(total > 0 ? (double) error / total * 100 : 0).build(),
                AlertStatsDTO.LevelDistribution.builder().level("WARNING").count(warning).percentage(total > 0 ? (double) warning / total * 100 : 0).build(),
                AlertStatsDTO.LevelDistribution.builder().level("INFO").count(info).percentage(total > 0 ? (double) info / total * 100 : 0).build()
        );

        // 趋势数据
        List<AlertStatsDTO.AlertTrend> trends = getAlertTrends(alerts, level);

        return AlertStatsDTO.builder()
                .totalAlerts(total)
                .pendingAlerts(pending)
                .resolvedAlerts(resolved)
                .resolutionRate(resolutionRate)
                .avgResponseTime(avgResponseTime)
                .stats(AlertStatsDTO.AlertStats.builder()
                        .total(total)
                        .critical(critical)
                        .major(error)
                        .minor(warning)
                        .info(info)
                        .avgResolutionTime(avgResponseTime)
                        .build())
                .trends(trends)
                .typeDistribution(typeDistribution)
                .levelDistribution(levelDistribution)
                .build();
    }

    @Override
    public AnalyticsResponseDTO getAnalyticsData(AnalyticsRequestDTO request) {
        List<AnalyticsData> dataList;

        if (request.getDimensions() != null && !request.getDimensions().isEmpty()) {
            dataList = analyticsDataRepository.findByTypeAndDimensions(
                    request.getType(), request.getDimensions(),
                    request.getStartTime(), request.getEndTime());
        } else {
            dataList = analyticsDataRepository
                    .findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(
                            request.getType(), request.getLevel(),
                            request.getStartTime(), request.getEndTime());
        }

        List<AnalyticsResponseDTO.DataPoint> dataPoints = dataList.stream()
                .map(this::convertToDataPoint)
                .collect(Collectors.toList());

        AnalyticsResponseDTO.Summary summary = calculateSummary(dataPoints);

        return AnalyticsResponseDTO.builder()
                .type(request.getType())
                .level(request.getLevel())
                .dataPoints(dataPoints)
                .summary(summary)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    @Override
    public List<AnalyticsResponseDTO.DataPoint> getTrendData(AnalyticsType type, String dimension,
                                                               LocalDateTime startTime, LocalDateTime endTime,
                                                               AggregationLevel level) {
        List<AnalyticsData> dataList;
        if (dimension != null) {
            dataList = analyticsDataRepository
                    .findByMetricNameAndPeriodStartBetweenOrderByPeriodStartAsc(dimension, startTime, endTime);
        } else {
            dataList = analyticsDataRepository
                    .findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(type, level, startTime, endTime);
        }

        return dataList.stream()
                .map(this::convertToDataPoint)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordAnalyticsData(AnalyticsType type, String dimension, String dimensionValue,
                                    String metricName, Double metricValue, String extraData) {
        AnalyticsData data = new AnalyticsData();
        data.setAnalyticsType(type);
        data.setDimension(dimension);
        data.setDimensionValue(dimensionValue);
        data.setMetricName(metricName);
        data.setMetricValue(metricValue);
        data.setExtraData(extraData);
        data.setAggregationLevel(AggregationLevel.MINUTE);
        data.setPeriodStart(LocalDateTime.now());
        data.setPeriodEnd(LocalDateTime.now());
        data.setCreatedAt(LocalDateTime.now());

        analyticsDataRepository.save(data);
    }

    @Override
    @Transactional
    public void recordBatchAnalyticsData(List<AnalyticsDataRecord> records) {
        LocalDateTime now = LocalDateTime.now();
        List<AnalyticsData> dataList = records.stream()
                .map(record -> {
                    AnalyticsData data = new AnalyticsData();
                    data.setAnalyticsType(record.type());
                    data.setDimension(record.dimension());
                    data.setDimensionValue(record.dimensionValue());
                    data.setMetricName(record.metricName());
                    data.setMetricValue(record.metricValue());
                    data.setExtraData(record.extraData());
                    data.setAggregationLevel(AggregationLevel.MINUTE);
                    data.setPeriodStart(now);
                    data.setPeriodEnd(now);
                    data.setCreatedAt(now);
                    return data;
                })
                .collect(Collectors.toList());

        analyticsDataRepository.saveAll(dataList);
    }

    @Override
    public AnalyticsResponseDTO.Summary calculateSummary(List<AnalyticsResponseDTO.DataPoint> dataPoints) {
        if (dataPoints.isEmpty()) {
            return AnalyticsResponseDTO.Summary.builder().build();
        }

        List<Double> values = dataPoints.stream()
                .map(AnalyticsResponseDTO.DataPoint::getValue)
                .collect(Collectors.toList());

        double total = values.stream().mapToDouble(v -> v).sum();
        double average = values.stream().mapToDouble(v -> v).average().orElse(0);
        double max = values.stream().mapToDouble(v -> v).max().orElse(0);
        double min = values.stream().mapToDouble(v -> v).min().orElse(0);
        double median = calculateMedian(values);
        double stdDev = calculateStdDev(values, average);
        double growthRate = calculateGrowthRate(values);

        return AnalyticsResponseDTO.Summary.builder()
                .total(total)
                .average(average)
                .max(max)
                .min(min)
                .median(median)
                .stdDev(stdDev)
                .count((long) values.size())
                .growthRate(growthRate)
                .build();
    }

    // ==================== 私有方法 ====================

    private double calculateMTBF(List<Camera> cameras) {
        // 简化计算：MTBF暂返回0，实际需要从历史数据计算
        return 0;
    }

    private double calculateMTTR(List<Camera> cameras) {
        // 简化计算：MTTR暂返回0，实际需要从历史数据计算
        return 0;
    }

    private List<DeviceUsageStatsDTO.TrendData> getDeviceUsageTrends(LocalDateTime startTime,
                                                                       LocalDateTime endTime,
                                                                       AggregationLevel level) {
        List<AnalyticsData> data = analyticsDataRepository
                .findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(
                        AnalyticsType.DEVICE_USAGE, level, startTime, endTime);

        return data.stream()
                .map(d -> DeviceUsageStatsDTO.TrendData.builder()
                        .timestamp(d.getPeriodStart())
                        .onlineRate(d.getMetricValue())
                        .offlineRate(100 - d.getMetricValue())
                        .failureRate(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AlertStatsDTO.AlertTrend> getAlertTrends(List<AlertRecord> alerts, AggregationLevel level) {
        // 按时间聚合告警数据
        Map<String, List<AlertRecord>> grouped = alerts.stream()
                .collect(Collectors.groupingBy(a -> {
                    LocalDateTime time = a.getAlertTime();
                    return switch (level) {
                        case HOUR -> time.withMinute(0).toString();
                        case DAY -> time.toLocalDate().toString();
                        case WEEK -> time.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
                        case MONTH -> time.getYear() + "-" + time.getMonthValue();
                        default -> time.toString();
                    };
                }));

        return grouped.entrySet().stream()
                .map(e -> {
                    List<AlertRecord> group = e.getValue();
                    long resolved = group.stream().filter(a -> a.getStatus() == AlertRecord.AlertStatus.RESOLVED).count();
                    long pending = group.size() - resolved;
                    return AlertStatsDTO.AlertTrend.builder()
                            .timestamp(LocalDateTime.parse(e.getKey()))
                            .count((long) group.size())
                            .resolved(resolved)
                            .pending(pending)
                            .build();
                })
                .sorted(Comparator.comparing(AlertStatsDTO.AlertTrend::getTimestamp))
                .collect(Collectors.toList());
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        }
        return sorted.get(size / 2);
    }

    private double calculateStdDev(List<Double> values, double mean) {
        if (values.size() <= 1) return 0;
        double sumSquaredDiff = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }

    private double calculateUsageRate(double peakBandwidth) {
        // 假设总带宽容量为1000Mbps
        double totalCapacity = 1000.0;
        return (peakBandwidth / totalCapacity) * 100;
    }

    private double calculateTraffic(List<Double> bandwidthValues, LocalDateTime start, LocalDateTime end) {
        // 简化计算：平均带宽 * 时间(小时)
        double avgBandwidth = bandwidthValues.stream().mapToDouble(v -> v).average().orElse(0);
        long hours = Duration.between(start, end).toHours();
        return avgBandwidth * hours / 8; // 转换为GB
    }

    private double calculateAvgResponseTime(List<AlertRecord> alerts) {
        // 简化计算：暂返回0，实际需要根据告警处理时间计算
        return 0;
    }

    private double calculateGrowthRate(List<Double> values) {
        if (values.size() < 2) return 0;
        double first = values.get(0);
        double last = values.get(values.size() - 1);
        if (first == 0) return 0;
        return ((last - first) / first) * 100;
    }

    private DeviceUsageStatsDTO.DeviceDetail convertToDeviceDetail(Camera camera) {
        return DeviceUsageStatsDTO.DeviceDetail.builder()
                .cameraId(camera.getId())
                .cameraName(camera.getName())
                .status(camera.getStatus() != null ? camera.getStatus().name() : "UNKNOWN")
                .totalOnlineDuration(0L)
                .failureCount(0)
                .build();
    }

    private AnalyticsResponseDTO.DataPoint convertToDataPoint(AnalyticsData data) {
        return AnalyticsResponseDTO.DataPoint.builder()
                .timestamp(data.getPeriodStart())
                .value(data.getMetricValue())
                .dimension(data.getDimension())
                .dimensionValue(data.getDimensionValue())
                .build();
    }
}
