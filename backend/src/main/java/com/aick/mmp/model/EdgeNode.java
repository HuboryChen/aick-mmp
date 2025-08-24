package com.aick.mmp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.aick.mmp.converter.MapToStringConverter;

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

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private Integer port;

    @Column(name = "auth_username")
    private String authUsername;

    @Column(name = "auth_password")
    private String authPassword;

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

    @Column(name = "system_metrics", columnDefinition = "JSON")
    @Convert(converter = MapToStringConverter.class)
    private Map<String, Object> systemMetrics = new HashMap<>();

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