package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.Camera;
import java.util.Map;

/**
 * Janus Gateway 客户端接口
 * 封装与Janus WebRTC Gateway的交互
 */
public interface JanusClient {

    /**
     * 创建Janus会话
     * @return 会话ID
     */
    String createSession();

    /**
     * 销毁Janus会话
     * @param sessionId 会话ID
     */
    void destroySession(String sessionId);

    /**
     * 创建RTSP转WebRTC插件句柄
     * @param sessionId Janus会话ID
     * @param camera 摄像头信息
     * @return 插件句柄ID
     */
    String createRtspPlugin(String sessionId, Camera camera);

    /**
     * 启动RTSP流转WebRTC
     * @param sessionId Janus会话ID
     * @param handleId 插件句柄ID
     * @param rtspUrl RTSP流地址
     * @return 包含SDP Offer的响应
     */
    Map<String, Object> startRtspStream(String sessionId, String handleId, String rtspUrl);

    /**
     * 处理WebRTC Answer
     * @param sessionId Janus会话ID
     * @param handleId 插件句柄ID
     * @param answer SDP Answer
     */
    void processAnswer(String sessionId, String handleId, String answer);

    /**
     * 添加ICE候选
     * @param sessionId Janus会话ID
     * @param handleId 插件句柄ID
     * @param candidate ICE候选信息
     */
    void addIceCandidate(String sessionId, String handleId, Map<String, Object> candidate);

    /**
     * 停止RTSP流转WebRTC
     * @param sessionId Janus会话ID
     * @param handleId 插件句柄ID
     */
    void stopRtspStream(String sessionId, String handleId);

    /**
     * 销毁插件句柄
     * @param sessionId Janus会话ID
     * @param handleId 插件句柄ID
     */
    void destroyHandle(String sessionId, String handleId);

    /**
     * 获取Janus Gateway健康状态
     * @return 是否健康
     */
    boolean isHealthy();

    /**
     * 获取Janus Gateway信息
     * @return Janus信息
     */
    Map<String, Object> getInfo();
}
