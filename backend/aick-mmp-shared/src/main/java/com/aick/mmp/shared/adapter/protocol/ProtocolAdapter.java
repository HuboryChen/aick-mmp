package com.aick.mmp.shared.adapter.protocol;

import com.aick.mmp.shared.model.Camera;
import java.util.Map;
import java.util.Optional;

/**
 * 协议适配器接口，定义不同摄像头协议适配器的统一方法签名
 */
public interface ProtocolAdapter {

    /**
     * 获取协议类型
     * @return 协议名称
     */
    String getProtocol();

    /**
     * 验证连接URL格式是否正确
     * @param url 连接URL
     * @return 是否有效
     */
    default boolean validateUrl(String url) {
        return url != null && !url.trim().isEmpty();
    }

    /**
     * 测试摄像头连接
     * @param camera 摄像头信息
     * @return 连接是否成功
     */
    boolean testConnection(Camera camera);

    /**
     * 获取摄像头能力信息
     * @param camera 摄像头信息
     * @return 能力信息Map
     */
    default Map<String, Object> getCapabilities(Camera camera) {
        return Map.of();
    }

    /**
     * 启动流会话
     * @param camera 摄像头信息
     * @return 会话ID
     */
    String startStreamSession(Camera camera);

    /**
     * 停止流会话
     * @param sessionId 会话ID
     */
    void stopStreamSession(String sessionId);

    /**
     * 暂停流会话
     * @param sessionId 会话ID
     */
    default void pauseStreamSession(String sessionId) {
        throw new UnsupportedOperationException("Pause not supported for this protocol");
    }

    /**
     * 恢复流会话
     * @param sessionId 会话ID
     */
    default void resumeStreamSession(String sessionId) {
        throw new UnsupportedOperationException("Resume not supported for this protocol");
    }

    /**
     * 调整流质量
     * @param sessionId 会话ID
     * @param qualityLevel 质量级别（1-5，从低到高）
     */
    void adjustStreamQuality(String sessionId, int qualityLevel);

    /**
     * 获取流指标
     * @param sessionId 会话ID
     * @return 包含比特率、帧率、丢包率等指标的Map
     */
    Map<String, Object> getStreamMetrics(String sessionId);

    /**
     * 获取会话关联的摄像头信息
     * @param sessionId 会话ID
     * @return 摄像头信息（如果存在）
     */
    default Optional<Camera> getCameraForSession(String sessionId) {
        return Optional.empty();
    }

    /**
     * 检查会话是否活跃
     * @param sessionId 会话ID
     * @return 是否活跃
     */
    default boolean isSessionActive(String sessionId) {
        return false;
    }

    /**
     * 获取连接池统计信息
     * @return 连接池统计Map
     */
    default Map<String, Object> getConnectionPoolStats() {
        return Map.of(
            "activeConnections", 0,
            "idleConnections", 0,
            "maxConnections", 0
        );
    }
}