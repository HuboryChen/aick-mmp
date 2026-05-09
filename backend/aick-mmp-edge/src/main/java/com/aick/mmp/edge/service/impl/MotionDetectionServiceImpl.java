package com.aick.mmp.edge.service.impl;

import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.MotionEventReport;
import com.aick.mmp.edge.service.MotionDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 移动侦测事件生成器服务
 * 负责检测摄像头画面中的移动，并生成移动侦测事件
 */
@Service
public class MotionDetectionServiceImpl implements MotionDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(MotionDetectionServiceImpl.class);

    @Autowired
    private EdgeNodeConfig edgeNodeConfig;

    // 移动侦测配置
    private double defaultSensitivity = 0.7;
    private int minMotionDurationSeconds = 10;
    private int preRecordSeconds = 5;
    private int postRecordSeconds = 30;

    // 活跃的移动事件映射 <cameraId, currentEvent>
    private final Map<Long, MotionEventReport> activeMotionEvents = new ConcurrentHashMap<>();

    // 事件计数器
    private final AtomicLong eventCounter = new AtomicLong(0);

    // 已完成待上报的事件
    private final List<MotionEventReport> pendingEvents = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        logger.info("MotionDetectionService initialized with config: sensitivity={}, minDuration={}s, preRecord={}s, postRecord={}s",
                defaultSensitivity, minMotionDurationSeconds, preRecordSeconds, postRecordSeconds);
    }

    /**
     * 处理帧数据，检测移动
     * 这是一个示例实现，实际应用中需要接入视频流分析库
     * 
     * @param cameraId 摄像头ID
     * @param frameTimestamp 帧时间戳
     * @param motionLevel 移动级别 (0.0 - 1.0)
     * @param motionRegions 检测到移动的区域列表
     */
    @Override
    public void processFrame(Long cameraId, LocalDateTime frameTimestamp, double motionLevel, List<String> motionRegions) {
        if (motionLevel < defaultSensitivity) {
            // 移动级别低于阈值，结束当前事件（如果有）
            endMotionEventIfActive(cameraId);
            return;
        }

        if (activeMotionEvents.containsKey(cameraId)) {
            // 更新现有事件
            updateMotionEvent(cameraId, motionLevel, motionRegions);
        } else {
            // 开始新事件
            startMotionEvent(cameraId, frameTimestamp, motionLevel, motionRegions);
        }
    }

    /**
     * 开始新的移动事件
     */
    private void startMotionEvent(Long cameraId, LocalDateTime timestamp, double confidence, List<String> regions) {
        MotionEventReport event = new MotionEventReport();
        event.setCameraId(cameraId);
        event.setEventTime(timestamp.minusSeconds(preRecordSeconds)); // 包含预录时间
        event.setDetectionType("MOTION");
        event.setConfidence(Math.min(1.0, confidence));
        event.setRegion(regions != null && !regions.isEmpty() ? String.join(",", regions) : "center");

        activeMotionEvents.put(cameraId, event);
        logger.debug("Motion event started for camera {}: confidence={}, regions={}", cameraId, confidence, regions);
    }

    /**
     * 更新移动事件
     */
    private void updateMotionEvent(Long cameraId, double confidence, List<String> regions) {
        MotionEventReport event = activeMotionEvents.get(cameraId);
        if (event != null) {
            // 更新置信度为最高值
            if (confidence > event.getConfidence()) {
                event.setConfidence(Math.min(1.0, confidence));
            }
            // 更新区域信息
            if (regions != null && !regions.isEmpty()) {
                event.setRegion(String.join(",", regions));
            }
        }
    }

    /**
     * 结束移动事件（如果有）
     */
    private void endMotionEventIfActive(Long cameraId) {
        MotionEventReport event = activeMotionEvents.remove(cameraId);
        if (event != null) {
            LocalDateTime now = LocalDateTime.now();
            event.setEndTime(now.plusSeconds(postRecordSeconds)); // 包含后录时间

            // 计算持续时间
            long durationSeconds = java.time.Duration.between(event.getEventTime(), event.getEndTime()).getSeconds();
            event.setDurationSeconds((int) durationSeconds);

            // 只有满足最小持续时间才上报
            if (durationSeconds >= minMotionDurationSeconds) {
                pendingEvents.add(event);
                logger.info("Motion event completed for camera {}: duration={}s, confidence={}, eventId={}",
                        cameraId, durationSeconds, event.getConfidence(), eventCounter.incrementAndGet());
            } else {
                logger.debug("Motion event discarded for camera {}: duration {}s < {}s threshold",
                        cameraId, durationSeconds, minMotionDurationSeconds);
            }
        }
    }

    /**
     * 手动触发移动事件结束（用于测试或外部控制）
     */
    @Override
    public void triggerMotionEnd(Long cameraId) {
        endMotionEventIfActive(cameraId);
    }

    /**
     * 获取待上报的事件列表
     */
    @Override
    public List<MotionEventReport> getPendingEvents() {
        return new ArrayList<>(pendingEvents);
    }

    /**
     * 清除已上报的事件
     */
    @Override
    public void clearPendingEvents(List<MotionEventReport> reportedEvents) {
        pendingEvents.removeAll(reportedEvents);
    }

    /**
     * 获取活跃的移动事件
     */
    @Override
    public Optional<MotionEventReport> getActiveMotionEvent(Long cameraId) {
        return Optional.ofNullable(activeMotionEvents.get(cameraId));
    }

    /**
     * 检查是否有活跃的移动事件
     */
    @Override
    public boolean hasActiveMotion(Long cameraId) {
        return activeMotionEvents.containsKey(cameraId);
    }

    /**
     * 配置移动侦测灵敏度
     */
    @Override
    public void setSensitivity(double sensitivity) {
        if (sensitivity < 0.0 || sensitivity > 1.0) {
            throw new IllegalArgumentException("Sensitivity must be between 0.0 and 1.0");
        }
        this.defaultSensitivity = sensitivity;
        logger.info("Motion detection sensitivity updated to {}", sensitivity);
    }

    /**
     * 获取当前灵敏度
     */
    @Override
    public double getSensitivity() {
        return defaultSensitivity;
    }

    /**
     * 配置最小移动持续时间
     */
    @Override
    public void setMinMotionDuration(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
        this.minMotionDurationSeconds = seconds;
    }

    /**
     * 获取最小移动持续时间
     */
    @Override
    public int getMinMotionDuration() {
        return minMotionDurationSeconds;
    }

    /**
     * 定时清理过长时间的事件（防止异常）
     * 如果事件持续超过预定时间，自动结束
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void cleanupLongRunningEvents() {
        LocalDateTime maxDuration = LocalDateTime.now().minusMinutes(30); // 最多30分钟
        List<Long> toRemove = new ArrayList<>();

        for (Map.Entry<Long, MotionEventReport> entry : activeMotionEvents.entrySet()) {
            if (entry.getValue().getEventTime().isBefore(maxDuration)) {
                toRemove.add(entry.getKey());
            }
        }

        for (Long cameraId : toRemove) {
            logger.warn("Cleaning up long-running motion event for camera {}", cameraId);
            endMotionEventIfActive(cameraId);
        }

        if (!toRemove.isEmpty()) {
            logger.info("Cleaned up {} long-running motion events", toRemove.size());
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeEvents", activeMotionEvents.size());
        stats.put("pendingEvents", pendingEvents.size());
        stats.put("totalEventsProcessed", eventCounter.get());
        stats.put("currentSensitivity", defaultSensitivity);
        stats.put("minMotionDurationSeconds", minMotionDurationSeconds);
        return stats;
    }
}
