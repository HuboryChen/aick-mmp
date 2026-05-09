package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警记录实体类
 * 用于存储触发的告警记录，包括告警详情、处理状态等
 */
@Entity
@Table(name = "alert_records", indexes = {
    @Index(name = "idx_alert_rule_id", columnList = "rule_id"),
    @Index(name = "idx_alert_level", columnList = "level"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_time", columnList = "alert_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的告警规则ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 关联的告警规则名称（冗余存储便于查询）
     */
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    /**
     * 告警类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertRule.AlertType alertType;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertRule.AlertLevel level;

    /**
     * 告警标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 告警详情/消息
     */
    @Column(length = 1000)
    private String message;

    /**
     * 告警发生时间
     */
    @CreationTimestamp
    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    /**
     * 告警状态
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.UNRESOLVED;

    /**
     * 监控目标类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private AlertRule.TargetType targetType;

    /**
     * 监控目标ID
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 监控目标名称（冗余存储）
     */
    @Column(name = "target_name")
    private String targetName;

    /**
     * 告警时采集的实际值
     */
    @Column(name = "actual_value")
    private Double actualValue;

    /**
     * 告警阈值（冗余存储）
     */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /**
     * 边缘节点ID（如果涉及）
     */
    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    /**
     * 摄像头ID（如果涉及）
     */
    @Column(name = "camera_id")
    private Long cameraId;

    /**
     * 摄像头名称（如果涉及）
     */
    @Column(name = "camera_name")
    private String cameraName;

    /**
     * 地区ID
     */
    @Column(name = "region_id")
    private Long regionId;

    /**
     * 告警来源（如 JANUS, CAMERA, SYSTEM）
     */
    @Column(name = "source", length = 100)
    private String source;

    /**
     * 额外数据（JSON格式，用于存储告警相关的扩展信息）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /**
     * 处理人ID
     */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    /**
     * 处理人用户名
     */
    @Column(name = "resolved_by_username")
    private String resolvedByUsername;

    /**
     * 处理时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 处理备注
     */
    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    /**
     * 告警持续时间（秒）
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * 是否已发送通知
     */
    @Builder.Default
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    /**
     * 通知发送时间
     */
    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    /**
     * 告警确认时间
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /**
     * 告警确认人
     */
    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    /**
     * 告警确认人用户名
     */
    @Column(name = "acknowledged_by_username")
    private String acknowledgedByUsername;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 告警状态枚举
     */
    public enum AlertStatus {
        UNRESOLVED,      // 未处理
        ACKNOWLEDGED,    // 已确认
        RESOLVED,        // 已解决
        IGNORED,         // 已忽略
        AUTO_RESOLVED    // 自动解决
    }
}
