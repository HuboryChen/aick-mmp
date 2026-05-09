package com.aick.mmp.central.service.impl;

import com.aick.mmp.shared.adapter.protocol.ProtocolAdapter;
import com.aick.mmp.shared.adapter.protocol.ProtocolAdapterFactory;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.StreamSession;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.StreamSessionRepository;
import com.aick.mmp.central.service.JanusClient;
import com.aick.mmp.central.service.RecordingNotificationService;
import com.aick.mmp.central.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流媒体服务实现类
 * 负责视频流的启动、停止、管理和WebRTC信令处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingServiceImpl implements StreamingService {

    private static final int MAX_CONCURRENT_STREAMS = 16;
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private final StreamSessionRepository streamSessionRepository;
    private final CameraRepository cameraRepository;
    private final ProtocolAdapterFactory protocolAdapterFactory;
    private final JanusClient janusClient;
    private final RecordingNotificationService recordingNotificationService;

    // 存储活动的流会话（cameraId -> StreamSession）
    private final Map<Long, StreamSession> activeStreams = new ConcurrentHashMap<>();
    
    // 存储WebRTC会话信息（sessionId -> WebRTCSession）
    private final Map<String, WebRTCSession> webrtcSessions = new ConcurrentHashMap<>();

    @Value("${janus.server.url:http://localhost:8088}")
    private String janusServerUrl;

    // ==================== 基础流管理 ====================

    @Override
    @Transactional
    public String startStream(Camera camera) {
        if (camera == null) {
            throw new ServiceException("Camera cannot be null");
        }

        // 检查并发流数量限制
        if (activeStreams.size() >= MAX_CONCURRENT_STREAMS) {
            throw new ServiceException("Concurrent video stream limit exceeded (max " + MAX_CONCURRENT_STREAMS + ")");
        }

        // 检查摄像头状态
        if (camera.getStatus() == Camera.CameraStatus.OFFLINE) {
            throw new ServiceException("Camera is offline, cannot start stream");
        }

        try {
            // 获取协议适配器
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(camera.getProtocol().name());
            
            // 启动协议层流会话
            String protocolSessionId = adapter.startStreamSession(camera);
            
            // 创建流会话
            String sessionId = generateSessionId();
            StreamSession session = StreamSession.builder()
                    .sessionId(sessionId)
                    .cameraId(camera.getId())
                    .protocol(camera.getProtocol().name())
                    .status(StreamSession.StreamStatus.CONNECTING)
                    .startTime(LocalDateTime.now())
                    .resolution(camera.getResolution())
                    .bitrate(camera.getBitrate())
                    .frameRate(camera.getFrameRate())
                    .build();

            // 保存到数据库
            streamSessionRepository.save(session);
            
            // 添加到活跃流
            activeStreams.put(camera.getId(), session);

            log.info("Started stream for camera {} with session {}", camera.getId(), sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("Failed to start stream for camera {}: {}", camera.getId(), e.getMessage());
            throw new ServiceException("Stream start failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void stopStream(String sessionId) {
        StreamSession session = streamSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ServiceException("Stream session not found: " + sessionId));

        try {
            // 停止协议层流会话
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(session.getProtocol());
            adapter.stopStreamSession(sessionId);

            // 更新会话状态
            session.setStatus(StreamSession.StreamStatus.DISCONNECTED);
            session.setEndTime(LocalDateTime.now());
            streamSessionRepository.save(session);

            // 从活跃流移除
            activeStreams.remove(session.getCameraId());
            
            // 清理WebRTC会话
            webrtcSessions.remove(sessionId);

            log.info("Stopped stream session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to stop stream {}: {}", sessionId, e.getMessage());
            throw new ServiceException("Stream stop failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        StreamSession session = streamSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ServiceException("Stream session not found: " + sessionId));

        try {
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(session.getProtocol());
            return adapter.getStreamMetrics(sessionId);
        } catch (Exception e) {
            log.error("Failed to get stream metrics for {}: {}", sessionId, e.getMessage());
            Map<String, Object> fallbackMetrics = new HashMap<>();
            fallbackMetrics.put("sessionId", sessionId);
            fallbackMetrics.put("cameraId", session.getCameraId());
            fallbackMetrics.put("status", session.getStatus());
            fallbackMetrics.put("error", e.getMessage());
            return fallbackMetrics;
        }
    }

    @Override
    @Transactional
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        StreamSession session = streamSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ServiceException("Stream session not found: " + sessionId));

        if (qualityLevel < 1 || qualityLevel > 5) {
            throw new ServiceException("Quality level must be between 1 and 5");
        }

        try {
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(session.getProtocol());
            adapter.adjustStreamQuality(sessionId, qualityLevel);
            
            log.info("Adjusted stream quality for session {} to level {}", sessionId, qualityLevel);
        } catch (Exception e) {
            log.error("Failed to adjust stream quality for {}: {}", sessionId, e.getMessage());
            throw new ServiceException("Stream quality adjustment failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StreamSession> getActiveSessions() {
        return streamSessionRepository.findByStatus(StreamSession.StreamStatus.STREAMING);
    }

    // ==================== 按摄像头ID的流管理 ====================

    @Override
    @Transactional
    public StreamSession startStream(Long cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ServiceException("Camera not found: " + cameraId));

        // 检查是否已有活跃流
        if (activeStreams.containsKey(cameraId)) {
            StreamSession existing = activeStreams.get(cameraId);
            log.info("Stream already active for camera {}, returning existing session {}", 
                    cameraId, existing.getSessionId());
            return existing;
        }

        // 启动新流
        String sessionId = startStream(camera);
        
        StreamSession session = streamSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ServiceException("Session not found after creation"));
        session.setStatus(StreamSession.StreamStatus.STREAMING);
        streamSessionRepository.save(session);
        
        activeStreams.put(cameraId, session);
        
        return session;
    }

    @Override
    @Transactional
    public void stopStream(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            log.warn("No active stream found for camera {}", cameraId);
            return;
        }
        
        stopStream(session.getSessionId());
    }

    @Override
    public void pauseStream(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }

        try {
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(session.getProtocol());
            adapter.pauseStreamSession(session.getSessionId());
            
            session.setStatus(StreamSession.StreamStatus.PAUSED);
            streamSessionRepository.save(session);
            
            log.info("Paused stream for camera {}", cameraId);
        } catch (UnsupportedOperationException e) {
            log.warn("Pause not supported for protocol {}", session.getProtocol());
            throw new ServiceException("Pause not supported for this protocol");
        }
    }

    @Override
    public void resumeStream(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }

        if (session.getStatus() != StreamSession.StreamStatus.PAUSED) {
            throw new ServiceException("Stream is not paused");
        }

        try {
            ProtocolAdapter adapter = protocolAdapterFactory.getAdapter(session.getProtocol());
            adapter.resumeStreamSession(session.getSessionId());
            
            session.setStatus(StreamSession.StreamStatus.STREAMING);
            streamSessionRepository.save(session);
            
            log.info("Resumed stream for camera {}", cameraId);
        } catch (UnsupportedOperationException e) {
            log.warn("Resume not supported for protocol {}", session.getProtocol());
            throw new ServiceException("Resume not supported for this protocol");
        }
    }

    @Override
    public Map<String, Object> getStreamStatus(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("cameraId", cameraId);
        
        if (session == null) {
            status.put("active", false);
            status.put("status", "NO_STREAM");
            return status;
        }
        
        status.put("active", true);
        status.put("sessionId", session.getSessionId());
        status.put("status", session.getStatus());
        status.put("protocol", session.getProtocol());
        status.put("startTime", session.getStartTime());
        
        // 获取详细指标
        try {
            Map<String, Object> metrics = getStreamMetrics(session.getSessionId());
            status.putAll(metrics);
        } catch (Exception e) {
            log.warn("Failed to get stream metrics for camera {}: {}", cameraId, e.getMessage());
        }
        
        return status;
    }

    // ==================== WebRTC 支持 ====================

    @Override
    public String generateWebRtcOffer(Long cameraId) {
        // 确保流已启动
        StreamSession session = startStream(cameraId);
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ServiceException("Camera not found: " + cameraId));
        
        try {
            // 获取或创建 Janus 会话
            String janusSessionId = getOrCreateJanusSession(cameraId);
            
            // 获取或创建 RTSP 插件句柄
            String handleId = getOrCreateRtspHandle(janusSessionId, camera);
            
            // 获取 RTSP 流地址
            String rtspUrl = getRtspUrl(camera);
            
            // 调用 Janus Gateway 启动 RTSP 流并获取 SDP Offer
            Map<String, Object> result = janusClient.startRtspStream(janusSessionId, handleId, rtspUrl);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                String sdpOffer = (String) result.get("sdpOffer");
                
                // 保存 WebRTC 会话信息
                WebRTCSession webrtcSession = new WebRTCSession();
                webrtcSession.setSessionId(session.getSessionId());
                webrtcSession.setJanusSessionId(janusSessionId);
                webrtcSession.setHandleId(handleId);
                webrtcSession.setCreatedAt(System.currentTimeMillis());
                webrtcSessions.put(session.getSessionId(), webrtcSession);
                
                log.info("Generated WebRTC offer for camera {} via Janus", cameraId);
                return sdpOffer;
            } else {
                String error = (String) result.get("error");
                throw new ServiceException("Failed to start RTSP stream: " + error);
            }
            
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate WebRTC offer for camera {}: {}", cameraId, e.getMessage());
            throw new ServiceException("WebRTC offer generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String processWebRtcAnswer(Long cameraId, String answer) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }
        
        try {
            WebRTCSession webrtcSession = webrtcSessions.get(session.getSessionId());
            if (webrtcSession == null) {
                throw new ServiceException("No WebRTC session found for camera: " + cameraId);
            }
            
            // 将 Answer 发送给 Janus Gateway
            janusClient.processAnswer(webrtcSession.getJanusSessionId(), webrtcSession.getHandleId(), answer);
            
            webrtcSession.setAnswerReceived(true);
            webrtcSession.setAnswerReceivedAt(System.currentTimeMillis());
            
            // 更新会话状态
            session.setStatus(StreamSession.StreamStatus.STREAMING);
            streamSessionRepository.save(session);
            
            log.info("Processed WebRTC answer for camera {}", cameraId);
            return "SUCCESS";
            
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process WebRTC answer for camera {}: {}", cameraId, e.getMessage());
            throw new ServiceException("WebRTC answer processing failed: " + e.getMessage(), e);
        }
    }

    // ==================== 视频质量调整 ====================

    @Override
    @Transactional
    public void updateStreamQuality(Long cameraId, String resolution, int bitrate, int frameRate) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }

        // 计算质量级别
        int qualityLevel = calculateQualityLevel(resolution, bitrate, frameRate);
        
        // 调整质量
        adjustStreamQuality(session.getSessionId(), qualityLevel);
        
        // 更新会话信息
        session.setResolution(resolution);
        session.setBitrate(bitrate);
        session.setFrameRate(frameRate);
        streamSessionRepository.save(session);
        
        log.info("Updated stream quality for camera {}: resolution={}, bitrate={}, framerate={}", 
                cameraId, resolution, bitrate, frameRate);
    }

    // ==================== 录像相关 ====================

    @Override
    public String getStreamRecordingUrl(Long cameraId, String startTime, String endTime) {
        log.info("Getting recording URL for camera {} from {} to {}", cameraId, startTime, endTime);

        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ServiceException("Camera not found: " + cameraId));

        // 解析时间参数
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);

        if (start == null || end == null) {
            throw new ServiceException("Invalid time format. Expected ISO format like 2026-04-08T10:00:00");
        }

        if (start.isAfter(end)) {
            throw new ServiceException("Start time must be before end time");
        }

        // 通过录像通知服务查询录像回放 URL
        String playbackUrl = recordingNotificationService.queryRecordingPlaybackUrl(
                cameraId,
                camera.getName(),
                start,
                end
        );

        log.info("Retrieved recording URL for camera {}: {}", cameraId, playbackUrl);
        return playbackUrl;
    }

    /**
     * 解析时间字符串为 LocalDateTime
     */
    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        try {
            // 尝试 ISO 格式
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            try {
                // 尝试带空格格式
                return LocalDateTime.parse(timeStr.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Override
    public void startStreamRecording(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }

        if (Boolean.TRUE.equals(session.getIsRecording())) {
            log.info("Recording already in progress for camera {}", cameraId);
            return;
        }

        // 获取摄像头信息
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ServiceException("Camera not found: " + cameraId));

        // 通过 Kafka 通知录像服务开始录像
        recordingNotificationService.sendStartRecordingCommand(
                cameraId,
                camera.getName(),
                session.getSessionId(),
                LocalDateTime.now(),
                buildRecordingConfig(camera)
        );

        session.setIsRecording(true);
        streamSessionRepository.save(session);

        log.info("Started recording for camera {}", cameraId);
    }

    /**
     * 构建录像配置
     */
    private Map<String, Object> buildRecordingConfig(Camera camera) {
        Map<String, Object> config = new HashMap<>();
        config.put("format", "mp4");
        config.put("videoCodec", "h264");
        config.put("audioCodec", "aac");

        // 根据摄像头配置设置录像质量
        if (camera.getResolution() != null) {
            config.put("resolution", camera.getResolution());
        }
        if (camera.getBitrate() != null) {
            config.put("bitrate", camera.getBitrate());
        }
        if (camera.getFrameRate() != null) {
            config.put("frameRate", camera.getFrameRate());
        }

        // 默认配置
        config.put("maxDuration", 3600); // 1小时
        config.put("maxFileSize", 512); // 512MB

        return config;
    }

    @Override
    public void stopStreamRecording(Long cameraId) {
        StreamSession session = activeStreams.get(cameraId);
        if (session == null) {
            throw new ServiceException("No active stream for camera: " + cameraId);
        }

        if (!Boolean.TRUE.equals(session.getIsRecording())) {
            log.info("No recording in progress for camera {}", cameraId);
            return;
        }

        // 获取摄像头信息
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ServiceException("Camera not found: " + cameraId));

        // 通过 Kafka 通知录像服务停止录像
        recordingNotificationService.sendStopRecordingCommand(
                cameraId,
                camera.getName(),
                session.getSessionId(),
                LocalDateTime.now()
        );

        session.setIsRecording(false);
        streamSessionRepository.save(session);

        log.info("Stopped recording for camera {}", cameraId);
    }

    // ==================== 辅助方法 ====================

    private String generateSessionId() {
        return "stream-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private int calculateQualityLevel(String resolution, int bitrate, int frameRate) {
        // 根据分辨率、码率和帧率计算质量级别
        if (resolution != null) {
            if (resolution.contains("1920") || resolution.contains("4K")) {
                return bitrate > 4000 ? 5 : 4;
            } else if (resolution.contains("1280") || resolution.contains("720")) {
                return bitrate > 2000 ? 3 : 2;
            } else {
                return 1;
            }
        }
        return bitrate > 4000 ? 4 : (bitrate > 2000 ? 3 : (bitrate > 1000 ? 2 : 1));
    }

    /**
     * WebRTC会话信息
     */
    private static class WebRTCSession {
        private String sessionId;
        private String janusSessionId;
        private String handleId;
        private long createdAt;
        private boolean answerReceived;
        private long answerReceivedAt;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getJanusSessionId() { return janusSessionId; }
        public void setJanusSessionId(String janusSessionId) { this.janusSessionId = janusSessionId; }
        public String getHandleId() { return handleId; }
        public void setHandleId(String handleId) { this.handleId = handleId; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public boolean isAnswerReceived() { return answerReceived; }
        public void setAnswerReceived(boolean answerReceived) { this.answerReceived = answerReceived; }
        public long getAnswerReceivedAt() { return answerReceivedAt; }
        public void setAnswerReceivedAt(long answerReceivedAt) { this.answerReceivedAt = answerReceivedAt; }
    }

    // Janus 会话缓存
    private final Map<Long, String> cameraJanusSessions = new ConcurrentHashMap<>();
    private final Map<String, String> cameraHandles = new ConcurrentHashMap<>();

    /**
     * 获取或创建 Janus 会话
     */
    private String getOrCreateJanusSession(Long cameraId) {
        return cameraJanusSessions.computeIfAbsent(cameraId, id -> {
            log.info("Creating new Janus session for camera {}", cameraId);
            return janusClient.createSession();
        });
    }

    /**
     * 获取或创建 RTSP 插件句柄
     */
    private String getOrCreateRtspHandle(String janusSessionId, Camera camera) {
        return cameraHandles.computeIfAbsent(camera.getId().toString(), id -> {
            log.info("Creating RTSP plugin handle for camera {}", camera.getId());
            return janusClient.createRtspPlugin(janusSessionId, camera);
        });
    }

    /**
     * 获取摄像头的 RTSP 流地址
     */
    private String getRtspUrl(Camera camera) {
        // 直接使用 connectionUrl
        if (camera.getConnectionUrl() != null && !camera.getConnectionUrl().isEmpty()) {
            return camera.getConnectionUrl();
        }
        
        // 如果没有 connectionUrl，尝试从其他属性构建
        String connectionUrl = camera.getConnectionUrl();
        if (connectionUrl == null || connectionUrl.isEmpty()) {
            throw new ServiceException("Camera connection URL is not configured: " + camera.getId());
        }
        
        return connectionUrl;
    }
}
