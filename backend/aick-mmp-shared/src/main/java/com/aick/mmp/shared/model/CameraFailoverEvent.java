package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 边缘节点故障转移事件实体
 * 记录每次故障转移操作的完整信息，用于审计和问题排查
 */
@Entity
@Table(name = "camera_failover_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraFailoverEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_edge_node_id", nullable = false)
    private Long sourceEdgeNodeId;

    @ElementCollection
    @CollectionTable(name = "failover_event_target_nodes", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "target_edge_node_id")
    @Builder.Default
    private List<Long> targetEdgeNodeIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "failover_event_cameras", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "camera_id")
    @Builder.Default
    private List<Long> cameraIds = new ArrayList<>();

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private FailoverTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FailoverStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 故障转移触发类型
     */
    public enum FailoverTriggerType {
        AUTO,   // 自动触发（节点离线）
        MANUAL  // 手动触发（管理员操作）
    }

    /**
     * 故障转移状态
     */
    public enum FailoverStatus {
        IN_PROGRESS, // 进行中
        COMPLETED,   // 全部成功
        PARTIAL,     // 部分失败
        FAILED       // 全部失败
    }
}
