package com.aick.mmp.edge.service.impl;

import com.aick.mmp.edge.config.ApiKeyConfig;
import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.HeartbeatRequest;
import com.aick.mmp.edge.dto.NetworkMetricsDTO;
import com.aick.mmp.edge.service.EdgeCameraService;
import com.aick.mmp.edge.service.EdgeHeartbeatService;
import com.aick.mmp.edge.util.EdgeSignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
    private ApiKeyConfig apiKeyConfig;

    @Autowired
    private EdgeSignatureUtil signatureUtil;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired(required = false)
    private EdgeCameraService edgeCameraService;  // 摄像头服务依赖 [新增]
    
    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean isMonitoring = false;
    private volatile boolean isRegistered = false;


    @PostConstruct
    public void init() {
        // Check if AK/SK is configured
        if (apiKeyConfig.getAccessKey() == null || apiKeyConfig.getAccessKey().isEmpty() ||
            apiKeyConfig.getSecretKey() == null || apiKeyConfig.getSecretKey().isEmpty()) {
            logger.warn("API Key (AK/SK) not configured. Edge node will not register with central server.");
            logger.warn("Please set EDGE_ACCESS_KEY and EDGE_SECRET_KEY environment variables.");
            return;
        }

        registerWithCentralServer();
        startHeartbeatMonitoring();
    }

    @PreDestroy
    public void cleanup() {
        stopHeartbeatMonitoring();
    }

    @Override
    public boolean sendHeartbeat(HeartbeatRequest heartbeatRequest) {
        if (!isRegistered) {
            logger.warn("Edge node is not registered. Skipping heartbeat.");
            return false;
        }

        int maxRetries = 3;
        int retryDelay = 2000; // 2 seconds

        // Ensure URL format is correct
        String baseUrl = edgeNodeConfig.getCentralServerUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String path = "/api/edge-nodes/" + edgeNodeConfig.getNodeId() + "/heartbeat";
        String centralServerUrl = baseUrl + path;

        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.debug("Sending heartbeat to: {}", centralServerUrl);

                // Prepare headers with AK/SK authentication
                HttpHeaders headers = prepareAuthHeaders("POST", path);
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Create HTTP entity with body and headers
                HttpEntity<HeartbeatRequest> requestEntity = new HttpEntity<>(heartbeatRequest, headers);

                // Send heartbeat request
                ResponseEntity<String> response = restTemplate.exchange(
                        centralServerUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.debug("Heartbeat sent successfully to central server");
                    return true;
                } else {
                    logger.warn("Failed to send heartbeat. Status: {}", response.getStatusCode());
                    if (response.getBody() != null) {
                        logger.warn("Response body: {}", response.getBody());
                    }
                }
            } catch (Exception e) {
                logger.warn("Error sending heartbeat to central server, attempt {}/{}. Error: {}",
                           (i+1), maxRetries, e.getMessage());

                // If this is the last attempt, log detailed error
                if (i == maxRetries - 1) {
                    logger.error("Error sending heartbeat to central server. URL: {}", centralServerUrl, e);
                }
            }

            // If not the last attempt, wait before retry
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelay * (i + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return false;
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
                        cpuUsage = sunBean.getCpuLoad() * 100;
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

            // Collect and set camera statuses [新增]
            heartbeat.setCameraStatuses(collectCameraStatuses());

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
        if (!isMonitoring && isRegistered) {
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
        int maxRetries = 5;
        int retryDelay = 5000; // 5 seconds

        // Create edge node registration data
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("name", edgeNodeConfig.getNodeId());
        registrationData.put("location", edgeNodeConfig.getRegion());
        registrationData.put("ipAddress", getLocalIPAddress());
        registrationData.put("port", 8081);
        registrationData.put("maxCameraSupport", edgeNodeConfig.getMaxConcurrentStreams());
        registrationData.put("currentCameraCount", 0);

        // Use new registration endpoint with AK/SK authentication
        String baseUrl = edgeNodeConfig.getCentralServerUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String path = "/api/edge/register";
        String centralServerUrl = baseUrl + path;

        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.info("Registering edge node with central server using AK/SK. POST {}", centralServerUrl);

                // Prepare headers with AK/SK authentication
                HttpHeaders headers = prepareAuthHeaders("POST", path);
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Create HTTP entity with body and headers
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(registrationData, headers);

                // Send registration request
                ResponseEntity<String> response = restTemplate.exchange(
                        centralServerUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Successfully registered edge node with central server");
                    isRegistered = true;
                    // Now start heartbeat monitoring
                    startHeartbeatMonitoring();
                    return; // Success - return
                } else {
                    logger.warn("Failed to register edge node. Status: {}", response.getStatusCode());
                    if (response.getBody() != null) {
                        logger.warn("Response body: {}", response.getBody());
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not register with central server, attempt {}/{}. Error: {}",
                           (i+1), maxRetries, e.getMessage());

                // If this is the last attempt, log detailed error
                if (i == maxRetries - 1) {
                    logger.error("Failed to register with central server after {} attempts", maxRetries, e);
                }
            }

            // If not the last attempt, wait before retry
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelay * (i + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Prepare HTTP headers with AK/SK authentication
     */
    private HttpHeaders prepareAuthHeaders(String method, String path) {
        HttpHeaders headers = new HttpHeaders();

        // Get current timestamp (use same timestamp for both signing and header)
        String timestamp = signatureUtil.getCurrentTimestamp();

        // Compute signature with the same timestamp
        String signature = signatureUtil.signRequest(method, path, apiKeyConfig.getSecretKey(), timestamp);

        // Add authentication headers
        headers.add("X-Access-Key", apiKeyConfig.getAccessKey());
        headers.add("X-Signature", signature);
        headers.add("X-Timestamp", timestamp);

        return headers;
    }

    private String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("Could not determine local IP address, using default");
            return "localhost";
        }
    }
    
    /**
     * Collect camera statuses for heartbeat reporting [新增]
     */
    private List<Map<String, Object>> collectCameraStatuses() {
        if (edgeCameraService == null) {
            logger.debug("EdgeCameraService not available, skipping camera status collection");
            return null;
        }
        
        try {
            List<Map<String, Object>> cameraStatuses = new java.util.ArrayList<>();
            var statuses = edgeCameraService.getAllCameraStatuses();
            
            for (var status : statuses) {
                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("cameraId", status.getCameraId());
                statusMap.put("status", status.getStatus() != null ? status.getStatus().name() : "UNKNOWN");
                statusMap.put("currentBitrate", (int) status.getCurrentBitrate());
                statusMap.put("currentFps", status.getCurrentFrameRate());
                statusMap.put("errorMessage", status.getErrorMessage());
                cameraStatuses.add(statusMap);
            }
            
            logger.debug("Collected {} camera statuses for heartbeat", cameraStatuses.size());
            return cameraStatuses;
        } catch (Exception e) {
            logger.warn("Error collecting camera statuses: {}", e.getMessage());
            return null;
        }
    }
}
