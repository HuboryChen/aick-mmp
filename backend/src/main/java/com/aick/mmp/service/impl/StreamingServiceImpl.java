package com.aick.mmp.service.impl;

import com.aick.mmp.adapter.protocol.ProtocolAdapter;
import com.aick.mmp.model.Camera;
import com.aick.mmp.model.StreamSession;
import com.aick.mmp.repository.StreamSessionRepository;
import com.aick.mmp.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingServiceImpl implements StreamingService {

    private final StreamSessionRepository streamSessionRepository;
    private final List<ProtocolAdapter> protocolAdapters;
    
    // 存储活动的流会话
    private final Map<String, StreamSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public String startStream(Camera camera) {
        try {
            // 查找合适的协议适配器
            ProtocolAdapter adapter = findProtocolAdapter(camera.getProtocol().name());
            if (adapter == null) {
                throw new RuntimeException("No protocol adapter found for: " + camera.getProtocol());
            }

            // 创建流会话
            StreamSession session = StreamSession.builder()
                    .sessionId(generateSessionId())
                    .cameraId(camera.getId())
                    .protocol(camera.getProtocol().name())
                    .status(StreamSession.StreamStatus.INITIATED)
                    .startTime(LocalDateTime.now())
                    .build();

            // 启动流
            String sessionId = adapter.startStreamSession(camera);
            session.setSessionId(sessionId);
            session.setStatus(StreamSession.StreamStatus.STREAMING);

            // 保存会话
            streamSessionRepository.save(session);
            activeSessions.put(sessionId, session);

            log.info("Started stream for camera {} with session {}", camera.getId(), sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("Failed to start stream for camera {}: {}", camera.getId(), e.getMessage());
            throw new RuntimeException("Stream start failed: " + e.getMessage());
        }
    }

    @Override
    public void stopStream(String sessionId) {
        try {
            StreamSession session = activeSessions.get(sessionId);
            if (session == null) {
                log.warn("Stream session not found: {}", sessionId);
                return;
            }

            // 查找协议适配器并停止流
            ProtocolAdapter adapter = findProtocolAdapter(session.getProtocol());
            if (adapter != null) {
                adapter.stopStreamSession(sessionId);
            }

            // 更新会话状态
            session.setStatus(StreamSession.StreamStatus.DISCONNECTED);
            session.setEndTime(LocalDateTime.now());
            streamSessionRepository.save(session);
            activeSessions.remove(sessionId);

            log.info("Stopped stream session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to stop stream {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStreamMetrics(String sessionId) {
        try {
            StreamSession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new RuntimeException("Stream session not found: " + sessionId);
            }

            ProtocolAdapter adapter = findProtocolAdapter(session.getProtocol());
            if (adapter == null) {
                throw new RuntimeException("Protocol adapter not found for: " + session.getProtocol());
            }

            return adapter.getStreamMetrics(sessionId);

        } catch (Exception e) {
            log.error("Failed to get stream metrics for {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to get stream metrics: " + e.getMessage());
        }
    }

    @Override
    public void adjustStreamQuality(String sessionId, int qualityLevel) {
        try {
            StreamSession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new RuntimeException("Stream session not found: " + sessionId);
            }

            ProtocolAdapter adapter = findProtocolAdapter(session.getProtocol());
            if (adapter == null) {
                throw new RuntimeException("Protocol adapter not found for: " + session.getProtocol());
            }

            adapter.adjustStreamQuality(sessionId, qualityLevel);
            log.info("Adjusted stream quality for session {} to level {}", sessionId, qualityLevel);

        } catch (Exception e) {
            log.error("Failed to adjust stream quality for {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Stream quality adjustment failed: " + e.getMessage());
        }
    }

    @Override
    public List<StreamSession> getActiveSessions() {
        return streamSessionRepository.findByStatus(StreamSession.StreamStatus.STREAMING);
    }

    private ProtocolAdapter findProtocolAdapter(String protocolName) {
        // 简单的协议匹配，实际应该更智能
        for (ProtocolAdapter adapter : protocolAdapters) {
            Class<?> adapterClass = adapter.getClass();
            String className = adapterClass.getSimpleName().toLowerCase();
            if (className.contains(protocolName.toLowerCase())) {
                return adapter;
            }
        }
        return null;
    }

    private String generateSessionId() {
        return "stream-" + UUID.randomUUID().toString().substring(0, 8);
    }
}