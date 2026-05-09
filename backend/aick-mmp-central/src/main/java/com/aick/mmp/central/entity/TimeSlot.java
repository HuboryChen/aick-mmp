package com.aick.mmp.central.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 时间段嵌入类
 * 用于定义录像的起止时间段
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    /**
     * 开始时间
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * 录像质量 (HIGH, MEDIUM, LOW)
     */
    @Column(name = "quality", length = 10)
    @Builder.Default
    private String quality = "MEDIUM";
}
