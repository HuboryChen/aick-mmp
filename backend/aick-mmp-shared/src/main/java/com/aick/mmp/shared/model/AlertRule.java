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
 * 告警规则实体类
 * 用于配置系统监控的告警规则，包括阈值、告警级别、通知方式等
 */
@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 规则名称，唯一标识
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 规则描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 告警类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel level;

    /**
     * 监控目标类型（CAMERA/EDGE_NODE/SYSTEM等）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    /**
     * 监控目标ID（如果是针对特定摄像头或节点）
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 阈值表达式（如 "cpu > 80"）
     */
    @Column(name = "threshold_expression", length = 255)
    private String thresholdExpression;

    /**
     * 警告阈值
     */
    @Column(name = "warning_threshold")
    private Double warningThreshold;

    /**
     * 严重阈值
     */
    @Column(name = "critical_threshold")
    private Double criticalThreshold;

    /**
     * 持续时间（秒），指标超过阈值持续多长时间才触发告警
     */
    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 300;

    /**
     * 冷却时间（秒），告警触发后多久内不重复告警
     */
    @Column(name = "cooldown_seconds")
    @Builder.Default
    private Integer cooldownSeconds = 600;

    /**
     * 告警时段（cron表达式），为空表示全天生效
     */
    @Column(name = "alert_schedule", length = 100)
    private String alertSchedule;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    /**
     * 规则状态
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private RuleStatus status = RuleStatus.ENABLED;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 上次触发时间
     */
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    /**
     * 通知方式（IN_APP/EMAIL/SMS/WEBHOOK）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_method")
    @Builder.Default
    private NotificationMethod notificationMethod = NotificationMethod.IN_APP;

    /**
     * 通知目标（如邮箱地址、webhook URL）
     */
    @Column(name = "notification_target", length = 500)
    private String notificationTarget;

    /**
     * 规则创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 规则更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 告警类型枚举
     */
    public enum AlertType {
        CPU_USAGE,           // CPU使用率
        MEMORY_USAGE,        // 内存使用率
        DISK_USAGE,          // 磁盘使用率
        NETWORK_LATENCY,     // 网络延迟
        CAMERA_OFFLINE,      // 摄像头离线
        CAMERA_ERROR,        // 摄像头错误
        EDGE_NODE_OFFLINE,   // 边缘节点离线
        STREAM_INTERRUPTED,  // 视频流中断
        MOTION_DETECTED,     // 移动侦测
        RECORDING_FAILED,    // 录像失败
        SYSTEM_ERROR,        // 系统错误
        CUSTOM               // 自定义规则
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        INFO,       // 信息
        WARNING,     // 警告
        ERROR,      // 错误
        CRITICAL    // 严重
    }

    /**
     * 监控目标类型枚举
     */
    public enum TargetType {
        SYSTEM,         // 系统级别
        EDGE_NODE,      // 边缘节点
        CAMERA,         // 摄像头
        STREAM,         // 视频流
        REGION          // 区域
    }

    /**
     * 规则状态枚举
     */
    public enum RuleStatus {
        ENABLED,    // 启用
        DISABLED,   // 禁用
        PAUSED      // 暂停
    }

    /**
     * 通知方式枚举
     */
    public enum NotificationMethod {
        IN_APP,     // 应用内通知
        EMAIL,      // 邮件通知
        SMS,        // 短信通知
        WEBHOOK,    // Webhook通知
        DINGTALK    // 钉钉通知
    }
}
