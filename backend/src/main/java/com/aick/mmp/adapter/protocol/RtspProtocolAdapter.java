package com.aick.mmp.adapter.protocol;

import com.aick.mmp.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTSP协议适配器，处理RTSP协议摄像头的接入和媒体流管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RtspProtocolAdapter implements ProtocolAdapter {

    // 存储活动的RTSP连接
    private final Map<String, Object> activeConnections = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return "RTSP";
    }

    @Override
    public boolean testConnection(Camera camera) {
        try {
            log.info("Testing RTSP connection for camera: {}", camera.getName());
            
            // 模拟RTSP连接测试
            // 实际实现应该使用RTSP客户端库进行连接测试
            
            return true; // 模拟成功
        } catch (Exception e) {
            log.error("RTSP connection test failed for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public String startStreamSession(Camera camera) {
        try {
            String sessionId = generateSessionId();
            
            log.info("Starting RTSP stream session for camera: {} (session: {})", 
                    camera.getId(), sessionId);
            
            // 模拟启动RTSP流会话
            activeConnections.put(sessionId, camera);
            
            return sessionId;
        } catch (Exception e) {
            log.error("Failed to start RTSP stream session for camera {}: {}", 
                    camera.getId(), e.getMessage());
            throw new RuntimeException("RTSP stream initialization failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStreamSession(String sessionId) {
        try {
            Object connection = activeConnections.remove(sessionId);
            if (connection != null) {
                log.info("Stopped RTSP stream session: {}", sessionId);
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

        try {
            log.info("Adjusting RTSP stream quality for session {}: bitrate={}kbps, framerate={}fps",
                    sessionId, bitrate, framerate);
        } catch (Exception e) {
            log.error("Failed to adjust RTSP stream quality for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Stream quality adjustment failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("RTSP stream session not found: " + sessionId);
        }

        // 模拟返回流指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bitrate", 2048);
        metrics.put("framerate", 25);
        metrics.put("packetLoss", 0.05);
        metrics.put("latency", 50);
        metrics.put("protocol", "RTSP");
        
        return metrics;
    }

    private String generateSessionId() {
        return "rtsp-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
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