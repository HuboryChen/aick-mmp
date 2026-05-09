package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.aick.mmp.shared.converter.MapToStringConverter;

@Entity
@Table(name = "edge_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Detailed address (street address, etc.)
     * 详细地址（如街道门牌号等）
     */
    @Column(nullable = false)
    private String location;

    /**
     * Associated region for hierarchical management
     * 关联区域，用于层级管理
     */
    @Column(name = "region_id")
    private Long regionId;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private Integer port;

    /**
     * Associated system app for authentication
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id")
    private SystemApp systemApp;

    /**
     * Registration timestamp
     */
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    @Column(name = "last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "storage_usage")
    private Double storageUsage;

    @Column(name = "max_camera_support")
    private Integer maxCameraSupport;

    @Column(name = "current_camera_count")
    private Integer currentCameraCount;

    @Column(name = "software_version")
    private String softwareVersion;

    @Column(name = "hardware_info")
    private String hardwareInfo;

    @Column(name = "network_bandwidth")
    private String networkBandwidth;

    @Builder.Default
    @Column(name = "system_metrics", columnDefinition = "JSON")
    @Convert(converter = MapToStringConverter.class)
    private Map<String, Object> systemMetrics = new HashMap<>();

    @Builder.Default
    @Column(name = "is_enabled")
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum NodeStatus {
        ONLINE, OFFLINE, CONNECTING, ERROR, MAINTENANCE, UPGRADING
    }
}