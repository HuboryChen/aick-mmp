package com.aick.mmp.service;

import com.aick.mmp.model.Camera;
import com.aick.mmp.model.StreamSession;

import java.util.List;
import java.util.Map;

public interface StreamingService {
    // 基础流管理
    String startStream(Camera camera);
    void stopStream(String sessionId);
    Map<String, Object> getStreamMetrics(String sessionId);
    void adjustStreamQuality(String sessionId, int qualityLevel);
    List<StreamSession> getActiveSessions();
    
    // 兼容原有接口
    default String getStreamUrl(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default boolean testCameraConnection(Camera camera) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default StreamSession startStream(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void stopStream(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void pauseStream(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void resumeStream(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default Map<String, Object> getStreamStatus(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default String generateWebRtcOffer(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default String processWebRtcAnswer(Long cameraId, String answer) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void updateStreamQuality(Long cameraId, String resolution, int bitrate, int frameRate) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default String getStreamRecordingUrl(Long cameraId, String startTime, String endTime) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void startStreamRecording(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    default void stopStreamRecording(Long cameraId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}