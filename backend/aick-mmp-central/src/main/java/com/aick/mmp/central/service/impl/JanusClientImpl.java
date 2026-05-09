package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.JanusConfig;
import com.aick.mmp.central.service.JanusClient;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janus Gateway 客户端实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JanusClientImpl implements JanusClient {

    private final JanusConfig janusConfig;
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    // Janus会话缓存
    private final Map<String, JanusSession> activeSessions = new ConcurrentHashMap<>();
    
    // Janus插件句柄缓存
    private final Map<String, JanusHandle> activeHandles = new ConcurrentHashMap<>();

    @Override
    public String createSession() {
        try {
            String url = janusConfig.getServerUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "create");
            body.put("transaction", generateTransactionId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            
            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
            
            if (jsonResponse.has("data") && jsonResponse.getAsJsonObject("data").has("id")) {
                String sessionId = jsonResponse.getAsJsonObject("data").get("id").getAsString();
                
                JanusSession session = new JanusSession();
                session.setSessionId(sessionId);
                session.setCreatedAt(System.currentTimeMillis());
                activeSessions.put(sessionId, session);
                
                log.info("Created Janus session: {}", sessionId);
                return sessionId;
            }
            
            throw new ServiceException("Failed to create Janus session");
            
        } catch (RestClientException e) {
            log.error("Failed to create Janus session: {}", e.getMessage());
            throw new ServiceException("Janus connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroySession(String sessionId) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "destroy");
            body.put("transaction", generateTransactionId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            // 清理会话相关句柄
            activeHandles.entrySet().removeIf(entry -> {
                if (entry.getValue().getSessionId().equals(sessionId)) {
                    return true;
                }
                return false;
            });
            
            activeSessions.remove(sessionId);
            log.info("Destroyed Janus session: {}", sessionId);
            
        } catch (RestClientException e) {
            log.error("Failed to destroy Janus session {}: {}", sessionId, e.getMessage());
            throw new ServiceException("Janus session destroy failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String createRtspPlugin(String sessionId, Camera camera) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "attach");
            body.put("plugin", "janus.plugin.rtsp");
            body.put("transaction", generateTransactionId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            
            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
            
            if (jsonResponse.has("data") && jsonResponse.getAsJsonObject("data").has("id")) {
                String handleId = jsonResponse.getAsJsonObject("data").get("id").getAsString();
                
                JanusHandle handle = new JanusHandle();
                handle.setHandleId(handleId);
                handle.setSessionId(sessionId);
                handle.setCameraId(camera.getId());
                handle.setCreatedAt(System.currentTimeMillis());
                activeHandles.put(handleId, handle);
                
                log.info("Created RTSP plugin handle: {} for camera {}", handleId, camera.getId());
                return handleId;
            }
            
            throw new ServiceException("Failed to create RTSP plugin handle");
            
        } catch (RestClientException e) {
            log.error("Failed to create RTSP plugin for camera {}: {}", camera.getId(), e.getMessage());
            throw new ServiceException("RTSP plugin creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> startRtspStream(String sessionId, String handleId, String rtspUrl) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "message");
            body.put("transaction", generateTransactionId());
            
            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("request", "watch");
            bodyContent.put("video", true);
            bodyContent.put("rtspurl", rtspUrl);
            
            // RTSP认证信息
            if (rtspUrl.contains("@")) {
                // 从URL中解析认证信息
                String authInfo = rtspUrl.substring(rtspUrl.indexOf("@") - 1);
                bodyContent.put("rtspuser", extractRtspUsername(rtspUrl));
                bodyContent.put("rtsppwd", extractRtspPassword(rtspUrl));
            }
            
            body.put("body", bodyContent);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            
            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
            
            // 解析响应
            Map<String, Object> result = new HashMap<>();
            
            if (jsonResponse.has("janus") && "success".equals(jsonResponse.get("janus").getAsString())) {
                result.put("success", true);
                
                if (jsonResponse.has("jsep")) {
                    JsonObject jsep = jsonResponse.getAsJsonObject("jsep");
                    result.put("sdpOffer", jsep.get("sdp").getAsString());
                    result.put("type", jsep.get("type").getAsString());
                }
                
                log.info("Started RTSP stream for handle {}", handleId);
            } else {
                result.put("success", false);
                if (jsonResponse.has("error")) {
                    result.put("error", jsonResponse.getAsJsonObject("error").get("reason").getAsString());
                }
            }
            
            return result;
            
        } catch (RestClientException e) {
            log.error("Failed to start RTSP stream for handle {}: {}", handleId, e.getMessage());
            throw new ServiceException("RTSP stream start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void processAnswer(String sessionId, String handleId, String answer) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "message");
            body.put("transaction", generateTransactionId());
            
            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("request", "start");
            body.put("body", bodyContent);
            
            // JSEP SDP Answer
            Map<String, Object> jsep = new HashMap<>();
            jsep.put("type", "answer");
            jsep.put("sdp", answer);
            body.put("jsep", jsep);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            // 更新句柄状态
            JanusHandle handle = activeHandles.get(handleId);
            if (handle != null) {
                handle.setStreaming(true);
            }
            
            log.info("Processed WebRTC answer for handle {}", handleId);
            
        } catch (RestClientException e) {
            log.error("Failed to process WebRTC answer for handle {}: {}", handleId, e.getMessage());
            throw new ServiceException("WebRTC answer processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void addIceCandidate(String sessionId, String handleId, Map<String, Object> candidate) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "trickle");
            body.put("transaction", generateTransactionId());
            body.put("candidate", candidate);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            log.debug("Added ICE candidate for handle {}", handleId);
            
        } catch (RestClientException e) {
            log.warn("Failed to add ICE candidate for handle {}: {}", handleId, e.getMessage());
            // 不抛出异常，因为ICE候选添加失败不应该中断流程
        }
    }

    @Override
    public void stopRtspStream(String sessionId, String handleId) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "message");
            body.put("transaction", generateTransactionId());
            
            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("request", "stop");
            body.put("body", bodyContent);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            // 更新句柄状态
            JanusHandle handle = activeHandles.get(handleId);
            if (handle != null) {
                handle.setStreaming(false);
            }
            
            log.info("Stopped RTSP stream for handle {}", handleId);
            
        } catch (RestClientException e) {
            log.error("Failed to stop RTSP stream for handle {}: {}", handleId, e.getMessage());
            throw new ServiceException("RTSP stream stop failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroyHandle(String sessionId, String handleId) {
        try {
            String url = janusConfig.getServerUrl() + "/" + sessionId + "/" + handleId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "detach");
            body.put("transaction", generateTransactionId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            activeHandles.remove(handleId);
            log.info("Destroyed handle: {}", handleId);
            
        } catch (RestClientException e) {
            log.error("Failed to destroy handle {}: {}", handleId, e.getMessage());
            throw new ServiceException("Handle destroy failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // 通过创建 Janus 会话来检测健康状态
            String url = janusConfig.getServerUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("janus", "create");
            body.put("transaction", generateTransactionId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // 如果创建成功，立即销毁这个测试会话
                try {
                    JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
                    if (jsonResponse.has("data") && jsonResponse.getAsJsonObject("data").has("id")) {
                        String sessionId = jsonResponse.getAsJsonObject("data").get("id").getAsString();
                        destroySession(sessionId);
                    }
                } catch (Exception ignored) {}
                return true;
            }
            return false;
            
        } catch (RestClientException e) {
            log.warn("Janus health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeSessions", activeSessions.size());
        info.put("activeHandles", activeHandles.size());
        info.put("configuredServerUrl", janusConfig.getServerUrl());
        return info;
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String extractRtspUsername(String rtspUrl) {
        // 格式: rtsp://username:password@host:port/path
        String auth = rtspUrl.substring(7, rtspUrl.indexOf("@")); // 去掉rtsp://
        return auth.split(":")[0];
    }

    private String extractRtspPassword(String rtspUrl) {
        String auth = rtspUrl.substring(7, rtspUrl.indexOf("@"));
        return auth.split(":")[1];
    }

    /**
     * Janus会话信息
     */
    private static class JanusSession {
        private String sessionId;
        private long createdAt;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Janus插件句柄信息
     */
    private static class JanusHandle {
        private String handleId;
        private String sessionId;
        private Long cameraId;
        private long createdAt;
        private boolean streaming;

        public String getHandleId() { return handleId; }
        public void setHandleId(String handleId) { this.handleId = handleId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Long getCameraId() { return cameraId; }
        public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public boolean isStreaming() { return streaming; }
        public void setStreaming(boolean streaming) { this.streaming = streaming; }
    }
}
