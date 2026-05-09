package com.aick.mmp.central.service;

import com.aick.mmp.central.entity.MotionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 移动侦测事件服务接口
 */
public interface MotionEventService {

    /**
     * 创建移动事件
     */
    MotionEvent createMotionEvent(MotionEvent event);

    /**
     * 批量创建移动事件
     */
    List<MotionEvent> createMotionEventsBatch(List<MotionEvent> events);

    /**
     * 获取移动事件详情
     */
    Optional<MotionEvent> getMotionEvent(Long id);

    /**
     * 查询指定摄像头的移动事件
     */
    Page<MotionEvent> getMotionEventsByCamera(Long cameraId, Pageable pageable);

    /**
     * 查询指定时间范围内的移动事件
     */
    List<MotionEvent> getMotionEventsByTimeRange(Long cameraId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询指定时间范围内的移动事件
     */
    Page<MotionEvent> getMotionEventsByTimeRangePaged(Long cameraId, LocalDateTime startTime, 
                                                       LocalDateTime endTime, Pageable pageable);

    /**
     * 查询触发了录像的移动事件
     */
    Page<MotionEvent> getTriggeredRecordingEvents(Pageable pageable);

    /**
     * 统计指定摄像头的移动事件数量
     */
    long countByCamera(Long cameraId);

    /**
     * 统计指定时间范围内的移动事件数量
     */
    long countByCameraAndTimeRange(Long cameraId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除指定时间之前的旧事件
     */
    int cleanupOldEvents(int daysOld);

    /**
     * 关联录像到移动事件
     */
    MotionEvent linkRecording(Long eventId, Long recordingId);
}
