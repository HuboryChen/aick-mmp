package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CDN节点实体
 * 用于管理和分发视频内容的CDN节点
 */
@Entity
@Table(name = "cdn_nodes")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * CDN节点唯一标识符
     */
    @Column(name = "node_id", unique = true)
    private String nodeId;

    /**
     * 节点名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * IP地址
     */
    @Column(nullable = false)
    private String ipAddress;

    /**
     * 端口
     */
    @Column(nullable = false)
    private Integer port;

    /**
     * 节点状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    /**
     * 地理位置描述（详细地址）
     */
    private String location;

    /**
     * 关联区域ID，用于层级管理
     */
    @Column(name = "region_id")
    private Long regionId;

    /**
     * 节点容量（最大负载）
     */
    @Column(name = "capacity")
    private Integer capacity;

    /**
     * 当前负载
     */
    @Column(name = "current_load")
    private Integer currentLoad;

    /**
     * 最后心跳时间
     */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    /**
     * CPU使用率（百分比）
     */
    @Column(name = "cpu_usage")
    private Double cpuUsage;

    /**
     * 内存使用率（百分比）
     */
    @Column(name = "memory_usage")
    private Double memoryUsage;

    /**
     * 带宽使用率（百分比）
     */
    @Column(name = "bandwidth_usage")
    private Double bandwidthUsage;

    /**
     * 存储使用率（百分比）
     */
    @Column(name = "storage_usage")
    private Double storageUsage;

    /**
     * 上行带宽（Mbps）
     */
    @Column(name = "up_bandwidth")
    private Integer upBandwidth;

    /**
     * 下行带宽（Mbps）
     */
    @Column(name = "down_bandwidth")
    private Integer downBandwidth;

    /**
     * 节点权重（用于负载均衡）
     */
    @Column(name = "weight")
    @Builder.Default
    private Integer weight = 100;

    /**
     * 优先级（数字越小优先级越高）
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    /**
     * 健康检查URL
     */
    @Column(name = "health_check_url")
    private String healthCheckUrl;

    /**
     * 连接超时时间（毫秒）
     */
    @Column(name = "connect_timeout")
    @Builder.Default
    private Integer connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    @Column(name = "read_timeout")
    @Builder.Default
    private Integer readTimeout = 10000;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /**
     * 是否软删除
     */
    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * CDN节点状态枚举
     */
    public enum NodeStatus {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 维护中 */
        MAINTENANCE,
        /** 降级（部分功能可用） */
        DEGRADED,
        /** 升级中 */
        UPGRADING
    }

    /**
     * 计算负载百分比
     */
    public double getLoadPercentage() {
        if (capacity == null || capacity == 0) {
            return 0.0;
        }
        return (currentLoad != null ? currentLoad : 0) * 100.0 / capacity;
    }

    /**
     * 计算总带宽使用率（考虑上下行）
     */
    public double getTotalBandwidthUsage() {
        if (upBandwidth == null || downBandwidth == null || 
            upBandwidth == 0 || downBandwidth == 0) {
            return 0.0;
        }
        return (bandwidthUsage != null ? bandwidthUsage : 0);
    }

    /**
     * 检查节点是否健康
     */
    public boolean isHealthy() {
        return status == NodeStatus.ONLINE && 
               isEnabled != null && isEnabled &&
               getLoadPercentage() < 90.0;
    }

    /**
     * 计算WLC权重分数
     * 公式: weight * (capacity - currentLoad) / capacity
     */
    public double calculateWlcScore() {
        if (capacity == null || capacity == 0) {
            return 0.0;
        }
        int available = capacity - (currentLoad != null ? currentLoad : 0);
        return weight * available / capacity;
    }
}
