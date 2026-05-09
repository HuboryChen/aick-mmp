package com.aick.mmp.central.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 摄像头统计聚合DTO
 */
@Data
@Builder
public class CameraStatisticsDTO {

    /**
     * 摄像头总数
     */
    private long total;

    /**
     * 按状态分布统计
     */
    private Map<String, Long> byStatus;

    /**
     * 按边缘节点统计
     */
    private List<NodeStatistic> byEdgeNode;

    /**
     * 录像相关统计
     */
    private RecordingStatistics recordingStatistics;

    /**
     * 统计缓存时间
     */
    private LocalDateTime cachedAt;

    /**
     * 统计节点信息
     */
    @Data
    @Builder
    public static class NodeStatistic {
        private Long edgeNodeId;
        private String edgeNodeName;
        private long cameraCount;
        private long onlineCount;
    }

    /**
     * 录像统计信息
     */
    @Data
    @Builder
    public static class RecordingStatistics {
        private long totalRecordings;
        private long orphanedRecordings;
        private long deletedRecordings;
        private long totalStorageSize;
    }
}
