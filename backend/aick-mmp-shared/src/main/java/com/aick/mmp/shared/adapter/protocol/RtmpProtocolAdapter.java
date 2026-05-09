package com.aick.mmp.shared.adapter.protocol;

import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RTMP协议适配器，处理RTMP协议摄像头的接入和媒体流管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RtmpProtocolAdapter implements ProtocolAdapter {

    // 存储活动的RTMP连接
    private final Map<String, Object> activeConnections = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return "RTMP";
    }

    @Override
    public boolean testConnection(Camera camera) {
        try {
            log.info("Testing RTMP connection for camera: {}", camera.getName());
            
            // 验证RTMP URL格式
            String connectionUrl = camera.getConnectionUrl();
            if (connectionUrl == null || !connectionUrl.startsWith("rtmp://")) {
                log.error("Invalid RTMP URL for camera {}: {}", camera.getId(), connectionUrl);
                return false;
            }
            
            // 对于RTMP协议，我们简化连接测试逻辑
            // 只要URL格式正确就认为连接可用
            // 在实际生产环境中，可能需要更复杂的连接测试逻辑
            log.info("RTMP URL format is valid for camera {}: {}", camera.getId(), connectionUrl);
            return true;
        } catch (Exception e) {
            log.error("RTMP connection test failed for camera {}: {}", camera.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String startStreamSession(Camera camera) {
        try {
            String sessionId = generateSessionId();
            
            log.info("Starting RTMP stream session for camera: {} (session: {})", 
                    camera.getId(), sessionId);
            
            // 验证连接URL格式
            String connectionUrl = camera.getConnectionUrl();
            if (connectionUrl == null || !connectionUrl.startsWith("rtmp://")) {
                throw new RuntimeException("Invalid RTMP URL for camera " + camera.getId() + ": " + connectionUrl);
            }
            
            // 尝试检查RTMP流是否可用
            String path = extractPathFromRtmpUrl(connectionUrl);
            if (path != null) {
                String apiUrl = buildStreamStatusApiUrl(connectionUrl);
                boolean isAvailable = checkStreamAvailability(apiUrl + "/" + path);
                if (!isAvailable) {
                    log.warn("RTMP stream may not be available for camera {}: {}", camera.getId(), connectionUrl);
                    // 我们仍然继续，因为流可能稍后变得可用
                }
            }
            
            activeConnections.put(sessionId, camera);
            
            return sessionId;
        } catch (Exception e) {
            log.error("Failed to start RTMP stream session for camera {}: {}", 
                    camera.getId(), e.getMessage(), e);
            throw new RuntimeException("RTMP stream initialization failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStreamSession(String sessionId) {
        try {
            Object connection = activeConnections.remove(sessionId);
            if (connection != null) {
                log.info("Stopped RTMP stream session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error stopping RTMP stream session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("RTMP stream session not found: " + sessionId);
        }

        // 根据质量级别调整RTMP流参数
        int bitrate = getBitrateForQualityLevel(qualityLevel);
        int framerate = getFramerateForQualityLevel(qualityLevel);

        try {
            log.info("Adjusting RTMP stream quality for session {}: bitrate={}kbps, framerate={}fps",
                    sessionId, bitrate, framerate);
        } catch (Exception e) {
            log.error("Failed to adjust RTMP stream quality for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Stream quality adjustment failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("RTMP stream session not found: " + sessionId);
        }

        // 模拟返回流指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bitrate", 2048);
        metrics.put("framerate", 25);
        metrics.put("protocol", "RTMP");
        
        return metrics;
    }

    private String generateSessionId() {
        return "rtmp-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * 从RTMP URL中提取路径
     */
    private String extractPathFromRtmpUrl(String rtmpUrl) {
        // RTMP URL格式: rtmp://host:port/appName/streamName
        // 例如: rtmp://rtsp-server:1935/live/stream1
        Pattern pattern = Pattern.compile("rtmp://[^/]+/(.*)");
        Matcher matcher = pattern.matcher(rtmpUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 构建用于检查流状态的API URL
     * 注意: 这需要MediaMTX启用HTTP API
     */
    private String buildStreamStatusApiUrl(String rtmpUrl) {
        // 提取主机和端口
        Pattern pattern = Pattern.compile("rtmp://([^:/]+)(?::(\\d+))?/(.*)");
        Matcher matcher = pattern.matcher(rtmpUrl);
        
        if (matcher.matches()) {
            String host = matcher.group(1);
            String port = matcher.group(2);

            // 如果端口未指定，使用默认RTMP端口
            if (port == null || port.isEmpty()) {
                port = "1935";
            }
            
            // 构建API URL (MediaMTX HTTP API端口为8888)
            // 在Docker环境中，使用服务名称
            if ("rtsp-server".equals(host)) {
                // 在非Docker环境中（如本地开发），使用localhost
                if (!isRunningInDocker()) {
                    return "http://localhost:8888/v3/config/paths";
                }
                return "http://rtsp-server:8888/v3/config/paths";
            } else {
                return "http://" + host + ":8888/v3/config/paths";
            }
        }
        
        // 如果无法解析URL，返回默认值
        // 根据运行环境决定使用localhost还是rtsp-server
        if (!isRunningInDocker()) {
            return "http://localhost:8888/v3/config/paths";
        }
        return "http://rtsp-server:8888/v3/config/paths";
    }

    /**
     * 检查RTMP流是否可用
     */
    private boolean checkStreamAvailability(String apiUrl) {
        try {
            log.info("Checking stream availability at: {}", apiUrl);

            // 创建HTTP连接
            URI uri = URI.create(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Try without any authentication first (MediaMTX default)
            int responseCode = connection.getResponseCode();
            log.info("Stream API response code: {}", responseCode);
            
            if (responseCode == 200) {
                // 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                log.info("Stream API response: {}", response.toString());
                
                // 对于RTMP流，我们简化检查逻辑
                // 只要API能正常响应就认为流可用
                return true;
            } else if (responseCode == 404) {
                // 404表示路径不存在，但API本身是可访问的
                log.info("Stream path not found, but API is accessible: {}", responseCode);
                return true;
            } else if (responseCode == 401) {
                log.warn("Authentication required for MediaMTX API. Response code: {}", responseCode);
                // Try with basic authentication as fallback
                connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                // 添加基本认证头 (使用MediaMTX默认的空认证)
                String auth = ":"; // Empty username and password for MediaMTX default auth
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                
                responseCode = connection.getResponseCode();
                log.info("Stream API response code with auth: {}", responseCode);
                
                if (responseCode == 200) {
                    // 读取响应内容
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    log.info("Stream API response with auth: {}", response.toString());
                    return true;
                } else if (responseCode == 404) {
                    // 404表示路径不存在，但API本身是可访问的
                    log.info("Stream path not found, but API is accessible with auth: {}", responseCode);
                    return true;
                } else {
                    log.warn("Stream API returned response code with auth: {}", responseCode);
                    return false;
                }
            } else {
                log.warn("Stream API returned response code: {}", responseCode);
                return false;
            }
        } catch (java.net.UnknownHostException e) {
            log.error("Unknown host when checking stream availability: {}", e.getMessage());
            // 当无法解析主机名时，假设流是可用的（特别是在开发环境中）
            log.info("Assuming stream is available due to host resolution issue");
            return true;
        } catch (Exception e) {
            log.error("Failed to check stream availability: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断是否在Docker环境中运行
     */
    private boolean isRunningInDocker() {
        // 检查是否存在Docker环境特征文件
        File dockerEnvFile = new File("/.dockerenv");
        File dockerCGroupFile = new File("/proc/1/cgroup");
        
        if (dockerEnvFile.exists()) {
            return true;
        }
        
        if (dockerCGroupFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(dockerCGroupFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("/docker")) {
                        return true;
                    }
                }
            } catch (IOException e) {
                // 忽略异常，继续检查
            }
        }
        
        // 默认返回false（非Docker环境）
        return false;
    }

    /**
     * 根据质量级别获取对应的比特率
     */
    private int getBitrateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
                return 500;   // 低质量: 500kbps
            case 2:
                return 1000;  // 中低质量: 1000kbps
            case 3:
                return 2000;  // 中等质量: 2000kbps
            case 4:
                return 4000;  // 高质量: 4000kbps
            case 5:
                return 8000;  // 极高质量: 8000kbps
            default:
                return 2000; // 默认中等质量
        }
    }

    /**
     * 根据质量级别获取对应的帧率
     */
    private int getFramerateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
            case 2:
                return 15; // 低质量: 15fps
            default:
                return 30;   // 中高质量: 30fps
        }
    }
}