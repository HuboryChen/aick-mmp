package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 存储统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatsDTO {

    /**
     * 总存储容量(GB)
     */
    private Double totalCapacity;

    /**
     * 已使用存储(GB)
     */
    private Double usedStorage;

    /**
     * 可用存储(GB)
     */
    private Double availableStorage;

    /**
     * 使用率(%)
     */
    private Double usageRate;

    /**
     * 录像存储统计
     */
    private RecordingStorage recordingStorage;

    /**
     * 存储趋势
     */
    private List<StorageTrend> trends;

    /**
     * 录像分布
     */
    private List<RecordingDistribution> distributions;

    /**
     * 录像存储统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordingStorage {
        private Long totalRecordingCount;
        private Double totalRecordingSize; // GB
        private Double averageRecordingSize; // GB
        private Long totalRecordingDuration; // 秒
        private Long averageRecordingDuration; // 秒
        private Integer retentionDays;
    }

    /**
     * 存储趋势
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageTrend {
        private LocalDateTime timestamp;
        private Double usedStorage;
        private Double availableStorage;
        private Double usageRate;
        private Double growthRate;
    }

    /**
     * 录像分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordingDistribution {
        private String category; // 按摄像头/按日期/按区域
        private String key;
        private String name;
        private Long recordingCount;
        private Double size; // GB
        private Double percentage;
    }
}
