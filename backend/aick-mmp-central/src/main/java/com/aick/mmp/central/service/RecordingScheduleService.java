package com.aick.mmp.central.service;

import com.aick.mmp.central.entity.RecordingSchedule;

import java.util.List;
import java.util.Optional;

/**
 * 录像计划服务接口
 */
public interface RecordingScheduleService {

    /**
     * 创建录像计划
     */
    RecordingSchedule createSchedule(RecordingSchedule schedule);

    /**
     * 更新录像计划
     */
    RecordingSchedule updateSchedule(Long id, RecordingSchedule schedule);

    /**
     * 删除录像计划
     */
    void deleteSchedule(Long id);

    /**
     * 根据ID获取录像计划
     */
    Optional<RecordingSchedule> getSchedule(Long id);

    /**
     * 获取所有录像计划
     */
    List<RecordingSchedule> getAllSchedules();

    /**
     * 根据摄像头ID获取录像计划
     */
    List<RecordingSchedule> getSchedulesByCamera(Long cameraId);

    /**
     * 根据摄像头ID获取已启用的录像计划
     */
    List<RecordingSchedule> getEnabledSchedulesByCamera(Long cameraId);

    /**
     * 启用/禁用录像计划
     */
    RecordingSchedule setEnabled(Long id, Boolean enabled);

    /**
     * 获取所有已启用的录像计划（供边缘节点同步使用）
     */
    List<RecordingSchedule> getAllEnabledSchedules();

    /**
     * 批量创建录像计划
     */
    List<RecordingSchedule> createSchedulesBatch(List<RecordingSchedule> schedules);

    /**
     * 根据录像类型获取录像计划
     */
    List<RecordingSchedule> getSchedulesByType(RecordingSchedule.ScheduleType scheduleType);
}
