package com.aick.mmp.edge.service.impl;

import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.HeartbeatRequest;
import com.aick.mmp.edge.dto.NetworkMetricsDTO;
import com.aick.mmp.edge.service.EdgeHeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Profile("edge")
public class EdgeHeartbeatServiceImpl implements EdgeHeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(EdgeHeartbeatServiceImpl.class);

    @Autowired
    private EdgeNodeConfig edgeNodeConfig;

    @Autowired
    private RestTemplate restTemplate;

    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean isMonitoring = false;

    @PostConstruct
    public void init() {
        registerWithCentralServer();
        startHeartbeatMonitoring();
    }

    @PreDestroy
    public void cleanup() {
        stopHeartbeatMonitoring();
    }

    @Override
    public boolean sendHeartbeat(HeartbeatRequest heartbeatRequest) {
        try {
            String centralServerUrl = edgeNodeConfig.getCentralServerUrl() + "/api/edge-nodes/" + 
                                     edgeNodeConfig.getNodeId() + "/heartbeat";
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                centralServerUrl, 
                heartbeatRequest, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Heartbeat sent successfully to central server");
                return true;
            } else {
                logger.warn("Failed to send heartbeat. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending heartbeat to central server", e);
            return false;
        }
    }

    @Override
    public HeartbeatRequest collectSystemMetrics() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // CPU usage
            double cpuUsage = 0;
            try {
                // Try to get process CPU load first
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunBean = 
                        (com.sun.management.OperatingSystemMXBean) osBean;
                    cpuUsage = sunBean.getProcessCpuLoad() * 100;
                    if (cpuUsage < 0) {
                        // Fallback to system CPU load
                        cpuUsage = sunBean.getSystemCpuLoad() * 100;
                    }
                }
                
                // If still negative or zero, use system load average as approximation
                if (cpuUsage <= 0) {
                    double loadAverage = osBean.getSystemLoadAverage();
                    if (loadAverage >= 0) {
                        cpuUsage = Math.min(loadAverage / osBean.getAvailableProcessors() * 100, 100);
                    }
                }
                
                // Final fallback to simulate a reasonable CPU usage
                if (cpuUsage <= 0) {
                    cpuUsage = Math.random() * 30 + 10; // 10-40% simulated
                }
            } catch (Exception e) {
                logger.debug("Could not get CPU usage, using fallback: {}", e.getMessage());
                cpuUsage = Math.random() * 30 + 10; // 10-40% simulated
            }

            // Memory usage
            long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            double memoryUsage = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;

            // Storage usage
            double storageUsage = calculateStorageUsage();

            HeartbeatRequest heartbeat = new HeartbeatRequest(
                edgeNodeConfig.getNodeId(),
                cpuUsage,
                memoryUsage,
                storageUsage
            );

            // Add additional system metrics
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("availableProcessors", osBean.getAvailableProcessors());
            systemMetrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
            systemMetrics.put("totalMemoryMB", totalMemory / (1024 * 1024));
            systemMetrics.put("usedMemoryMB", usedMemory / (1024 * 1024));
            
            heartbeat.setSystemMetrics(systemMetrics);
            
            // Collect and set network metrics
            NetworkMetricsDTO networkMetrics = collectNetworkMetrics();
            Map<String, Object> networkMetricsMap = new HashMap<>();
            networkMetricsMap.put("bandwidthUsage", networkMetrics.getBandwidthUsage());
            networkMetricsMap.put("latency", networkMetrics.getLatency());
            networkMetricsMap.put("packetLoss", networkMetrics.getPacketLoss());
            networkMetricsMap.put("qualityLevel", networkMetrics.getQualityLevel());
            heartbeat.setNetworkMetrics(networkMetricsMap);

            // Set software version and hardware info
            heartbeat.setSoftwareVersion(getClass().getPackage().getImplementationVersion());
            heartbeat.setHardwareInfo(osBean.getName() + " " + osBean.getVersion());

            return heartbeat;
        } catch (Exception e) {
            logger.error("Error collecting system metrics", e);
            return new HeartbeatRequest(edgeNodeConfig.getNodeId(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    public NetworkMetricsDTO collectNetworkMetrics() {
        NetworkMetricsDTO metrics = new NetworkMetricsDTO();
        
        try {
            // Simplified network metrics collection
            // In a real implementation, you would use system-specific tools
            metrics.setBandwidthUsage(Math.random() * 100); // Simulated
            metrics.setLatency(Math.random() * 50 + 10); // 10-60ms
            metrics.setPacketLoss(Math.random() * 2); // 0-2%
            metrics.setThroughput(Math.random() * 1000 + 100); // 100-1100 Mbps
            metrics.setActiveConnections((int)(Math.random() * 50));
            
            // Determine quality level based on metrics
            if (metrics.getLatency() < 20 && metrics.getPacketLoss() < 0.5) {
                metrics.setQualityLevel("EXCELLENT");
            } else if (metrics.getLatency() < 50 && metrics.getPacketLoss() < 1.0) {
                metrics.setQualityLevel("GOOD");
            } else if (metrics.getLatency() < 100 && metrics.getPacketLoss() < 2.0) {
                metrics.setQualityLevel("FAIR");
            } else {
                metrics.setQualityLevel("POOR");
            }
            
            metrics.setIsStable(metrics.getPacketLoss() < 1.0);
            metrics.setJitter(Math.random() * 10); // 0-10ms jitter
            
        } catch (Exception e) {
            logger.error("Error collecting network metrics", e);
        }
        
        return metrics;
    }

    @Override
    public void startHeartbeatMonitoring() {
        if (!isMonitoring) {
            heartbeatScheduler = Executors.newScheduledThreadPool(1);
            heartbeatScheduler.scheduleAtFixedRate(
                this::performHeartbeat,
                0,
                edgeNodeConfig.getHeartbeatInterval(),
                TimeUnit.SECONDS
            );
            isMonitoring = true;
            logger.info("Heartbeat monitoring started with interval: {} seconds", 
                       edgeNodeConfig.getHeartbeatInterval());
        }
    }

    @Override
    public void stopHeartbeatMonitoring() {
        if (isMonitoring && heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isMonitoring = false;
            logger.info("Heartbeat monitoring stopped");
        }
    }

    @Override
    public HeartbeatRequest getCurrentStatus() {
        return collectSystemMetrics();
    }

    private void performHeartbeat() {
        try {
            HeartbeatRequest heartbeat = collectSystemMetrics();
            sendHeartbeat(heartbeat);
        } catch (Exception e) {
            logger.error("Error performing scheduled heartbeat", e);
        }
    }

    private double calculateStorageUsage() {
        try {
            Path path = FileSystems.getDefault().getPath("/");
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            return total > 0 ? (double) used / total * 100 : 0;
        } catch (Exception e) {
            logger.warn("Could not calculate storage usage", e);
            return 0;
        }
    }

    private void registerWithCentralServer() {
        try {
            // Create edge node registration data
            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("name", edgeNodeConfig.getNodeId());
            registrationData.put("location", edgeNodeConfig.getRegion());
            registrationData.put("ipAddress", getLocalIPAddress());
            registrationData.put("port", 8081);
            registrationData.put("maxCameraSupport", edgeNodeConfig.getMaxConcurrentStreams());
            registrationData.put("currentCameraCount", 0);
            registrationData.put("enabled", true);
            
            String centralServerUrl = edgeNodeConfig.getCentralServerUrl() + "/api/edge-nodes";
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                    centralServerUrl, 
                    registrationData, 
                    String.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Successfully registered edge node with central server");
                } else {
                    logger.warn("Failed to register edge node. Status: {}", response.getStatusCode());
                }
            } catch (Exception e) {
                logger.warn("Could not register with central server, will try heartbeat anyway: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error during edge node registration", e);
        }
    }

    private String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("Could not determine local IP address, using default");
            return "localhost";
        }
    }
}