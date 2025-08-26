package com.aick.mmp.edge.dto;

import java.time.LocalDateTime;

public class NetworkMetricsDTO {
    
    private Double bandwidthUsage;
    private Double latency;
    private Double packetLoss;
    private Double throughput;
    private Integer activeConnections;
    private LocalDateTime timestamp;
    
    // Network quality indicators
    private String qualityLevel; // EXCELLENT, GOOD, FAIR, POOR
    private Boolean isStable;
    private Double jitter;
    
    // Constructors
    public NetworkMetricsDTO() {
        this.timestamp = LocalDateTime.now();
    }
    
    public NetworkMetricsDTO(Double bandwidthUsage, Double latency, Double packetLoss, Double throughput) {
        this();
        this.bandwidthUsage = bandwidthUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.throughput = throughput;
    }
    
    // Getters and setters
    public Double getBandwidthUsage() {
        return bandwidthUsage;
    }
    
    public void setBandwidthUsage(Double bandwidthUsage) {
        this.bandwidthUsage = bandwidthUsage;
    }
    
    public Double getLatency() {
        return latency;
    }
    
    public void setLatency(Double latency) {
        this.latency = latency;
    }
    
    public Double getPacketLoss() {
        return packetLoss;
    }
    
    public void setPacketLoss(Double packetLoss) {
        this.packetLoss = packetLoss;
    }
    
    public Double getThroughput() {
        return throughput;
    }
    
    public void setThroughput(Double throughput) {
        this.throughput = throughput;
    }
    
    public Integer getActiveConnections() {
        return activeConnections;
    }
    
    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = activeConnections;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getQualityLevel() {
        return qualityLevel;
    }
    
    public void setQualityLevel(String qualityLevel) {
        this.qualityLevel = qualityLevel;
    }
    
    public Boolean getIsStable() {
        return isStable;
    }
    
    public void setIsStable(Boolean isStable) {
        this.isStable = isStable;
    }
    
    public Double getJitter() {
        return jitter;
    }
    
    public void setJitter(Double jitter) {
        this.jitter = jitter;
    }
}