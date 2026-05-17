package com.aick.mmp.shared.model;

import com.aick.mmp.shared.converter.CameraPasswordEncryptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Entity
@Table(name = "cameras")
@Where(clause = "is_deleted = false")
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

    @Column(nullable = true)
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

    @Column(name = "password", length = 1024)
    @Size(max = 300, message = "Password cannot exceed 300 characters")
    @Convert(converter = CameraPasswordEncryptor.class)
    private String password;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "frame_rate")
    private Integer frameRate;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Column(name = "current_bitrate")
    private Integer currentBitrate;  // 当前实际码率

    @Column(name = "current_fps")
    private Double currentFps;  // 当前实际帧率

    @Column(name = "last_error_code")
    private String lastErrorCode;  // 最后错误代码

    @Column(name = "last_error_message")
    private String lastErrorMessage;  // 最后错误消息

    @Column(name = "last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;  // 最后心跳时间

    /**
     * Video compression format (e.g., H.264, H.265)
     */
    @Column(name = "compression")
    private String compression;

    /**
     * Whether audio is enabled for this camera
     */
    @Column(name = "audio_enabled")
    private Boolean audioEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraStatus status;

    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;

    @Builder.Default
    @Column(name = "is_enabled")
    private boolean enabled = true;

    @Column(name = "region_id")
    private Long regionId;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Protocol {
        RTSP, ONVIF, GB28181, HTTP, RTMP
    }

    public enum CameraStatus {
        ONLINE, OFFLINE, CONNECTING, ERROR, MAINTENANCE, PENDING_ALLOCATION
    }
}