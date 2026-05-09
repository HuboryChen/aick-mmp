package com.aick.mmp.central.task;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.service.AnalyticsService;
import com.aick.mmp.central.service.CdnNodeService;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.aick.mmp.shared.model.Camera.CameraStatus;
import static com.aick.mmp.shared.model.CdnNode.NodeStatus;

/**
 * 分析数据收集定时任务
 * 每5分钟收集一次系统统计数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsCollectionTask {

    private final AnalyticsService analyticsService;
    private final CameraRepository cameraRepository;
    private final CdnNodeService cdnNodeService;

    /**
     * 每5分钟收集设备利用率数据
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void collectDeviceUsageData() {
        try {
            List<Camera> cameras = cameraRepository.findAll();
            
            long total = cameras.size();
            long online = cameras.stream().filter(c -> c.getStatus() == CameraStatus.ONLINE).count();
            double onlineRate = total > 0 ? (double) online / total * 100 : 0;

            // 记录设备在线率
            analyticsService.recordAnalyticsData(
                    AnalyticsType.DEVICE_USAGE,
                    "system",
                    "overall",
                    "online_rate",
                    onlineRate,
                    null
            );

            // 按区域统计
            cameras.stream()
                    .collect(Collectors.groupingBy(c -> c.getRegionId() != null ? c.getRegionId().toString() : "unassigned"))
                    .forEach((regionId, regionCameras) -> {
                        long regionOnline = regionCameras.stream().filter(c -> c.getStatus() == CameraStatus.ONLINE).count();
                        double regionRate = regionCameras.size() > 0 ? (double) regionOnline / regionCameras.size() * 100 : 0;

                        analyticsService.recordAnalyticsData(
                                AnalyticsType.DEVICE_USAGE,
                                "region",
                                regionId,
                                "online_rate",
                                regionRate,
                                null
                        );
                    });

            log.debug("Collected device usage data: onlineRate={}", onlineRate);
        } catch (Exception e) {
            log.error("Failed to collect device usage data", e);
        }
    }

    /**
     * 每5分钟收集带宽数据
     */
    @Scheduled(fixedRate = 300000)
    public void collectBandwidthData() {
        try {
            List<CdnNode> nodes = cdnNodeService.getAllNodes();
            
            // 使用带宽使用率作为指标
            double totalBandwidthUsage = nodes.stream()
                    .filter(n -> n.getStatus() == NodeStatus.ONLINE)
                    .mapToDouble(n -> n.getBandwidthUsage() != null ? n.getBandwidthUsage() : 0)
                    .sum();

            // 记录总带宽使用率
            analyticsService.recordAnalyticsData(
                    AnalyticsType.NETWORK_BANDWIDTH,
                    "system",
                    "total",
                    "bandwidth_usage",
                    totalBandwidthUsage,
                    null
            );

            // 按节点统计
            nodes.forEach(node -> {
                if (node.getStatus() == NodeStatus.ONLINE && node.getBandwidthUsage() != null) {
                    analyticsService.recordAnalyticsData(
                            AnalyticsType.NETWORK_BANDWIDTH,
                            "cdn_node",
                            node.getId().toString(),
                            "bandwidth_usage",
                            node.getBandwidthUsage(),
                            null
                    );
                }
            });

            log.debug("Collected bandwidth data: totalBandwidthUsage={}", totalBandwidthUsage);
        } catch (Exception e) {
            log.error("Failed to collect bandwidth data", e);
        }
    }

    /**
     * 每小时收集存储数据
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void collectStorageData() {
        try {
            List<CdnNode> nodes = cdnNodeService.getAllNodes();
            
            // 使用存储使用率作为指标
            double avgStorageUsage = nodes.stream()
                    .filter(n -> n.getStorageUsage() != null)
                    .mapToDouble(n -> n.getStorageUsage())
                    .average()
                    .orElse(0);

            analyticsService.recordAnalyticsData(
                    AnalyticsType.STORAGE_CAPACITY,
                    "system",
                    "total",
                    "storage_usage",
                    avgStorageUsage,
                    null
            );

            log.debug("Collected storage data: avgStorageUsage={}%", avgStorageUsage);
        } catch (Exception e) {
            log.error("Failed to collect storage data", e);
        }
    }

    /**
     * 每小时收集告警统计数据
     */
    @Scheduled(fixedRate = 3600000)
    public void collectAlertStats() {
        try {
            // 收集各类告警数量
            analyticsService.recordAnalyticsData(
                    AnalyticsType.ALERT_COUNT,
                    "system",
                    "total",
                    "count",
                    0.0, // 实际值由AlertRecordService统计
                    null
            );

            log.debug("Collected alert stats");
        } catch (Exception e) {
            log.error("Failed to collect alert stats", e);
        }
    }
}
