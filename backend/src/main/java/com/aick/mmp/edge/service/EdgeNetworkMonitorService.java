package com.aick.mmp.edge.service;

import com.aick.mmp.edge.dto.NetworkMetricsDTO;

import java.util.Map;

/**
 * Edge Network Monitor Service - monitors network conditions on edge nodes independently
 */
public interface EdgeNetworkMonitorService {
    
    /**
     * Collect current network metrics
     */
    NetworkMetricsDTO collectNetworkMetrics();
    
    /**
     * Evaluate network conditions and adjust streaming parameters
     */
    void evaluateNetworkConditions();
    
    /**
     * Check if network meets minimum requirements for streaming
     */
    boolean meetsMinimumRequirements();
    
    /**
     * Get recommended quality level based on current network conditions
     */
    int getRecommendedQualityLevel();
    
    /**
     * Get network health status
     */
    Map<String, Object> getNetworkHealth();
    
    /**
     * Start network monitoring
     */
    void startMonitoring();
    
    /**
     * Stop network monitoring
     */
    void stopMonitoring();
    
    /**
     * Get network monitoring statistics
     */
    Map<String, Object> getMonitoringStatistics();
    
    /**
     * Reset network monitoring statistics
     */
    void resetStatistics();
    
    /**
     * Check if monitoring is active
     */
    boolean isMonitoringActive();
    
    /**
     * Get average latency over time period
     */
    double getAverageLatency(int minutes);
    
    /**
     * Get average bandwidth usage over time period
     */
    double getAverageBandwidthUsage(int minutes);
    
    /**
     * Check if network is stable
     */
    boolean isNetworkStable();
    
    /**
     * Predict network conditions for next period
     */
    NetworkMetricsDTO predictNetworkConditions(int minutesAhead);
}