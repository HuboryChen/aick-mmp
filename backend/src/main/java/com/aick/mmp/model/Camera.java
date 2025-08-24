package com.aick.mmp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cameras")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Camera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Protocol protocol;

    @Column(name = "connection_url", nullable = false)
    private String connectionUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "frame_rate")
    private Integer frameRate;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraStatus status;

    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;

    @Column(name = "is_enabled")
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Protocol {
        RTSP, ONVIF, GB28181, HTTP
    }

    public enum CameraStatus {
        ONLINE, OFFLINE, CONNECTING, ERROR, MAINTENANCE
    }
}