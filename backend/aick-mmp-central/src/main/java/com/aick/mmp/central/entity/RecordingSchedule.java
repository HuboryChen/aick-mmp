package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 录像计划实体类
 * 用于配置摄像头的录像计划，包括录像类型、时间段、录像日期等
 */
@Entity
@Table(name = "recording_schedules", indexes = {
    @Index(name = "idx_recording_schedule_camera", columnList = "camera_id"),
    @Index(name = "idx_recording_schedule_enabled", columnList = "enabled")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 计划名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 摄像头ID
     */
    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    /**
     * 录像类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    /**
     * 是否启用
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 录像时间段列表
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recording_schedule_time_slots", 
                     joinColumns = @JoinColumn(name = "schedule_id"))
    @OrderColumn(name = "slot_order")
    @Builder.Default
    private List<TimeSlot> timeSlots = new ArrayList<>();

    /**
     * 录像日期（星期几）
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recording_schedule_days", 
                     joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    @Builder.Default
    private Set<DayOfWeek> recordingDays = new HashSet<>();

    /**
     * 移动侦测灵敏度 (0-100)
     */
    @Column(name = "motion_sensitivity")
    @Builder.Default
    private Integer motionSensitivity = 50;

    /**
     * 录像保留天数
     */
    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30;

    /**
     * 备注说明
     */
    @Column(length = 500)
    private String description;

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
     * 录像类型枚举
     */
    public enum ScheduleType {
        /**
         * 持续录像 - 24小时不间断录像
         */
        CONTINUOUS,
        
        /**
         * 定时录像 - 按设定时间段录像
         */
        TIMED,
        
        /**
         * 移动侦测 - 检测到移动时录像
         */
        MOTION,
        
        /**
         * 事件录像 - 特定事件触发录像
         */
        EVENT,
        
        /**
         * 智能录像 - AI智能分析录像
         */
        SMART
    }
}
