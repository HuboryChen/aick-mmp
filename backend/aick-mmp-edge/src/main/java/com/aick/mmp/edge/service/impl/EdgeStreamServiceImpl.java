package com.aick.mmp.edge.service.impl;

import com.aick.mmp.shared.adapter.protocol.ProtocolAdapter;
import com.aick.mmp.shared.adapter.protocol.ProtocolAdapterFactory;
import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.EdgeCameraDTO;
import com.aick.mmp.edge.event.StreamFailedEvent;
import com.aick.mmp.edge.dto.EdgeStreamDTO;
import com.aick.mmp.edge.service.EdgeCameraService;
import com.aick.mmp.edge.service.EdgeStreamService;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.StreamSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeStreamServiceImpl implements EdgeStreamService {

    private final EdgeNodeConfig edgeNodeConfig;
    private final EdgeCameraService edgeCameraService;
    private final ProtocolAdapterFactory protocolAdapterFactory;
    private final ApplicationEventPublisher eventPublisher;

    // Reconnect configuration
    @Value("${stream.reconnect.max-retries:3}")
    private int maxRetries;

    @Value("${stream.reconnect.enabled:true}")
    private boolean reconnectEnabled;

    @Value("${stream.reconnect.retry-interval-seconds:60}")
    private long retryIntervalSeconds;

    // Local stream storage
    private final Map<String, EdgeStreamDTO> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> streamMetrics = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> streamHeartbeats = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing EdgeStreamService for node: {}", edgeNodeConfig.getNodeId());
        initializeStreaming();
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down EdgeStreamService");
        shutdownAllStreams();
    }

    @Override
    public EdgeStreamDTO startStream(Long cameraId, Map<String, Object> parameters) {
        log.info("Starting stream for camera: {} on edge node: {}", cameraId, edgeNodeConfig.getNodeId());
        
        // Check if we can handle more streams
        if (!canHandleMoreStreams()) {
            throw new RuntimeException("Edge node has reached maximum stream capacity");
        }
        
        // Get camera information
        Optional<EdgeCameraDTO> cameraOpt = edgeCameraService.getCameraById(cameraId);
        if (!cameraOpt.isPresent()) {
            throw new RuntimeException("Camera not found: " + cameraId);
        }
        
        EdgeCameraDTO camera = cameraOpt.get();
        if (camera.getStatus() != Camera.CameraStatus.ONLINE) {
            throw new RuntimeException("Camera is not online: " + cameraId);
        }
        
        try {
            // Generate session ID
            String sessionId = generateSessionId();
            
            // Get protocol adapter
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
            if (adapter == null) {
                throw new RuntimeException("No protocol adapter found for: " + camera.getProtocol());
            }
            
            // Convert to Camera entity for adapter
            Camera cameraEntity = convertToEntity(camera);

            // Start stream session
            adapter.startStreamSession(cameraEntity);

            // Create stream DTO
            EdgeStreamDTO streamDTO = EdgeStreamDTO.builder()
                    .sessionId(sessionId)
                    .cameraId(cameraId)
                    .protocol(camera.getProtocol().name())
                    .status(StreamSession.StreamStatus.STREAMING)
                    .startTime(LocalDateTime.now())
                    .edgeNodeId(edgeNodeConfig.getNodeId())
                    .qualityLevel(getQualityLevel(parameters))
                    .bitrate(camera.getBitrate() != null ? camera.getBitrate().doubleValue() : 1000.0)
                    .frameRate(camera.getFrameRate() != null ? camera.getFrameRate().doubleValue() : 25.0)
                    .resolution(camera.getResolution())
                    .isActive(true)
                    .metrics(new HashMap<>())
                    .connectionRetries(0)
                    .lastHeartbeat(LocalDateTime.now())
                    .build();
            
            // Generate stream URLs
            streamDTO.setLocalStreamUrl(generateLocalStreamUrl(sessionId));
            
            // Store active stream
            activeStreams.put(sessionId, streamDTO);
            streamHeartbeats.put(sessionId, LocalDateTime.now());
            
            // Initialize metrics
            initializeStreamMetrics(sessionId);
            
            log.info("Started stream for camera {} with session {}", cameraId, sessionId);
            return streamDTO;
            
        } catch (Exception e) {
            log.error("Failed to start stream for camera {}: {}", cameraId, e.getMessage());
            throw new RuntimeException("Stream start failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStream(String sessionId) {
        log.info("Stopping stream: {}", sessionId);
        
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        if (stream == null) {
            log.warn("Stream session not found: {}", sessionId);
            return;
        }
        
        try {
            // Get camera and adapter
            Optional<EdgeCameraDTO> cameraOpt = edgeCameraService.getCameraById(stream.getCameraId());
            if (cameraOpt.isPresent()) {
                EdgeCameraDTO camera = cameraOpt.get();
                ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
                if (adapter != null) {
                    adapter.stopStreamSession(sessionId);
                }
            }
            
            // Update stream status
            stream.setStatus(StreamSession.StreamStatus.DISCONNECTED);
            stream.setEndTime(LocalDateTime.now());
            stream.setActive(false);
            
            // Remove from active streams
            activeStreams.remove(sessionId);
            streamMetrics.remove(sessionId);
            streamHeartbeats.remove(sessionId);
            
            log.info("Stopped stream: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Error stopping stream {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public Optional<EdgeStreamDTO> getStream(String sessionId) {
        return Optional.ofNullable(activeStreams.get(sessionId));
    }

    @Override
    public List<EdgeStreamDTO> getActiveStreams() {
        return activeStreams.values().stream()
                .filter(EdgeStreamDTO::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<EdgeStreamDTO> getStreamsByCamera(Long cameraId) {
        return activeStreams.values().stream()
                .filter(stream -> stream.getCameraId().equals(cameraId))
                .collect(Collectors.toList());
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        log.info("Adjusting stream quality for session {} to level {}", sessionId, qualityLevel);
        
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        if (stream == null) {
            log.warn("Stream session not found: {}", sessionId);
            return;
        }
        
        try {
            Optional<EdgeCameraDTO> cameraOpt = edgeCameraService.getCameraById(stream.getCameraId());
            if (cameraOpt.isPresent()) {
                EdgeCameraDTO camera = cameraOpt.get();
                ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
                if (adapter != null) {
                    adapter.adjustStreamQuality(sessionId, qualityLevel);
                    stream.setQualityLevel(qualityLevel);
                    
                    // Update bitrate and frame rate based on quality level
                    updateStreamParameters(stream, qualityLevel);
                }
            }
        } catch (Exception e) {
            log.error("Error adjusting stream quality for session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        return streamMetrics.getOrDefault(sessionId, new HashMap<>());
    }

    @Override
    public Map<String, Object> getAllStreamMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.put("totalActiveStreams", getActiveStreamsCount());
        allMetrics.put("totalBandwidthUsage", getCurrentBandwidthUsage());
        allMetrics.put("edgeNodeId", edgeNodeConfig.getNodeId());
        allMetrics.put("maxConcurrentStreams", edgeNodeConfig.getMaxConcurrentStreams());
        allMetrics.put("streamDetails", streamMetrics);
        return allMetrics;
    }

    @Override
    public void updateStreamStatus(String sessionId, StreamSession.StreamStatus status) {
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        if (stream != null) {
            stream.setStatus(status);
            stream.setLastHeartbeat(LocalDateTime.now());

            if (status == StreamSession.StreamStatus.ERROR) {
                stream.setActive(false);
                stream.setConnectionRetries(stream.getConnectionRetries() + 1);

                // 发布流失败事件，触发异步重连（仅在支持重连时）
                if (reconnectEnabled && stream.getConnectionRetries() < maxRetries) {
                    StreamFailedEvent event = new StreamFailedEvent(
                            this,
                            stream.getCameraId(),
                            edgeNodeConfig.getNodeId(),
                            "STREAM_ERROR",
                            stream.getErrorMessage()
                    );
                    eventPublisher.publishEvent(event);
                    log.debug("Published StreamFailedEvent for camera: {}", stream.getCameraId());
                }
            }
        }
    }

    @Override
    public long getStreamCountByStatus(StreamSession.StreamStatus status) {
        return activeStreams.values().stream()
                .filter(stream -> stream.getStatus() == status)
                .count();
    }

    @Override
    public long getActiveStreamsCount() {
        return activeStreams.values().stream()
                .filter(EdgeStreamDTO::isActive)
                .count();
    }

    @Override
    public boolean canHandleMoreStreams() {
        return getActiveStreamsCount() < edgeNodeConfig.getMaxConcurrentStreams();
    }

    @Override
    public double getCurrentBandwidthUsage() {
        return activeStreams.values().stream()
                .filter(EdgeStreamDTO::isActive)
                .mapToDouble(EdgeStreamDTO::getBitrate)
                .sum() / 1000.0; // Convert to Mbps
    }

    @Override
    @Scheduled(fixedDelayString = "${stream.reconnect.retry-interval-seconds:60}000")
    public void restartFailedStreams() {
        if (!reconnectEnabled) {
            return;
        }

        log.debug("Checking for failed streams to restart");

        List<EdgeStreamDTO> failedStreams = activeStreams.values().stream()
                .filter(stream -> stream.getStatus() == StreamSession.StreamStatus.ERROR)
                .filter(stream -> stream.getConnectionRetries() < maxRetries)
                .collect(Collectors.toList());

        for (EdgeStreamDTO stream : failedStreams) {
            log.info("Attempting to restart failed stream: {} (attempt {}/{})",
                    stream.getSessionId(), stream.getConnectionRetries() + 1, maxRetries);
            try {
                restartStream(stream);
            } catch (Exception e) {
                log.error("Failed to restart stream {}: {}", stream.getSessionId(), e.getMessage());
            }
        }
    }

    @Override
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupInactiveStreams() {
        log.debug("Cleaning up inactive streams");
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<String> inactiveSessionIds = streamHeartbeats.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        for (String sessionId : inactiveSessionIds) {
            log.info("Cleaning up inactive stream: {}", sessionId);
            stopStream(sessionId);
        }
    }

    @Override
    public String getLocalStreamUrl(String sessionId) {
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        return stream != null ? stream.getLocalStreamUrl() : null;
    }

    @Override
    public String getExternalStreamUrl(String sessionId) {
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        if (stream != null) {
            return String.format("http://%s:8080/api/edge/stream/%s", 
                               edgeNodeConfig.getNodeId(), sessionId);
        }
        return null;
    }

    @Override
    public void initializeStreaming() {
        log.info("Initializing streaming service for edge node: {}", edgeNodeConfig.getNodeId());
        // Initialize any required streaming infrastructure
    }

    @Override
    public void shutdownAllStreams() {
        log.info("Shutting down all streams");
        
        List<String> sessionIds = activeStreams.keySet().stream().collect(Collectors.toList());
        for (String sessionId : sessionIds) {
            stopStream(sessionId);
        }
        
        activeStreams.clear();
        streamMetrics.clear();
        streamHeartbeats.clear();
    }

    @Override
    public void processStreamHeartbeat(String sessionId) {
        streamHeartbeats.put(sessionId, LocalDateTime.now());
        
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        if (stream != null) {
            stream.setLastHeartbeat(LocalDateTime.now());
        }
    }

    @Override
    public Map<String, Object> getStreamHealth(String sessionId) {
        Map<String, Object> health = new HashMap<>();
        EdgeStreamDTO stream = activeStreams.get(sessionId);
        
        if (stream != null) {
            health.put("sessionId", sessionId);
            health.put("status", stream.getStatus());
            health.put("isActive", stream.isActive());
            health.put("lastHeartbeat", stream.getLastHeartbeat());
            health.put("connectionRetries", stream.getConnectionRetries());
            health.put("qualityLevel", stream.getQualityLevel());
            health.put("bitrate", stream.getBitrate());
            health.put("frameRate", stream.getFrameRate());
        } else {
            health.put("error", "Stream not found");
        }
        
        return health;
    }

    // Private helper methods
    
    private String generateSessionId() {
        return "edge_" + edgeNodeConfig.getNodeId() + "_" + UUID.randomUUID().toString();
    }

    private String generateLocalStreamUrl(String sessionId) {
        return String.format("rtmp://localhost:1935/live/%s", sessionId);
    }

    private int getQualityLevel(Map<String, Object> parameters) {
        if (parameters != null && parameters.containsKey("qualityLevel")) {
            return (Integer) parameters.get("qualityLevel");
        }
        return 3; // Default medium quality
    }

    private void initializeStreamMetrics(String sessionId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("sessionId", sessionId);
        metrics.put("startTime", LocalDateTime.now());
        metrics.put("bytesTransferred", 0L);
        metrics.put("packetsTransferred", 0L);
        metrics.put("errors", 0);
        streamMetrics.put(sessionId, metrics);
    }

    private void updateStreamParameters(EdgeStreamDTO stream, int qualityLevel) {
        // Adjust bitrate and frame rate based on quality level
        switch (qualityLevel) {
            case 1: // Low quality
                stream.setBitrate(500.0);
                stream.setFrameRate(15.0);
                stream.setResolution("640x480");
                break;
            case 2: // Medium-low quality
                stream.setBitrate(1000.0);
                stream.setFrameRate(20.0);
                stream.setResolution("720x576");
                break;
            case 3: // Medium quality
                stream.setBitrate(2000.0);
                stream.setFrameRate(25.0);
                stream.setResolution("1280x720");
                break;
            case 4: // High quality
                stream.setBitrate(4000.0);
                stream.setFrameRate(30.0);
                stream.setResolution("1920x1080");
                break;
            case 5: // Ultra quality
                stream.setBitrate(8000.0);
                stream.setFrameRate(30.0);
                stream.setResolution("3840x2160");
                break;
        }
    }

    /**
     * 真正执行流重建 - 停止旧会话，建立新连接
     */
    private void restartStream(EdgeStreamDTO stream) {
        Long cameraId = stream.getCameraId();
        log.info("Attempting to restart stream for camera: {}, attempt: {}",
                cameraId, stream.getConnectionRetries() + 1);

        try {
            // 1. 更新状态为 CONNECTING
            stream.setStatus(StreamSession.StreamStatus.CONNECTING);
            stream.setConnectionRetries(stream.getConnectionRetries() + 1);

            // 2. 获取摄像头信息
            Optional<EdgeCameraDTO> cameraOpt = edgeCameraService.getCameraById(cameraId);
            if (cameraOpt.isEmpty()) {
                log.error("Camera not found for restart: {}", cameraId);
                stream.setStatus(StreamSession.StreamStatus.ERROR);
                stream.setErrorMessage("Camera not found: " + cameraId);
                return;
            }

            EdgeCameraDTO camera = cameraOpt.get();

            // 3. 获取协议适配器并停止旧会话
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
            if (adapter == null) {
                log.error("No protocol adapter found for: {}", camera.getProtocol());
                stream.setStatus(StreamSession.StreamStatus.ERROR);
                stream.setErrorMessage("No protocol adapter for: " + camera.getProtocol());
                return;
            }

            // 停止旧会话
            if (stream.getSessionId() != null) {
                try {
                    adapter.stopStreamSession(stream.getSessionId());
                    log.debug("Stopped old session: {}", stream.getSessionId());
                } catch (Exception e) {
                    log.warn("Error stopping old session {}: {}", stream.getSessionId(), e.getMessage());
                }
            }

            // 4. 等待一小段时间（避免立即重连）
            Thread.sleep(1000L * stream.getConnectionRetries());

            // 5. 建立新的流会话
            Camera camModel = Camera.builder()
                    .id(camera.getId())
                    .name(camera.getName())
                    .location(camera.getLocation())
                    .connectionUrl(camera.getConnectionUrl())
                    .username(camera.getUsername())
                    .password(camera.getPassword())
                    .protocol(camera.getProtocol())
                    .resolution(stream.getResolution())
                    .bitrate((int) stream.getBitrate())
                    .frameRate((int) stream.getFrameRate())
                    .build();

            String newSessionId = adapter.startStreamSession(camModel);
            log.info("Started new stream session: {} for camera: {}", newSessionId, cameraId);

            // 6. 更新流状态
            stream.setSessionId(newSessionId);
            stream.setStatus(StreamSession.StreamStatus.STREAMING);
            stream.setActive(true);
            stream.setLastHeartbeat(LocalDateTime.now());
            stream.setErrorMessage(null);
            stream.setConnectionRetries(0); // 重置重试计数

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            stream.setStatus(StreamSession.StreamStatus.ERROR);
            stream.setErrorMessage("Restart interrupted");
            log.error("Restart interrupted for camera: {}", cameraId);
        } catch (Exception e) {
            log.error("Failed to restart stream for camera {}: {}", cameraId, e.getMessage());
            stream.setStatus(StreamSession.StreamStatus.ERROR);
            stream.setErrorMessage(e.getMessage());

            if (stream.getConnectionRetries() >= maxRetries) {
                log.warn("Max retries ({}) reached for camera: {}, giving up", maxRetries, cameraId);
            }
        }
    }

    private Camera convertToEntity(EdgeCameraDTO dto) {
        return Camera.builder()
                .id(dto.getId())
                .name(dto.getName())
                .location(dto.getLocation())
                .connectionUrl(dto.getConnectionUrl())
                .username(dto.getUsername())
                .password(dto.getPassword())
                .protocol(dto.getProtocol())
                .status(dto.getStatus())
                .resolution(dto.getResolution())
                .frameRate(dto.getFrameRate())
                .bitrate(dto.getBitrate())
                .enabled(dto.isEnabled())
                .lastActiveTime(dto.getLastActiveTime())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}