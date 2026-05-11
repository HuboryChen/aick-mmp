package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_vehicle_records")
public class AiVehicleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "plate_number", nullable = false, length = 50)
    private String plateNumber;

    @Column(name = "plate_color", length = 20)
    private String plateColor;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "is_whitelisted")
    private Boolean isWhitelisted = false;

    @Column(name = "is_blacklisted")
    private Boolean isBlacklisted = false;

    @Column(name = "detect_time", nullable = false)
    private LocalDateTime detectTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getPlateColor() { return plateColor; }
    public void setPlateColor(String plateColor) { this.plateColor = plateColor; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public Boolean getIsWhitelisted() { return isWhitelisted; }
    public void setIsWhitelisted(Boolean isWhitelisted) { this.isWhitelisted = isWhitelisted; }
    public Boolean getIsBlacklisted() { return isBlacklisted; }
    public void setIsBlacklisted(Boolean isBlacklisted) { this.isBlacklisted = isBlacklisted; }
    public LocalDateTime getDetectTime() { return detectTime; }
    public void setDetectTime(LocalDateTime detectTime) { this.detectTime = detectTime; }
}
