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
 * 告警升级配置实体
 * 用于配置告警未处理时的升级机制
 */
@Entity
@Table(name = "alert_escalations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的告警规则ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 关联的告警记录ID
     */
    @Column(name = "alert_record_id")
    private Long alertRecordId;

    /**
     * 升级级别（1, 2, 3...）
     */
    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private Integer escalationLevel = 1;

    /**
     * 升级条件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private EscalationConditionType conditionType;

    /**
     * 条件值（时长分钟数或次数）
     */
    @Column(name = "condition_value")
    @Builder.Default
    private Integer conditionValue = 30;

    /**
     * 升级方式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private EscalationAction actionType;

    /**
     * 升级通知渠道
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type")
    private AlertNotification.ChannelType channelType;

    /**
     * 升级通知目标
     */
    @Column(name = "target", length = 500)
    private String target;

    /**
     * 升级标题模板
     */
    @Column(name = "title_template", length = 255)
    private String titleTemplate;

    /**
     * 升级内容模板
     */
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    /**
     * 升级通知配置（JSON格式）
     */
    @Column(name = "notification_config", columnDefinition = "TEXT")
    private String notificationConfig;

    /**
     * 是否已触发
     */
    @Builder.Default
    @Column(name = "is_triggered")
    private Boolean isTriggered = false;

    /**
     * 触发时间
     */
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    /**
     * 升级执行状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private EscalationStatus status = EscalationStatus.PENDING;

    /**
     * 执行结果
     */
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

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
     * 升级条件类型枚举
     */
    public enum EscalationConditionType {
        TIME_UNACKED,      // 未确认时长
        TIME_UNRESOLVED,   // 未解决时长
        REPEAT_COUNT,      // 重复次数
        SEVERITY           // 严重程度升级
    }

    /**
     * 升级动作类型枚举
     */
    public enum EscalationAction {
        NOTIFY,            // 通知
        ASSIGN,            // 分配给其他人
        ESCALATE,          // 继续升级
        AUTO_RESOLVE,      // 自动解决
        CREATE_TICKET,     // 创建工单
        EXECUTE_WEBHOOK    // 执行Webhook
    }

    /**
     * 升级状态枚举
     */
    public enum EscalationStatus {
        PENDING,        // 待执行
        EXECUTING,      // 执行中
        COMPLETED,      // 已完成
        FAILED,         // 执行失败
        SKIPPED         // 跳过
    }
}
