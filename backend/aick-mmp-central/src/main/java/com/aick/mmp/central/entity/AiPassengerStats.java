package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_passenger_stats")
public class AiPassengerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "enter_count")
    private Integer enterCount = 0;

    @Column(name = "exit_count")
    private Integer exitCount = 0;

    @Column(name = "inside_count")
    private Integer insideCount = 0;

    @Column(name = "max_inside_count")
    private Integer maxInsideCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public Long getEdgeNodeId() { return edgeNodeId; }
    public void setEdgeNodeId(Long edgeNodeId) { this.edgeNodeId = edgeNodeId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getEnterCount() { return enterCount; }
    public void setEnterCount(Integer enterCount) { this.enterCount = enterCount; }
    public Integer getExitCount() { return exitCount; }
    public void setExitCount(Integer exitCount) { this.exitCount = exitCount; }
    public Integer getInsideCount() { return insideCount; }
    public void setInsideCount(Integer insideCount) { this.insideCount = insideCount; }
    public Integer getMaxInsideCount() { return maxInsideCount; }
    public void setMaxInsideCount(Integer maxInsideCount) { this.maxInsideCount = maxInsideCount; }
}
