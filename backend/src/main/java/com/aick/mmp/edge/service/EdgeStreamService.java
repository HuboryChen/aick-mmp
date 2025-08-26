package com.aick.mmp.edge.service;

import com.aick.mmp.edge.dto.EdgeStreamDTO;
import com.aick.mmp.model.StreamSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Edge Stream Service - handles local stream processing and management on edge nodes
 */
public interface EdgeStreamService {
    
    /**
     * Start a stream for a camera
     */
    EdgeStreamDTO startStream(Long cameraId, Map<String, Object> parameters);
    
    /**
     * Stop a stream session
     */
    void stopStream(String sessionId);
    
    /**
     * Get stream by session ID
     */
    Optional<EdgeStreamDTO> getStream(String sessionId);
    
    /**
     * Get all active streams on this edge node
     */
    List<EdgeStreamDTO> getActiveStreams();
    
    /**
     * Get streams by camera ID
     */
    List<EdgeStreamDTO> getStreamsByCamera(Long cameraId);
    
    /**
     * Adjust stream quality
     */
    void adjustStreamQuality(String sessionId, int qualityLevel);
    
    /**
     * Get stream metrics
     */
    Map<String, Object> getStreamMetrics(String sessionId);
    
    /**
     * Get all stream metrics for this edge node
     */
    Map<String, Object> getAllStreamMetrics();
    
    /**
     * Update stream status
     */
    void updateStreamStatus(String sessionId, StreamSession.StreamStatus status);
    
    /**
     * Get stream count by status
     */
    long getStreamCountByStatus(StreamSession.StreamStatus status);
    
    /**
     * Get total active streams count
     */
    long getActiveStreamsCount();
    
    /**
     * Check if edge node can handle more streams
     */
    boolean canHandleMoreStreams();
    
    /**
     * Get current bandwidth usage
     */
    double getCurrentBandwidthUsage();
    
    /**
     * Restart failed streams
     */
    void restartFailedStreams();
    
    /**
     * Cleanup inactive streams
     */
    void cleanupInactiveStreams();
    
    /**
     * Get stream URL for local access
     */
    String getLocalStreamUrl(String sessionId);
    
    /**
     * Get stream URL for external access
     */
    String getExternalStreamUrl(String sessionId);
    
    /**
     * Initialize streaming service
     */
    void initializeStreaming();
    
    /**
     * Shutdown all streams
     */
    void shutdownAllStreams();
    
    /**
     * Process stream heartbeat
     */
    void processStreamHeartbeat(String sessionId);
    
    /**
     * Get stream health status
     */
    Map<String, Object> getStreamHealth(String sessionId);
}