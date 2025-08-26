package com.aick.mmp.edge.dto;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class HeartbeatRequest {
    
    @NotNull
    private String nodeId;
    
    @NotNull
    private Double cpuUsage;
    
    @NotNull
    private Double memoryUsage;
    
    @NotNull
    private Double storageUsage;
    
    private String softwareVersion;
    private String hardwareInfo;
    private Integer currentCameraCount;
    private Map<String, Object> systemMetrics;
    private Map<String, Object> networkMetrics;
    
    // Constructors
    public HeartbeatRequest() {}
    
    public HeartbeatRequest(String nodeId, Double cpuUsage, Double memoryUsage, Double storageUsage) {
        this.nodeId = nodeId;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.storageUsage = storageUsage;
    }
    
    // Getters and setters
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public Double getCpuUsage() {
        return cpuUsage;
    }
    
    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
    
    public Double getMemoryUsage() {
        return memoryUsage;
    }
    
    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    
    public Double getStorageUsage() {
        return storageUsage;
    }
    
    public void setStorageUsage(Double storageUsage) {
        this.storageUsage = storageUsage;
    }
    
    public String getSoftwareVersion() {
        return softwareVersion;
    }
    
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }
    
    public String getHardwareInfo() {
        return hardwareInfo;
    }
    
    public void setHardwareInfo(String hardwareInfo) {
        this.hardwareInfo = hardwareInfo;
    }
    
    public Integer getCurrentCameraCount() {
        return currentCameraCount;
    }
    
    public void setCurrentCameraCount(Integer currentCameraCount) {
        this.currentCameraCount = currentCameraCount;
    }
    
    public Map<String, Object> getSystemMetrics() {
        return systemMetrics;
    }
    
    public void setSystemMetrics(Map<String, Object> systemMetrics) {
        this.systemMetrics = systemMetrics;
    }
    
    public Map<String, Object> getNetworkMetrics() {
        return networkMetrics;
    }
    
    public void setNetworkMetrics(Map<String, Object> networkMetrics) {
        this.networkMetrics = networkMetrics;
    }
}