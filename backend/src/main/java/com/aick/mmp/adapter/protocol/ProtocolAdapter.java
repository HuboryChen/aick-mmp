package com.aick.mmp.adapter.protocol;

import com.aick.mmp.model.Camera;
import com.aick.mmp.model.StreamSession;
import java.util.Map;

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
     * 测试摄像头连接
     * @param camera 摄像头信息
     * @return 连接是否成功
     */
    boolean testConnection(Camera camera);

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
}