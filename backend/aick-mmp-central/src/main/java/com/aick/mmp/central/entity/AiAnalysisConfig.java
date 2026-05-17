package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_config")
public class AiAnalysisConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false, unique = true)
    private Long cameraId;

    @Column(name = "enable_passenger")
    private Boolean enablePassenger = true;

    @Column(name = "enable_behavior")
    private Boolean enableBehavior = true;

    @Column(name = "enable_plate")
    private Boolean enablePlate = true;

    @Column(name = "passenger_frame_rate")
    private Integer passengerFrameRate = 1;

    @Column(name = "behavior_frame_rate")
    private Integer behaviorFrameRate = 2;

    @Column(name = "plate_frame_rate")
    private Integer plateFrameRate = 5;

    @Column(name = "loitering_threshold_seconds")
    private Integer loiteringThresholdSeconds = 30;

    @Column(name = "gathering_min_people")
    private Integer gatheringMinPeople = 5;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public Boolean getEnablePassenger() { return enablePassenger; }
    public void setEnablePassenger(Boolean enablePassenger) { this.enablePassenger = enablePassenger; }
    public Boolean getEnableBehavior() { return enableBehavior; }
    public void setEnableBehavior(Boolean enableBehavior) { this.enableBehavior = enableBehavior; }
    public Boolean getEnablePlate() { return enablePlate; }
    public void setEnablePlate(Boolean enablePlate) { this.enablePlate = enablePlate; }
    public Integer getPassengerFrameRate() { return passengerFrameRate; }
    public void setPassengerFrameRate(Integer passengerFrameRate) { this.passengerFrameRate = passengerFrameRate; }
    public Integer getBehaviorFrameRate() { return behaviorFrameRate; }
    public void setBehaviorFrameRate(Integer behaviorFrameRate) { this.behaviorFrameRate = behaviorFrameRate; }
    public Integer getPlateFrameRate() { return plateFrameRate; }
    public void setPlateFrameRate(Integer plateFrameRate) { this.plateFrameRate = plateFrameRate; }
    public Integer getLoiteringThresholdSeconds() { return loiteringThresholdSeconds; }
    public void setLoiteringThresholdSeconds(Integer loiteringThresholdSeconds) { this.loiteringThresholdSeconds = loiteringThresholdSeconds; }
    public Integer getGatheringMinPeople() { return gatheringMinPeople; }
    public void setGatheringMinPeople(Integer gatheringMinPeople) { this.gatheringMinPeople = gatheringMinPeople; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
