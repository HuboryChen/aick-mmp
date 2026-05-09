package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备利用率统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceUsageStatsDTO {

    /**
     * 总体在线率
     */
    private Double onlineRate;

    /**
     * 总体离线率
     */
    private Double offlineRate;

    /**
     * 故障率
     */
    private Double failureRate;

    /**
     * 平均无故障时间(MTBF) - 小时
     */
    private Double mtbf;

    /**
     * 平均修复时间(MTTR) - 分钟
     */
    private Double mttr;

    /**
     * 设备统计
     */
    private DeviceStats stats;

    /**
     * 趋势数据(按时间)
     */
    private List<TrendData> trends;

    /**
     * 设备详情列表
     */
    private List<DeviceDetail> deviceDetails;

    /**
     * 设备统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStats {
        private Long total;
        private Long online;
        private Long offline;
        private Long abnormal;
        private Long maintenance;
    }

    /**
     * 趋势数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private LocalDateTime timestamp;
        private Double onlineRate;
        private Double offlineRate;
        private Double failureRate;
    }

    /**
     * 设备详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceDetail {
        private Long cameraId;
        private String cameraName;
        private String status;
        private LocalDateTime lastOnlineTime;
        private LocalDateTime lastOfflineTime;
        private Long totalOnlineDuration; // 秒
        private Long totalOfflineDuration; // 秒
        private Integer failureCount;
    }
}
