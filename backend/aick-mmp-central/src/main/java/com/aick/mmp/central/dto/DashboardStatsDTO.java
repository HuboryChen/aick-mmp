package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // ==================== 基础统计 ====================
    private long totalCameras;
    private long onlineCameras;
    private long totalEdgeNodes;
    private long onlineEdgeNodes;
    private long totalStreams;
    private long activeStreams;
    private long onlineUsers;
    
    // ==================== 区域统计 ====================
    private List<RegionStatsSummaryDTO> regionStats;
    private long totalRegions;
    
    // ==================== CDN节点统计 ====================
    private CdnNodeStatsSummaryDTO cdnNodeStats;
    
    // ==================== 告警统计 ====================
    private AlertStatsSummaryDTO alertStats;
    
    // ==================== 内部类：区域统计摘要 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionStatsSummaryDTO {
        private Long regionId;
        private String regionName;
        private String regionCode;
        private Long totalCameras;
        private Long onlineCameras;
        private Long totalEdges;
        private Long onlineEdges;
    }
    
    // ==================== 内部类：CDN节点统计摘要 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdnNodeStatsSummaryDTO {
        private long totalNodes;
        private long onlineNodes;
        private long offlineNodes;
        private long enabledNodes;
        private double avgLoad;
        private long highLoadNodes;
        private long lowLoadNodes;
    }
    
    // ==================== 内部类：告警统计摘要 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertStatsSummaryDTO {
        private long totalActive;
        private long pending;
        private long acknowledged;
        private long critical;
        private long warning;
        private long error;
        private long info;
        private long resolvedToday;
        private long triggeredToday;
    }
}
