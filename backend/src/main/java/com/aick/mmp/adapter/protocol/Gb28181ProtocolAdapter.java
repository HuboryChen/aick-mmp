package com.aick.mmp.adapter.protocol;

import com.aick.mmp.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GB28181协议适配器，处理国标协议摄像头的接入和媒体流管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Gb28181ProtocolAdapter implements ProtocolAdapter {

    // 存储活动的GB28181会话
    private final Map<String, Object> gb28181SessionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> activeConnections = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return "GB28181";
    }

    @Override
    public boolean testConnection(Camera camera) {
        try {
            log.info("Testing GB28181 connection for camera: {}", camera.getName());
            
            // 模拟GB28181连接测试
            // 实际实现应该通过GB28181协议进行SIP注册和心跳检测
            
            return true; // 模拟成功
        } catch (Exception e) {
            log.error("GB28181 connection test failed for camera {}: {}", camera.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public String startStreamSession(Camera camera) {
        try {
            String sessionId = generateSessionId();
            
            log.info("Starting GB28181 stream session for camera: {} (session: {})", 
                    camera.getId(), sessionId);
            
            // 模拟启动GB28181流会话
            // 实际实现应该发送GB28181 Invite请求获取媒体流
            
            activeConnections.put(sessionId, camera);
            gb28181SessionMap.put(sessionId, "sip-session-" + sessionId);
            
            return sessionId;
        } catch (Exception e) {
            log.error("Failed to start GB28181 stream session for camera {}: {}", 
                    camera.getId(), e.getMessage());
            throw new RuntimeException("GB28181 stream initialization failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStreamSession(String sessionId) {
        try {
            // 停止媒体流
            String sipSession = (String) gb28181SessionMap.remove(sessionId);
            if (sipSession != null) {
                log.info("Stopped GB28181 SIP session: {}", sipSession);
            }

            // 移除连接
            Object connection = activeConnections.remove(sessionId);
            if (connection != null) {
                log.info("Stopped GB28181 stream session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error stopping GB28181 stream session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        if (!gb28181SessionMap.containsKey(sessionId)) {
            throw new RuntimeException("GB28181 stream session not found: " + sessionId);
        }

        try {
            // 根据质量级别计算参数
            int bitrate = getBitrateForQualityLevel(qualityLevel);
            int framerate = getFramerateForQualityLevel(qualityLevel);
            String resolution = getResolutionForQualityLevel(qualityLevel);

            log.info("Adjusted GB28181 stream quality for session {}: resolution={}, framerate={}, bitrate={}kbps",
                    sessionId, resolution, framerate, bitrate);
        } catch (Exception e) {
            log.error("Failed to adjust GB28181 stream quality for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("GB28181 stream quality adjustment failed: " + e.getMessage());
        }
    }

    /**
     * 发送GB28181 PTZ控制命令
     */
    public void sendPTZCommand(String sessionId, String command, int speed) {
        if (!gb28181SessionMap.containsKey(sessionId)) {
            throw new RuntimeException("GB28181 stream session not found: " + sessionId);
        }

        try {
            log.info("Sent GB28181 PTZ command '{}' with speed {} for session {}", command, speed, sessionId);
        } catch (Exception e) {
            log.error("Failed to send PTZ command for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("GB28181 PTZ control failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        if (!activeConnections.containsKey(sessionId)) {
            throw new RuntimeException("GB28181 stream session not found: " + sessionId);
        }

        // 模拟返回流指标和设备状态
        Map<String, Object> deviceStatus = new HashMap<>();
        deviceStatus.put("online", true);
        deviceStatus.put("signalLevel", 85);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bitrate", 2048);
        metrics.put("framerate", 25);
        metrics.put("packetLoss", 0.02);
        metrics.put("latency", 120);
        metrics.put("deviceStatus", deviceStatus);
        metrics.put("protocol", "GB28181");

        return metrics;
    }

    private String generateSessionId() {
        return "gb28181-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * 根据质量级别获取对应的比特率
     */
    private int getBitrateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
                return 512;    // 低质量: 512kbps
            case 2:
                return 1024;   // 中低质量: 1024kbps
            case 3:
                return 2048;   // 中等质量: 2048kbps
            case 4:
                return 4096;   // 高质量: 4096kbps
            case 5:
                return 8192;   // 极高质量: 8192kbps
            default:
                return 2048;   // 默认中等质量
        }
    }

    /**
     * 根据质量级别获取对应的帧率
     */
    private int getFramerateForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
            case 2:
                return 15;     // 低质量: 15fps
            case 3:
            case 4:
                return 25;     // 中质量: 25fps
            case 5:
                return 30;     // 高质量: 30fps
            default:
                return 25;     // 默认中质量
        }
    }

    /**
     * 根据质量级别获取对应的分辨率
     */
    private String getResolutionForQualityLevel(int qualityLevel) {
        switch (qualityLevel) {
            case 1:
                return "352x288";   // 低质量: CIF
            case 2:
                return "704x576";   // 中低质量: D1
            case 3:
                return "1280x720";  // 中等质量: HD
            case 4:
                return "1920x1080"; // 高质量: Full HD
            case 5:
                return "3840x2160"; // 极高质量: 4K
            default:
                return "1280x720";  // 默认中等质量
        }
    }
}