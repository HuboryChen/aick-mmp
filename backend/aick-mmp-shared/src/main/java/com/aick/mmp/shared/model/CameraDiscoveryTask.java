package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camera_discovery_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraDiscoveryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "network_segment", nullable = false, length = 50)
    private String networkSegment;

    @Column(nullable = false, length = 20)
    private String status;

    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "total_ips")
    @Builder.Default
    private Integer totalIps = 0;

    @Column(name = "found_devices", columnDefinition = "JSON")
    private String foundDevices;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (progress == null) progress = 0;
        if (totalIps == null) totalIps = 0;
    }
}
