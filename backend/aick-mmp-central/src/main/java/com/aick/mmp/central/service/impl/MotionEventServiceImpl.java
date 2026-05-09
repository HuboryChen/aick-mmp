package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.entity.MotionEvent;
import com.aick.mmp.central.repository.MotionEventRepository;
import com.aick.mmp.central.service.MotionEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 移动侦测事件服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MotionEventServiceImpl implements MotionEventService {

    private final MotionEventRepository motionEventRepository;

    @Override
    public MotionEvent createMotionEvent(MotionEvent event) {
        log.debug("创建移动事件，摄像头ID: {}", event.getCameraId());
        
        // 设置默认值
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        if (event.getEventType() == null) {
            event.setEventType(MotionEvent.EventType.MOTION);
        }
        if (event.getTriggeredRecording() == null) {
            event.setTriggeredRecording(false);
        }
        
        return motionEventRepository.save(event);
    }

    @Override
    public List<MotionEvent> createMotionEventsBatch(List<MotionEvent> events) {
        log.info("批量创建移动事件，数量: {}", events.size());
        return motionEventRepository.saveAll(events);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MotionEvent> getMotionEvent(Long id) {
        return motionEventRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MotionEvent> getMotionEventsByCamera(Long cameraId, Pageable pageable) {
        return motionEventRepository.findByCameraId(cameraId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotionEvent> getMotionEventsByTimeRange(Long cameraId, LocalDateTime startTime, LocalDateTime endTime) {
        return motionEventRepository.findByCameraIdAndTimeRange(cameraId, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MotionEvent> getMotionEventsByTimeRangePaged(Long cameraId, LocalDateTime startTime, 
                                                            LocalDateTime endTime, Pageable pageable) {
        return motionEventRepository.findByCameraIdAndEventTimeBetween(cameraId, startTime, endTime, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MotionEvent> getTriggeredRecordingEvents(Pageable pageable) {
        return motionEventRepository.findByTriggeredRecordingTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCamera(Long cameraId) {
        return motionEventRepository.countByCameraId(cameraId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCameraAndTimeRange(Long cameraId, LocalDateTime startTime, LocalDateTime endTime) {
        return motionEventRepository.countByCameraIdAndTimeRange(cameraId, startTime, endTime);
    }

    @Override
    public int cleanupOldEvents(int daysOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
        List<MotionEvent> oldEvents = motionEventRepository.findAll().stream()
                .filter(e -> e.getEventTime().isBefore(cutoffTime))
                .toList();
        
        int count = oldEvents.size();
        if (count > 0) {
            motionEventRepository.deleteAll(oldEvents);
            log.info("清理了 {} 条超过 {} 天的移动事件", count, daysOld);
        }
        return count;
    }

    @Override
    public MotionEvent linkRecording(Long eventId, Long recordingId) {
        MotionEvent event = motionEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("移动事件不存在: " + eventId));
        
        event.setRecordingId(recordingId);
        event.setTriggeredRecording(true);
        
        return motionEventRepository.save(event);
    }
}
