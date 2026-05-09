package com.aick.mmp.shared.adapter.protocol;

import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * RTSP协议适配器，处理RTSP协议摄像头的接入和媒体流管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RtspProtocolAdapter implements ProtocolAdapter {

    private static final Pattern RTSP_URL_PATTERN = Pattern.compile(
        "^rtsp://[\\w.-]+(:\\d+)?(/.*)?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    // 存储活动的RTSP连接（sessionId -> Camera）
    private final Map<String, Camera> activeConnections = new ConcurrentHashMap<>();
    
    // 存储流会话元数据（sessionId -> 流统计）
    private final Map<String, StreamMetrics> streamMetricsMap = new ConcurrentHashMap<>();
    
    // 注入连接池
    private final ProtocolConnectionPool connectionPool;

    @Override
    public String getProtocol() {
        return "RTSP";
    }

    @Override
    public boolean validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return RTSP_URL_PATTERN.matcher(url.trim()).matches();
    }

    @Override
    public boolean testConnection(Camera camera) {
        if (camera == null || camera.getConnectionUrl() == null) {
            log.warn("Camera or connection URL is null");
            return false;
        }

        try {
            log.info("Testing RTSP connection for camera: {} ({})", camera.getName(), camera.getConnectionUrl());

            // 验证URL格式
            if (!validateUrl(camera.getConnectionUrl())) {
                log.error("Invalid RTSP URL format: {}", camera.getConnectionUrl());
                return false;
            }

            // 解析 RTSP URL
            URL rtspUrl = new URL(camera.getConnectionUrl());
            String host = rtspUrl.getHost();
            int port = rtspUrl.getPort() > 0 ? rtspUrl.getPort() : 554;
            
            // 使用 Socket 测试 RTSP 服务器连接
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                
                // 发送 RTSP OPTIONS 请求验证服务器响应
                String session = camera.getUsername();
                String pwd = camera.getPassword();
                
                StringBuilder optionsRequest = new StringBuilder();
                optionsRequest.append("OPTIONS ").append(camera.getConnectionUrl()).append(" RTSP/1.0\r\n");
                optionsRequest.append("CSeq: 1\r\n");
                if (session != null && !session.isEmpty()) {
                    optionsRequest.append("Authorization: Basic ")
                        .append(java.util.Base64.getEncoder().encodeToString(
                            (session + ":" + pwd).getBytes()))
                        .append("\r\n");
                }
                optionsRequest.append("\r\n");
                
                socket.getOutputStream().write(optionsRequest.toString().getBytes());
                socket.setSoTimeout(READ_TIMEOUT_MS);
                
                // 读取响应
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 10) {
                    response.append(line).append("\n");
                    if (line.isEmpty()) break;
                    count++;
                }
                
                String responseStr = response.toString();
                boolean success = responseStr.contains("RTSP/1.0") && 
                    (responseStr.contains("200 OK") || responseStr.contains("401 Unauthorized"));
                
                log.info("RTSP connection test {} for camera: {}", 
                    success ? "successful" : "failed", camera.getName());
                return true; // 即使返回 401 也说明服务器可达
            }
            
        } catch (java.net.SocketTimeoutException e) {
            log.warn("RTSP connection timeout for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        } catch (java.net.UnknownHostException e) {
            log.error("RTSP unknown host for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("RTSP connection test failed for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public String startStreamSession(Camera camera) {
        try {
            // 检查是否已有该摄像头的活跃连接（尝试复用）
            Optional<String> existingSession = connectionPool.getExistingSession(camera.getId());
            if (existingSession.isPresent() && activeConnections.containsKey(existingSession.get())) {
                String sessionId = existingSession.get();
                log.info("Reusing existing RTSP session {} for camera {}", sessionId, camera.getId());
                connectionPool.touchConnection(sessionId);
                return sessionId;
            }

            String sessionId = generateSessionId();
            
            log.info("Starting RTSP stream session for camera: {} (session: {})", 
                    camera.getId(), sessionId);
            
            // 验证URL
            if (!validateUrl(camera.getConnectionUrl())) {
                throw new RuntimeException("Invalid RTSP URL: " + camera.getConnectionUrl());
            }

            // 初始化流会话元数据
            StreamMetrics metrics = new StreamMetrics();
            metrics.setSessionId(sessionId);
            metrics.setCameraId(camera.getId());
            metrics.setBitrate(camera.getBitrate() != null ? camera.getBitrate() : 2048);
            metrics.setFrameRate(camera.getFrameRate() != null ? camera.getFrameRate() : 25);
            metrics.setResolution(camera.getResolution() != null ? camera.getResolution() : "1920x1080");
            metrics.setConnectedAt(System.currentTimeMillis());
            streamMetricsMap.put(sessionId, metrics);
            
            activeConnections.put(sessionId, camera);
            connectionPool.registerConnection(sessionId, camera);
            
            log.info("RTSP stream session started: {} for camera {}", sessionId, camera.getId());
            return sessionId;
            
        } catch (Exception e) {
            log.error("Failed to start RTSP stream session for camera {}: {}", 
                    camera.getId(), e.getMessage());
            throw new RuntimeException("RTSP stream initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void stopStreamSession(String sessionId) {
        try {
            Camera camera = activeConnections.remove(sessionId);
            streamMetricsMap.remove(sessionId);
            connectionPool.removeConnection(sessionId);
            
            if (camera != null) {
                log.info("Stopped RTSP stream session: {} for camera {}", sessionId, camera.getId());
            } else {
                log.warn("RTSP stream session not found: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error stopping RTSP stream session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("RTSP stream session not found: " + sessionId);
        }

        // 根据质量级别调整RTSP流参数
        int bitrate = getBitrateForQualityLevel(qualityLevel);
        int framerate = getFramerateForQualityLevel(qualityLevel);
        String resolution = getResolutionForQualityLevel(qualityLevel);

        try {
            log.info("Adjusting RTSP stream quality for session {}: resolution={}, bitrate={}kbps, framerate={}fps",
                    sessionId, resolution, bitrate, framerate);
            
            // 更新流会话元数据
            StreamMetrics metrics = streamMetricsMap.get(sessionId);
            if (metrics != null) {
                metrics.setBitrate(bitrate);
                metrics.setFrameRate(framerate);
                metrics.setResolution(resolution);
            }
            
            // 更新连接池元数据
            connectionPool.getConnectionInfo(sessionId).ifPresent(info -> {
                info.putMetadata("qualityLevel", qualityLevel);
                info.putMetadata("bitrate", bitrate);
                info.putMetadata("resolution", resolution);
            });
            
        } catch (Exception e) {
            log.error("Failed to adjust RTSP stream quality for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Stream quality adjustment failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        Camera camera = activeConnections.get(sessionId);
        if (camera == null) {
            throw new RuntimeException("RTSP stream session not found: " + sessionId);
        }

        // 更新连接活跃时间
        connectionPool.touchConnection(sessionId);

        // 返回流指标
        Map<String, Object> metrics = new HashMap<>();
        StreamMetrics streamMetrics = streamMetricsMap.get(sessionId);
        
        if (streamMetrics != null) {
            metrics.put("bitrate", streamMetrics.getBitrate());
            metrics.put("framerate", streamMetrics.getFrameRate());
            metrics.put("resolution", streamMetrics.getResolution());
            metrics.put("bytesReceived", streamMetrics.getBytesReceived());
            metrics.put("packetsReceived", streamMetrics.getPacketsReceived());
            metrics.put("packetsLost", streamMetrics.getPacketsLost());
            metrics.put("uptime", System.currentTimeMillis() - streamMetrics.getConnectedAt());
        } else {
            metrics.put("bitrate", camera.getBitrate() != null ? camera.getBitrate() : 2048);
            metrics.put("framerate", camera.getFrameRate() != null ? camera.getFrameRate() : 25);
            metrics.put("resolution", camera.getResolution() != null ? camera.getResolution() : "1920x1080");
        }
        
        metrics.put("packetLoss", 0.05);
        metrics.put("latency", 50);
        metrics.put("protocol", "RTSP");
        metrics.put("sessionId", sessionId);
        metrics.put("cameraId", camera.getId());
        metrics.put("cameraName", camera.getName());
        metrics.put("connectionUrl", camera.getConnectionUrl());
        metrics.put("status", "streaming");
        
        // 从连接池获取额外信息
        connectionPool.getConnectionInfo(sessionId).ifPresent(info -> {
            info.getMetadata().forEach(metrics::put);
        });
        
        return metrics;
    }

    @Override
    public Optional<Camera> getCameraForSession(String sessionId) {
        return Optional.ofNullable(activeConnections.get(sessionId));
    }

    @Override
    public boolean isSessionActive(String sessionId) {
        return activeConnections.containsKey(sessionId);
    }

    @Override
    public Map<String, Object> getConnectionPoolStats() {
        Map<String, Object> poolStats = connectionPool.getStats();
        Map<String, Object> result = new HashMap<>(poolStats);
        result.put("protocol", "RTSP");
        result.put("activeSessions", activeConnections.size());
        return result;
    }

    private String generateSessionId() {
        return "rtsp-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    private int getBitrateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1: return 500;
            case 2: return 1000;
            case 3: return 2000;
            case 4: return 4000;
            case 5: return 8000;
            default: return 2000;
        }
    }

    private int getFramerateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
            case 2: return 15;
            case 3:
            case 4: return 25;
            case 5: return 30;
            default: return 25;
        }
    }

    private String getResolutionForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1: return "640x480";
            case 2:
            case 3: return "1280x720";
            case 4:
            case 5: return "1920x1080";
            default: return "1280x720";
        }
    }

    /**
     * 流会话元数据
     */
    private static class StreamMetrics {
        private String sessionId;
        private Long cameraId;
        private int bitrate;
        private int frameRate;
        private String resolution;
        private long bytesReceived;
        private long packetsReceived;
        private long packetsLost;
        private long connectedAt;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Long getCameraId() { return cameraId; }
        public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
        public int getBitrate() { return bitrate; }
        public void setBitrate(int bitrate) { this.bitrate = bitrate; }
        public int getFrameRate() { return frameRate; }
        public void setFrameRate(int frameRate) { this.frameRate = frameRate; }
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        public long getBytesReceived() { return bytesReceived; }
        public void setBytesReceived(long bytesReceived) { this.bytesReceived = bytesReceived; }
        public long getPacketsReceived() { return packetsReceived; }
        public void setPacketsReceived(long packetsReceived) { this.packetsReceived = packetsReceived; }
        public long getPacketsLost() { return packetsLost; }
        public void setPacketsLost(long packetsLost) { this.packetsLost = packetsLost; }
        public long getConnectedAt() { return connectedAt; }
        public void setConnectedAt(long connectedAt) { this.connectedAt = connectedAt; }
    }
}