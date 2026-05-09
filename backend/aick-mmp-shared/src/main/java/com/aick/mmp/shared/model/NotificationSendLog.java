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
 * 通知发送日志实体
 * 用于记录每次通知发送的详细历史和重试信息
 */
@Entity
@Table(name = "notification_send_logs", indexes = {
    @Index(name = "idx_notification_alert_id", columnList = "alert_record_id"),
    @Index(name = "idx_notification_channel", columnList = "channel_type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的告警记录ID
     */
    @Column(name = "alert_record_id", nullable = false)
    private Long alertRecordId;

    /**
     * 关联的通知配置ID
     */
    @Column(name = "notification_config_id")
    private Long notificationConfigId;

    /**
     * 通知渠道类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private AlertNotification.ChannelType channelType;

    /**
     * 通知目标（如邮箱地址、手机号等）
     */
    @Column(name = "target", length = 500)
    private String target;

    /**
     * 发送状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SendStatus status = SendStatus.PENDING;

    /**
     * 发送结果消息
     */
    @Column(name = "result_message", length = 500)
    private String resultMessage;

    /**
     * 错误码
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * 错误详情
     */
    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    /**
     * 发送耗时（毫秒）
     */
    @Column(name = "cost_time_ms")
    private Long costTimeMs;

    /**
     * 当前重试次数
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retry")
    @Builder.Default
    private Integer maxRetry = 3;

    /**
     * 是否可重试
     */
    @Column(name = "retryable")
    @Builder.Default
    private Boolean retryable = true;

    /**
     * 下次重试时间
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * 最后发送时间
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 发送优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private AlertNotification.NotificationPriority priority = AlertNotification.NotificationPriority.NORMAL;

    /**
     * 额外配置（JSON格式）
     */
    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

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
     * 发送状态枚举
     */
    public enum SendStatus {
        PENDING,      // 待发送
        SENDING,      // 发送中
        SUCCESS,      // 发送成功
        FAILED,       // 发送失败
        RETRYING,     // 重试中
        CANCELLED     // 已取消
    }

    /**
     * 标记为发送成功
     */
    public void markSuccess(String message, Long costTime) {
        this.status = SendStatus.SUCCESS;
        this.resultMessage = message;
        this.costTimeMs = costTime;
        this.sentAt = LocalDateTime.now();
        this.retryable = false;
    }

    /**
     * 标记为发送失败
     */
    public void markFailed(String errorCode, String errorDetail) {
        this.status = SendStatus.FAILED;
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
        if (this.retryCount >= this.maxRetry) {
            this.retryable = false;
        }
    }

    /**
     * 标记为重试中
     */
    public void markRetrying(int delaySeconds) {
        this.status = SendStatus.RETRYING;
        this.retryCount++;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * 标记为发送中
     */
    public void markSending() {
        this.status = SendStatus.SENDING;
    }
}
