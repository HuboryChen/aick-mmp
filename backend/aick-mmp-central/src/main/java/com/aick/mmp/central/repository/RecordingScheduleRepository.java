package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.RecordingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 录像计划数据访问接口
 */
@Repository
public interface RecordingScheduleRepository extends JpaRepository<RecordingSchedule, Long> {

    /**
     * 根据摄像头ID查询录像计划
     */
    List<RecordingSchedule> findByCameraId(Long cameraId);

    /**
     * 根据摄像头ID查询已启用的录像计划
     */
    List<RecordingSchedule> findByCameraIdAndEnabled(Long cameraId, Boolean enabled);

    /**
     * 查询所有已启用的录像计划
     */
    List<RecordingSchedule> findByEnabled(Boolean enabled);

    /**
     * 根据摄像头ID和录像类型查询
     */
    List<RecordingSchedule> findByCameraIdAndScheduleType(Long cameraId, RecordingSchedule.ScheduleType scheduleType);

    /**
     * 统计指定摄像头的录像计划数量
     */
    long countByCameraId(Long cameraId);

    /**
     * 检查摄像头是否已有定时录像计划
     */
    @Query("SELECT COUNT(s) > 0 FROM RecordingSchedule s WHERE s.cameraId = :cameraId AND s.scheduleType = :scheduleType")
    boolean existsByCameraIdAndScheduleType(@Param("cameraId") Long cameraId, 
                                             @Param("scheduleType") RecordingSchedule.ScheduleType scheduleType);
}
