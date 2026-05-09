package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 带宽统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandwidthStatsDTO {

    /**
     * 当前带宽(Mbps)
     */
    private Double currentBandwidth;

    /**
     * 平均带宽(Mbps)
     */
    private Double averageBandwidth;

    /**
     * 峰值带宽(Mbps)
     */
    private Double peakBandwidth;

    /**
     * 带宽使用率(%)
     */
    private Double usageRate;

    /**
     * 总传输量(GB)
     */
    private Double totalTraffic;

    /**
     * 带宽统计数据
     */
    private BandwidthStats stats;

    /**
     * 趋势数据
     */
    private List<BandwidthTrend> trends;

    /**
     * CDN节点带宽分布
     */
    private List<CdnBandwidth> cdnDistribution;

    /**
     * 带宽统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BandwidthStats {
        private Double minBandwidth;
        private Double maxBandwidth;
        private Double medianBandwidth;
        private Double stdDev;
        private Long totalUpstream; // 上行流量(GB)
        private Long totalDownstream; // 下行流量(GB)
    }

    /**
     * 带宽趋势
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BandwidthTrend {
        private LocalDateTime timestamp;
        private Double inbound; // 入带宽
        private Double outbound; // 出带宽
        private Double total;
    }

    /**
     * CDN带宽分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdnBandwidth {
        private String cdnNodeId;
        private String cdnNodeName;
        private Double bandwidth;
        private Double percentage;
    }
}
