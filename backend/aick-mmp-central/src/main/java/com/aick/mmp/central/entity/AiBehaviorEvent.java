package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_behavior_events")
public class AiBehaviorEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "position_data", columnDefinition = "JSON")
    private String positionData;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "status", length = 20)
    private String status = "UNRESOLVED";

    @Column(name = "alert_record_id")
    private Long alertRecordId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getPositionData() { return positionData; }
    public void setPositionData(String positionData) { this.positionData = positionData; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAlertRecordId() { return alertRecordId; }
    public void setAlertRecordId(Long alertRecordId) { this.alertRecordId = alertRecordId; }
}
