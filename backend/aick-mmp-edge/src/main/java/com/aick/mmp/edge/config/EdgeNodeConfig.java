package com.aick.mmp.edge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("edge")
@ConfigurationProperties(prefix = "edge")
@EnableConfigurationProperties(EdgeNodeConfig.class)
public class EdgeNodeConfig {


    private String nodeId;
    private String region;
    private String centralServerUrl;
    private int heartbeatInterval = 30; // seconds
    private int maxConcurrentStreams = 10;
    private String registryServer;
    
    // Network monitoring configuration
    private NetworkMonitoring networkMonitoring = new NetworkMonitoring();
    
    public static class NetworkMonitoring {
        private int intervalSeconds = 60;
        private double cpuThreshold = 80.0;
        private double memoryThreshold = 85.0;
        private double bandwidthThreshold = 80.0;
        private double latencyThreshold = 100.0;
        
        // Getters and setters
        public int getIntervalSeconds() {
            return intervalSeconds;
        }
        
        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
        
        public double getCpuThreshold() {
            return cpuThreshold;
        }
        
        public void setCpuThreshold(double cpuThreshold) {
            this.cpuThreshold = cpuThreshold;
        }
        
        public double getMemoryThreshold() {
            return memoryThreshold;
        }
        
        public void setMemoryThreshold(double memoryThreshold) {
            this.memoryThreshold = memoryThreshold;
        }
        
        public double getBandwidthThreshold() {
            return bandwidthThreshold;
        }
        
        public void setBandwidthThreshold(double bandwidthThreshold) {
            this.bandwidthThreshold = bandwidthThreshold;
        }
        
        public double getLatencyThreshold() {
            return latencyThreshold;
        }
        
        public void setLatencyThreshold(double latencyThreshold) {
            this.latencyThreshold = latencyThreshold;
        }
    }
    
    // Getters and setters
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getCentralServerUrl() {
        return centralServerUrl;
    }
    
    public void setCentralServerUrl(String centralServerUrl) {
        this.centralServerUrl = centralServerUrl;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }
    
    public void setMaxConcurrentStreams(int maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }
    
    public String getRegistryServer() {
        return registryServer;
    }
    
    public void setRegistryServer(String registryServer) {
        this.registryServer = registryServer;
    }
    
    public NetworkMonitoring getNetworkMonitoring() {
        return networkMonitoring;
    }
    
    public void setNetworkMonitoring(NetworkMonitoring networkMonitoring) {
        this.networkMonitoring = networkMonitoring;
    }
}