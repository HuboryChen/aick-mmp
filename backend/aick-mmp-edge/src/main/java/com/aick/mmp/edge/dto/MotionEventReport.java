package com.aick.mmp.edge.dto;

import java.time.LocalDateTime;

/**
 * 移动侦测事件上报DTO
 * 用于边缘节点向中心服务器上报移动侦测事件
 */
public class MotionEventReport {

    private Long cameraId;
    private LocalDateTime eventTime;
    private LocalDateTime endTime;
    private String detectionType;
    private Double confidence;
    private String region;
    private String snapshotPath;
    private String videoClipPath;
    private Integer durationSeconds;

    public MotionEventReport() {
    }

    public MotionEventReport(Long cameraId, LocalDateTime eventTime, Double confidence, String region) {
        this.cameraId = cameraId;
        this.eventTime = eventTime;
        this.confidence = confidence;
        this.region = region;
        this.detectionType = "MOTION";
    }

    // Getters and Setters
    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public String getVideoClipPath() {
        return videoClipPath;
    }

    public void setVideoClipPath(String videoClipPath) {
        this.videoClipPath = videoClipPath;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String toString() {
        return "MotionEventReport{" +
                "cameraId=" + cameraId +
                ", eventTime=" + eventTime +
                ", endTime=" + endTime +
                ", detectionType='" + detectionType + '\'' +
                ", confidence=" + confidence +
                ", region='" + region + '\'' +
                '}';
    }
}
