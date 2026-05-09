package com.aick.mmp.shared.model;

import com.aick.mmp.shared.model.enums.ReportFormat;
import com.aick.mmp.shared.model.enums.ReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 报表订阅实体
 */
@Entity
@Table(name = "report_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订阅名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 报表类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    /**
     * 报表格式
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFormat format;

    /**
     * 订阅的统计维度(JSON数组) - 如["device_usage", "bandwidth"]
     */
    @Column(columnDefinition = "TEXT")
    private String dimensions;

    /**
     * 订阅的过滤条件(JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String filters;

    /**
     * 接收邮箱列表(JSON数组)
     */
    @Column(columnDefinition = "TEXT")
    private String recipients;

    /**
     * 下次发送时间
     */
    @Column(name = "next_send_time")
    private LocalDateTime nextSendTime;

    /**
     * 上次发送时间
     */
    @Column(name = "last_send_time")
    private LocalDateTime lastSendTime;

    /**
     * 启用状态
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 创建人ID
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
}
