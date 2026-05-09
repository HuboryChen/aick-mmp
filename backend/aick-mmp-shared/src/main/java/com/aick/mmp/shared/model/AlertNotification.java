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
 * 告警通知配置实体
 * 用于配置告警的通知渠道和通知规则
 */
@Entity
@Table(name = "alert_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的告警规则ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 关联的告警记录ID（可选，用于追踪通知历史）
     */
    @Column(name = "alert_record_id")
    private Long alertRecordId;

    /**
     * 通知渠道类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    /**
     * 通知目标地址
     */
    @Column(name = "target", length = 500)
    private String target;

    /**
     * 通知标题模板
     */
    @Column(name = "title_template", length = 255)
    private String titleTemplate;

    /**
     * 通知内容模板
     */
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    /**
     * 通知优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retry")
    @Builder.Default
    private Integer maxRetry = 3;

    /**
     * 重试间隔（秒）
     */
    @Column(name = "retry_interval")
    @Builder.Default
    private Integer retryInterval = 60;

    /**
     * 通知超时时间（秒）
     */
    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /**
     * 告警级别过滤（只发送指定级别的告警）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "level_filter")
    private AlertRule.AlertLevel levelFilter;

    /**
     * 通知发送状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * 最后发送时间
     */
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    /**
     * 最后发送状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_result")
    private SendResult lastResult;

    /**
     * 最后错误信息
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * 累计发送次数
     */
    @Column(name = "send_count")
    @Builder.Default
    private Integer sendCount = 0;

    /**
     * 额外配置（JSON格式，如邮件SMTP配置、webhook headers等）
     */
    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    /**
     * 是否启用升级
     */
    @Builder.Default
    @Column(name = "escalation_enabled")
    private Boolean escalationEnabled = false;

    /**
     * 升级延迟时间（分钟）
     */
    @Column(name = "escalation_delay_minutes")
    @Builder.Default
    private Integer escalationDelayMinutes = 30;

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
     * 通知渠道类型枚举
     */
    public enum ChannelType {
        IN_APP,         // 应用内通知
        EMAIL,          // 邮件通知
        SMS,            // 短信通知
        WEBHOOK,        // Webhook通知
        DINGTALK,       // 钉钉通知
        WECHAT,         // 企业微信通知
        FEISHU,         // 飞书通知
        PAGERDUTY,      // PagerDuty集成
        SLACK           // Slack通知
    }

    /**
     * 通知优先级枚举
     */
    public enum NotificationPriority {
        LOW,        // 低优先级
        NORMAL,     // 普通优先级
        HIGH,       // 高优先级
        URGENT      // 紧急优先级
    }

    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        PENDING,        // 待发送
        SENDING,        // 发送中
        SENT,           // 已发送
        FAILED,         // 发送失败
        RETRYING        // 重试中
    }

    /**
     * 发送结果枚举
     */
    public enum SendResult {
        SUCCESS,        // 成功
        FAILED,         // 失败
        TIMEOUT,        // 超时
        INVALID_TARGET, // 无效目标
        RATE_LIMITED    // 频率限制
    }
}
