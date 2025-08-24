package com.aick.mmp.adapter.protocol;

import com.aick.mmp.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnvifProtocolAdapter implements ProtocolAdapter {

    private final Map<String, Object> activeConnections = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return "ONVIF";
    }

    @Override
    public boolean testConnection(Camera camera) {
        try {
            log.info("Testing ONVIF connection for camera: {}", camera.getName());
            
            // 模拟ONVIF连接测试
            // 实际实现应该使用ONVIF客户端库进行设备发现和连接测试
            
            return true; // 模拟成功
        } catch (Exception e) {
            log.error("ONVIF connection test failed for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public String startStreamSession(Camera camera) {
        try {
            String sessionId = generateSessionId();
            
            log.info("Starting ONVIF stream session for camera: {} (session: {})", 
                    camera.getId(), sessionId);
            
            // 模拟启动ONVIF流会话
            // 实际实现应该通过ONVIF协议获取媒体配置和流URI
            
            activeConnections.put(sessionId, camera);
            
            return sessionId;
        } catch (Exception e) {
            log.error("Failed to start ONVIF stream session for camera {}: {}", 
                    camera.getId(), e.getMessage());
            throw new RuntimeException("ONVIF stream initialization failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStreamSession(String sessionId) {
        try {
            Object connection = activeConnections.remove(sessionId);
            if (connection != null) {
                log.info("Stopped ONVIF stream session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error stopping ONVIF stream session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("ONVIF stream session not found: " + sessionId);
        }

        try {
            // 根据质量级别计算参数
            int bitrate = getBitrateForQualityLevel(qualityLevel);
            int framerate = getFramerateForQualityLevel(qualityLevel);
            String resolution = getResolutionForQualityLevel(qualityLevel);

            log.info("Adjusted ONVIF stream quality for session {}: resolution={}, framerate={}, bitrate={}kbps",
                    sessionId, resolution, framerate, bitrate);
        } catch (Exception e) {
            log.error("Failed to adjust ONVIF stream quality for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("ONVIF stream quality adjustment failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("ONVIF stream session not found: " + sessionId);
        }

        // 模拟返回流指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bitrate", 2048);
        metrics.put("framerate", 25);
        metrics.put("packetLoss", 0.1);
        metrics.put("latency", 100);
        metrics.put("protocol", "ONVIF");

        return metrics;
    }

    private String generateSessionId() {
        return "onvif-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private int getBitrateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
                return 512;    // 低质量
            case 2:
                return 1024;   // 中低质量
            case 3:
                return 2048;   // 中等质量
            case 4:
                return 4096;   // 高质量
            case 5:
                return 8192;   // 极高质量
            default:
                return 2048;   // 默认中等质量
        }
    }

    private int getFramerateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
            case 2:
                return 15;     // 低质量
            case 3:
            case 4:
                return 25;     // 中质量
            case 5:
                return 30;     // 高质量
            default:
                return 25;     // 默认
        }
    }

    private String getResolutionForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
                return "640x480";
            case 2:
                return "1280x720";
            case 3:
                return "1280x720";
            case 4:
                return "1920x1080";
            case 5:
                return "1920x1080";
            default:
                return "1280x720";
        }
    }
}