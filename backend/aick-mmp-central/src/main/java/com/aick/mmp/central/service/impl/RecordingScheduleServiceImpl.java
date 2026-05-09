package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.entity.RecordingSchedule;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RecordingScheduleRepository;
import com.aick.mmp.central.service.RecordingScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 录像计划服务实现类
 */
@Service
@Transactional
public class RecordingScheduleServiceImpl implements RecordingScheduleService {

    private static final Logger log = LoggerFactory.getLogger(RecordingScheduleServiceImpl.class);

    @Autowired
    private RecordingScheduleRepository recordingScheduleRepository;

    @Autowired
    private CameraRepository cameraRepository;

    @Override
    public RecordingSchedule createSchedule(RecordingSchedule schedule) {
        log.info("创建录像计划: {}", schedule.getName());
        
        // 验证摄像头是否存在
        if (!cameraRepository.existsById(schedule.getCameraId())) {
            throw new IllegalArgumentException("摄像头不存在: " + schedule.getCameraId());
        }
        
        // 设置默认值
        if (schedule.getRetentionDays() == null) {
            schedule.setRetentionDays(30);
        }
        if (schedule.getMotionSensitivity() == null) {
            schedule.setMotionSensitivity(50);
        }
        if (schedule.getEnabled() == null) {
            schedule.setEnabled(true);
        }
        
        return recordingScheduleRepository.save(schedule);
    }

    @Override
    public RecordingSchedule updateSchedule(Long id, RecordingSchedule schedule) {
        log.info("更新录像计划 ID: {}", id);
        
        RecordingSchedule existing = recordingScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("录像计划不存在: " + id));
        
        // 验证摄像头是否存在（如果更改了摄像头）
        if (schedule.getCameraId() != null && 
            !schedule.getCameraId().equals(existing.getCameraId())) {
            if (!cameraRepository.existsById(schedule.getCameraId())) {
                throw new IllegalArgumentException("摄像头不存在: " + schedule.getCameraId());
            }
        }
        
        // 更新字段
        if (schedule.getName() != null) {
            existing.setName(schedule.getName());
        }
        if (schedule.getCameraId() != null) {
            existing.setCameraId(schedule.getCameraId());
        }
        if (schedule.getScheduleType() != null) {
            existing.setScheduleType(schedule.getScheduleType());
        }
        if (schedule.getEnabled() != null) {
            existing.setEnabled(schedule.getEnabled());
        }
        if (schedule.getTimeSlots() != null) {
            existing.setTimeSlots(schedule.getTimeSlots());
        }
        if (schedule.getRecordingDays() != null) {
            existing.setRecordingDays(schedule.getRecordingDays());
        }
        if (schedule.getMotionSensitivity() != null) {
            existing.setMotionSensitivity(schedule.getMotionSensitivity());
        }
        if (schedule.getRetentionDays() != null) {
            existing.setRetentionDays(schedule.getRetentionDays());
        }
        if (schedule.getDescription() != null) {
            existing.setDescription(schedule.getDescription());
        }
        
        return recordingScheduleRepository.save(existing);
    }

    @Override
    public void deleteSchedule(Long id) {
        log.info("删除录像计划 ID: {}", id);
        
        if (!recordingScheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("录像计划不存在: " + id);
        }
        
        recordingScheduleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecordingSchedule> getSchedule(Long id) {
        return recordingScheduleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingSchedule> getAllSchedules() {
        return recordingScheduleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingSchedule> getSchedulesByCamera(Long cameraId) {
        return recordingScheduleRepository.findByCameraId(cameraId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingSchedule> getEnabledSchedulesByCamera(Long cameraId) {
        return recordingScheduleRepository.findByCameraIdAndEnabled(cameraId, true);
    }

    @Override
    public RecordingSchedule setEnabled(Long id, Boolean enabled) {
        log.info("设置录像计划 {} 启用状态为: {}", id, enabled);
        
        RecordingSchedule schedule = recordingScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("录像计划不存在: " + id));
        
        schedule.setEnabled(enabled);
        return recordingScheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingSchedule> getAllEnabledSchedules() {
        return recordingScheduleRepository.findByEnabled(true);
    }

    @Override
    public List<RecordingSchedule> createSchedulesBatch(List<RecordingSchedule> schedules) {
        log.info("批量创建录像计划，数量: {}", schedules.size());
        
        List<RecordingSchedule> saved = new ArrayList<>();
        for (RecordingSchedule schedule : schedules) {
            saved.add(createSchedule(schedule));
        }
        
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingSchedule> getSchedulesByType(RecordingSchedule.ScheduleType scheduleType) {
        return recordingScheduleRepository.findByEnabled(true).stream()
                .filter(s -> s.getScheduleType() == scheduleType)
                .toList();
    }
}
