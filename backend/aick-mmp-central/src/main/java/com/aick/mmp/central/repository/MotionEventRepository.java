package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.MotionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 移动侦测事件数据访问接口
 */
@Repository
public interface MotionEventRepository extends JpaRepository<MotionEvent, Long> {

    /**
     * 根据摄像头ID查询移动事件
     */
    Page<MotionEvent> findByCameraId(Long cameraId, Pageable pageable);

    /**
     * 根据摄像头ID查询指定时间范围内的事件
     */
    @Query("SELECT m FROM MotionEvent m WHERE m.cameraId = :cameraId " +
           "AND m.eventTime BETWEEN :startTime AND :endTime " +
           "ORDER BY m.eventTime DESC")
    List<MotionEvent> findByCameraIdAndTimeRange(
            @Param("cameraId") Long cameraId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据摄像头ID查询指定时间范围内的事件（分页）
     */
    Page<MotionEvent> findByCameraIdAndEventTimeBetween(
            Long cameraId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询触发了录像的事件
     */
    Page<MotionEvent> findByTriggeredRecordingTrue(Pageable pageable);

    /**
     * 根据事件类型查询
     */
    List<MotionEvent> findByCameraIdAndEventType(Long cameraId, MotionEvent.EventType eventType);

    /**
     * 统计指定摄像头的事件数量
     */
    long countByCameraId(Long cameraId);

    /**
     * 统计指定摄像头在时间范围内的移动事件数量
     */
    @Query("SELECT COUNT(m) FROM MotionEvent m WHERE m.cameraId = :cameraId " +
           "AND m.eventTime BETWEEN :startTime AND :endTime")
    long countByCameraIdAndTimeRange(
            @Param("cameraId") Long cameraId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的事件（清理旧数据）
     */
    void deleteByEventTimeBefore(LocalDateTime before);
}
