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
 * 告警规则模板实体
 * 用于预设常用的告警规则模板，方便快速创建规则
 */
@Entity
@Table(name = "alert_rule_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模板名称
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * 模板描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 模板分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    @Builder.Default
    private TemplateCategory category = TemplateCategory.SYSTEM;

    /**
     * 告警类型（将应用到模板的AlertRule）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertRule.AlertType alertType;

    /**
     * 默认告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_level")
    @Builder.Default
    private AlertRule.AlertLevel defaultLevel = AlertRule.AlertLevel.WARNING;

    /**
     * 默认监控目标类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_target_type")
    private AlertRule.TargetType defaultTargetType;

    /**
     * 默认警告阈值
     */
    @Column(name = "default_warning_threshold")
    private Double defaultWarningThreshold;

    /**
     * 默认严重阈值
     */
    @Column(name = "default_critical_threshold")
    private Double defaultCriticalThreshold;

    /**
     * 默认持续时间（秒）
     */
    @Column(name = "default_duration_seconds")
    @Builder.Default
    private Integer defaultDurationSeconds = 300;

    /**
     * 默认冷却时间（秒）
     */
    @Column(name = "default_cooldown_seconds")
    @Builder.Default
    private Integer defaultCooldownSeconds = 600;

    /**
     * 默认通知方式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_notification_method")
    @Builder.Default
    private AlertRule.NotificationMethod defaultNotificationMethod = AlertRule.NotificationMethod.IN_APP;

    /**
     * 默认通知目标
     */
    @Column(name = "default_notification_target", length = 500)
    private String defaultNotificationTarget;

    /**
     * 预设条件（JSON格式）
     */
    @Column(name = "preset_conditions", columnDefinition = "TEXT")
    private String presetConditions;

    /**
     * 预设通知配置（JSON格式）
     */
    @Column(name = "preset_notifications", columnDefinition = "TEXT")
    private String presetNotifications;

    /**
     * 预设升级配置（JSON格式）
     */
    @Column(name = "preset_escalations", columnDefinition = "TEXT")
    private String presetEscalations;

    /**
     * 推荐阈值说明
     */
    @Column(name = "threshold_guide", columnDefinition = "TEXT")
    private String thresholdGuide;

    /**
     * 使用说明
     */
    @Column(name = "usage_guide", columnDefinition = "TEXT")
    private String usageGuide;

    /**
     * 模板标签（逗号分隔）
     */
    @Column(name = "tags", length = 255)
    private String tags;

    /**
     * 是否为系统内置模板
     */
    @Builder.Default
    @Column(name = "is_system")
    private Boolean isSystem = false;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /**
     * 使用次数
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * 创建人ID（系统模板为空）
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 模板分类枚举
     */
    public enum TemplateCategory {
        SYSTEM,         // 系统监控
        NETWORK,        // 网络监控
        STORAGE,        // 存储监控
        CAMERA,         // 摄像头监控
        STREAM,         // 流媒体监控
        SECURITY,       // 安全监控
        CUSTOM          // 自定义
    }
}
