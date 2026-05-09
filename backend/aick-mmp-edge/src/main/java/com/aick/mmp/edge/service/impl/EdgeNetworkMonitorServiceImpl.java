package com.aick.mmp.edge.service.impl;

import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.NetworkMetricsDTO;
import com.aick.mmp.edge.service.EdgeNetworkMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeNetworkMonitorServiceImpl implements EdgeNetworkMonitorService {

    private final EdgeNodeConfig edgeNodeConfig;
    
    // Monitoring data storage
    private final ConcurrentLinkedQueue<NetworkMetricsDTO> networkHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, Object> monitoringStatistics = new HashMap<>();
    
    private ScheduledExecutorService monitoringScheduler;
    private volatile boolean isMonitoring = false;
    private NetworkMetricsDTO currentMetrics;

    @PostConstruct
    public void init() {
        log.info("Initializing EdgeNetworkMonitorService for node: {}", edgeNodeConfig.getNodeId());
        startMonitoring();
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down EdgeNetworkMonitorService");
        stopMonitoring();
    }

    @Override
    public NetworkMetricsDTO collectNetworkMetrics() {
        try {
            NetworkMetricsDTO metrics = new NetworkMetricsDTO();

            // Network latency (simulate ping to central server)
            double latency = measureLatency();
            metrics.setLatency(latency);
            
            // Bandwidth usage (simplified calculation)
            double bandwidthUsage = calculateBandwidthUsage();
            metrics.setBandwidthUsage(bandwidthUsage);
            
            // Packet loss simulation
            double packetLoss = calculatePacketLoss(latency);
            metrics.setPacketLoss(packetLoss);
            
            // Throughput calculation
            double throughput = calculateThroughput(bandwidthUsage);
            metrics.setThroughput(throughput);
            
            // Active connections count
            int activeConnections = calculateActiveConnections();
            metrics.setActiveConnections(activeConnections);
            
            // Jitter calculation
            double jitter = calculateJitter();
            metrics.setJitter(jitter);
            
            // Determine quality level
            String qualityLevel = determineQualityLevel(metrics);
            metrics.setQualityLevel(qualityLevel);
            
            // Network stability
            boolean isStable = isNetworkStable(metrics);
            metrics.setIsStable(isStable);
            
            // Update current metrics
            currentMetrics = metrics;
            
            // Add to history (keep last 100 entries)
            networkHistory.offer(metrics);
            if (networkHistory.size() > 100) {
                networkHistory.poll();
            }
            
            return metrics;
            
        } catch (Exception e) {
            log.error("Error collecting network metrics", e);
            return createDefaultMetrics();
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000) // Every minute
    public void evaluateNetworkConditions() {
        if (!isMonitoring) {
            return;
        }
        
        log.debug("Evaluating network conditions for edge node: {}", edgeNodeConfig.getNodeId());
        
        NetworkMetricsDTO metrics = collectNetworkMetrics();
        
        // Check if conditions require action
        if (metrics.getLatency() > edgeNodeConfig.getNetworkMonitoring().getLatencyThreshold()) {
            log.warn("High latency detected: {}ms", metrics.getLatency());
            handleHighLatency(metrics);
        }
        
        if (metrics.getBandwidthUsage() > edgeNodeConfig.getNetworkMonitoring().getBandwidthThreshold()) {
            log.warn("High bandwidth usage detected: {}%", metrics.getBandwidthUsage());
            handleHighBandwidthUsage(metrics);
        }
        
        if (metrics.getPacketLoss() > 2.0) {
            log.warn("High packet loss detected: {}%", metrics.getPacketLoss());
            handleHighPacketLoss(metrics);
        }
        
        updateMonitoringStatistics(metrics);
    }

    @Override
    public boolean meetsMinimumRequirements() {
        if (currentMetrics == null) {
            collectNetworkMetrics();
        }
        
        return currentMetrics != null && 
               currentMetrics.getLatency() < 100 && 
               currentMetrics.getPacketLoss() < 5.0 &&
               currentMetrics.getBandwidthUsage() < 90;
    }

    @Override
    public int getRecommendedQualityLevel() {
        if (currentMetrics == null) {
            collectNetworkMetrics();
        }
        
        if (currentMetrics == null) {
            return 2; // Default medium-low quality
        }
        
        // Determine quality level based on network conditions
        if (currentMetrics.getLatency() < 20 && currentMetrics.getPacketLoss() < 0.5 && currentMetrics.getBandwidthUsage() < 50) {
            return 5; // Ultra quality
        } else if (currentMetrics.getLatency() < 50 && currentMetrics.getPacketLoss() < 1.0 && currentMetrics.getBandwidthUsage() < 70) {
            return 4; // High quality
        } else if (currentMetrics.getLatency() < 100 && currentMetrics.getPacketLoss() < 2.0 && currentMetrics.getBandwidthUsage() < 80) {
            return 3; // Medium quality
        } else if (currentMetrics.getLatency() < 200 && currentMetrics.getPacketLoss() < 3.0 && currentMetrics.getBandwidthUsage() < 90) {
            return 2; // Medium-low quality
        } else {
            return 1; // Low quality
        }
    }

    @Override
    public Map<String, Object> getNetworkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        if (currentMetrics != null) {
            health.put("latency", currentMetrics.getLatency());
            health.put("bandwidthUsage", currentMetrics.getBandwidthUsage());
            health.put("packetLoss", currentMetrics.getPacketLoss());
            health.put("throughput", currentMetrics.getThroughput());
            health.put("qualityLevel", currentMetrics.getQualityLevel());
            health.put("isStable", currentMetrics.getIsStable());
            health.put("jitter", currentMetrics.getJitter());
            health.put("activeConnections", currentMetrics.getActiveConnections());
        }
        
        health.put("meetsMinimumRequirements", meetsMinimumRequirements());
        health.put("recommendedQualityLevel", getRecommendedQualityLevel());
        health.put("isMonitoring", isMonitoring);
        health.put("edgeNodeId", edgeNodeConfig.getNodeId());
        health.put("lastUpdate", LocalDateTime.now());
        
        return health;
    }

    @Override
    public void startMonitoring() {
        if (!isMonitoring) {
            monitoringScheduler = Executors.newScheduledThreadPool(1);
            monitoringScheduler.scheduleAtFixedRate(
                this::evaluateNetworkConditions,
                0,
                edgeNodeConfig.getNetworkMonitoring().getIntervalSeconds(),
                TimeUnit.SECONDS
            );
            isMonitoring = true;
            log.info("Network monitoring started for edge node: {}", edgeNodeConfig.getNodeId());
        }
    }

    @Override
    public void stopMonitoring() {
        if (isMonitoring && monitoringScheduler != null) {
            monitoringScheduler.shutdown();
            try {
                if (!monitoringScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isMonitoring = false;
            log.info("Network monitoring stopped for edge node: {}", edgeNodeConfig.getNodeId());
        }
    }

    @Override
    public Map<String, Object> getMonitoringStatistics() {
        Map<String, Object> stats = new HashMap<>(monitoringStatistics);
        stats.put("totalMeasurements", networkHistory.size());
        stats.put("averageLatency", getAverageLatency(60));
        stats.put("averageBandwidthUsage", getAverageBandwidthUsage(60));
        stats.put("isNetworkStable", isNetworkStable());
        return stats;
    }

    @Override
    public void resetStatistics() {
        monitoringStatistics.clear();
        networkHistory.clear();
        log.info("Network monitoring statistics reset for edge node: {}", edgeNodeConfig.getNodeId());
    }

    @Override
    public boolean isMonitoringActive() {
        return isMonitoring;
    }

    @Override
    public double getAverageLatency(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        return networkHistory.stream()
                .filter(metrics -> metrics.getTimestamp().isAfter(cutoff))
                .mapToDouble(NetworkMetricsDTO::getLatency)
                .average()
                .orElse(0.0);
    }

    @Override
    public double getAverageBandwidthUsage(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        return networkHistory.stream()
                .filter(metrics -> metrics.getTimestamp().isAfter(cutoff))
                .mapToDouble(NetworkMetricsDTO::getBandwidthUsage)
                .average()
                .orElse(0.0);
    }

    @Override
    public boolean isNetworkStable() {
        if (networkHistory.size() < 5) {
            return false;
        }
        
        // Check last 5 measurements for stability
        List<NetworkMetricsDTO> recent = new ArrayList<>(networkHistory).subList(
            Math.max(0, networkHistory.size() - 5), networkHistory.size()
        );
        
        double avgLatency = recent.stream().mapToDouble(NetworkMetricsDTO::getLatency).average().orElse(0);
        double latencyVariance = recent.stream()
                .mapToDouble(m -> Math.pow(m.getLatency() - avgLatency, 2))
                .average().orElse(0);
        
        // Consider network stable if latency variance is low
        return latencyVariance < 100; // Adjust threshold as needed
    }

    @Override
    public NetworkMetricsDTO predictNetworkConditions(int minutesAhead) {
        if (networkHistory.size() < 3) {
            return currentMetrics != null ? currentMetrics : createDefaultMetrics();
        }
        
        // Simple linear prediction based on recent trend
        List<NetworkMetricsDTO> recent = new ArrayList<>(networkHistory).subList(
            Math.max(0, networkHistory.size() - 3), networkHistory.size()
        );
        
        double latencyTrend = calculateTrend(recent, NetworkMetricsDTO::getLatency);
        double bandwidthTrend = calculateTrend(recent, NetworkMetricsDTO::getBandwidthUsage);
        
        NetworkMetricsDTO prediction = new NetworkMetricsDTO();
        prediction.setLatency(Math.max(0, currentMetrics.getLatency() + (latencyTrend * minutesAhead)));
        prediction.setBandwidthUsage(Math.max(0, Math.min(100, currentMetrics.getBandwidthUsage() + (bandwidthTrend * minutesAhead))));
        prediction.setPacketLoss(currentMetrics.getPacketLoss()); // Assume stable packet loss
        prediction.setThroughput(calculateThroughput(prediction.getBandwidthUsage()));
        prediction.setQualityLevel(determineQualityLevel(prediction));
        prediction.setIsStable(isNetworkStable(prediction));
        
        return prediction;
    }

    // Private helper methods
    
    private double measureLatency() {
        try {
            // Simplified latency measurement - in reality, you'd ping the central server
            long startTime = System.currentTimeMillis();
            InetAddress.getByName("127.0.0.1").isReachable(5000);
            long endTime = System.currentTimeMillis();
            return endTime - startTime + (Math.random() * 30); // Add some realistic variation
        } catch (Exception e) {
            return 50 + (Math.random() * 100); // Default latency with variation
        }
    }

    private double calculateBandwidthUsage() {
        // Simplified bandwidth calculation - in reality, you'd measure actual network usage
        return Math.random() * 80 + 10; // 10-90% usage
    }

    private double calculatePacketLoss(double latency) {
        // Simulate packet loss based on latency
        if (latency < 50) {
            return Math.random() * 0.5;
        } else if (latency < 100) {
            return Math.random() * 1.0;
        } else {
            return Math.random() * 3.0;
        }
    }

    private double calculateThroughput(double bandwidthUsage) {
        // Assume 1Gbps connection, calculate actual throughput
        return (1000 * bandwidthUsage) / 100;
    }

    private int calculateActiveConnections() {
        // Simplified active connections count
        return (int)(Math.random() * 20) + 5;
    }

    private double calculateJitter() {
        return Math.random() * 10; // 0-10ms jitter
    }

    private String determineQualityLevel(NetworkMetricsDTO metrics) {
        if (metrics.getLatency() < 20 && metrics.getPacketLoss() < 0.5) {
            return "EXCELLENT";
        } else if (metrics.getLatency() < 50 && metrics.getPacketLoss() < 1.0) {
            return "GOOD";
        } else if (metrics.getLatency() < 100 && metrics.getPacketLoss() < 2.0) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    private boolean isNetworkStable(NetworkMetricsDTO metrics) {
        return metrics.getPacketLoss() < 1.0 && metrics.getJitter() < 5.0;
    }

    private NetworkMetricsDTO createDefaultMetrics() {
        NetworkMetricsDTO defaults = new NetworkMetricsDTO();
        defaults.setLatency(50.0);
        defaults.setBandwidthUsage(50.0);
        defaults.setPacketLoss(1.0);
        defaults.setThroughput(500.0);
        defaults.setActiveConnections(10);
        defaults.setJitter(5.0);
        defaults.setQualityLevel("FAIR");
        defaults.setIsStable(true);
        return defaults;
    }

    private void handleHighLatency(NetworkMetricsDTO metrics) {
        log.warn("Handling high latency: {}ms - reducing stream quality", metrics.getLatency());
        // In a real implementation, this would adjust stream quality automatically
    }

    private void handleHighBandwidthUsage(NetworkMetricsDTO metrics) {
        log.warn("Handling high bandwidth usage: {}% - optimizing streams", metrics.getBandwidthUsage());
        // In a real implementation, this would optimize bandwidth usage
    }

    private void handleHighPacketLoss(NetworkMetricsDTO metrics) {
        log.warn("Handling high packet loss: {}% - adjusting protocols", metrics.getPacketLoss());
        // In a real implementation, this would adjust transmission protocols
    }

    private void updateMonitoringStatistics(NetworkMetricsDTO metrics) {
        monitoringStatistics.put("lastUpdate", LocalDateTime.now());
        monitoringStatistics.put("totalMeasurements", networkHistory.size());
        monitoringStatistics.put("currentLatency", metrics.getLatency());
        monitoringStatistics.put("currentBandwidth", metrics.getBandwidthUsage());
        monitoringStatistics.put("currentQuality", metrics.getQualityLevel());
    }

    private double calculateTrend(List<NetworkMetricsDTO> data, java.util.function.ToDoubleFunction<NetworkMetricsDTO> extractor) {
        if (data.size() < 2) {
            return 0;
        }
        
        double first = extractor.applyAsDouble(data.get(0));
        double last = extractor.applyAsDouble(data.get(data.size() - 1));
        
        return (last - first) / data.size();
    }
}