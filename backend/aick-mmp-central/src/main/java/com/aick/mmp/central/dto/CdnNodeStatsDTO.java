package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDN节点统计信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeStatsDTO {
    
    /**
     * 节点基本信息
     */
    private CdnNodeDTO node;
    
    /**
     * 负载百分比
     */
    private Double loadPercentage;
    
    /**
     * CPU使用率
     */
    private Double cpuUsage;
    
    /**
     * 内存使用率
     */
    private Double memoryUsage;
    
    /**
     * 带宽使用率
     */
    private Double bandwidthUsage;
    
    /**
     * 存储使用率
     */
    private Double storageUsage;
    
    /**
     * 在线时长（秒）
     */
    private Long uptimeSeconds;
    
    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
    
    /**
     * 健康状态
     */
    private String healthStatus;
    
    /**
     * 建议
     */
    private String suggestion;
    
    /**
     * 历史负载数据
     */
    private List<CdnNodeLoadDTO> recentLoads;
    
    /**
     * 统计周期内的平均负载
     */
    private Double avgLoad;
    
    /**
     * 统计周期内的峰值负载
     */
    private Integer maxLoad;
    
    /**
     * 统计周期开始时间
     */
    private LocalDateTime statsStartTime;
    
    /**
     * 统计周期结束时间
     */
    private LocalDateTime statsEndTime;
}
