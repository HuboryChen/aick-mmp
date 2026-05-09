package com.aick.mmp.edge.service;

import com.aick.mmp.edge.dto.MotionEventReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 移动侦测服务接口
 * 定义移动侦测功能的核心行为
 */
public interface MotionDetectionService {

    /**
     * 处理视频帧，检测移动
     * 
     * @param cameraId 摄像头ID
     * @param frameTimestamp 帧时间戳
     * @param motionLevel 移动级别 (0.0 - 1.0)
     * @param motionRegions 检测到移动的区域
     */
    void processFrame(Long cameraId, LocalDateTime frameTimestamp, double motionLevel, List<String> motionRegions);

    /**
     * 手动触发移动事件结束
     * 
     * @param cameraId 摄像头ID
     */
    void triggerMotionEnd(Long cameraId);

    /**
     * 获取待上报的事件列表
     * 
     * @return 待上报的移动事件列表
     */
    List<MotionEventReport> getPendingEvents();

    /**
     * 清除已上报的事件
     * 
     * @param reportedEvents 已上报的事件列表
     */
    void clearPendingEvents(List<MotionEventReport> reportedEvents);

    /**
     * 获取指定摄像头的活跃移动事件
     * 
     * @param cameraId 摄像头ID
     * @return 活跃的移动事件（如果存在）
     */
    Optional<MotionEventReport> getActiveMotionEvent(Long cameraId);

    /**
     * 检查指定摄像头是否有活跃的移动事件
     * 
     * @param cameraId 摄像头ID
     * @return 是否有活跃事件
     */
    boolean hasActiveMotion(Long cameraId);

    /**
     * 设置移动侦测灵敏度
     * 
     * @param sensitivity 灵敏度 (0.0 - 1.0)
     */
    void setSensitivity(double sensitivity);

    /**
     * 获取当前灵敏度
     * 
     * @return 当前灵敏度
     */
    double getSensitivity();

    /**
     * 设置最小移动持续时间
     * 
     * @param seconds 最小持续秒数
     */
    void setMinMotionDuration(int seconds);

    /**
     * 获取最小移动持续时间
     * 
     * @return 最小持续秒数
     */
    int getMinMotionDuration();
}
