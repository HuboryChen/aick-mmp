package com.aick.mmp.edge.service;

import com.aick.mmp.edge.dto.HeartbeatRequest;
import com.aick.mmp.edge.dto.NetworkMetricsDTO;

public interface EdgeHeartbeatService {
    
    /**
     * Send heartbeat to central server
     * @param heartbeatRequest The heartbeat data
     * @return Success status
     */
    boolean sendHeartbeat(HeartbeatRequest heartbeatRequest);
    
    /**
     * Collect current system metrics
     * @return Current system metrics
     */
    HeartbeatRequest collectSystemMetrics();
    
    /**
     * Collect network metrics
     * @return Current network metrics
     */
    NetworkMetricsDTO collectNetworkMetrics();
    
    /**
     * Start periodic heartbeat monitoring
     */
    void startHeartbeatMonitoring();
    
    /**
     * Stop periodic heartbeat monitoring
     */
    void stopHeartbeatMonitoring();
    
    /**
     * Get current edge node status
     * @return Current status information
     */
    HeartbeatRequest getCurrentStatus();
}