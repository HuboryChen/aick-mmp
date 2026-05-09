package com.aick.mmp.central.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Janus WebRTC Gateway 配置类
 */
@Configuration
@ConfigurationProperties(prefix = "janus")
@Data
public class JanusConfig {

    /**
     * Janus Gateway 服务器地址
     */
    private String serverUrl = "http://localhost:8088/janus";

    /**
     * API 密钥（如果Janus配置了API密钥）
     */
    private String apiKey;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * WebRTC ICE服务器配置
     */
    private IceServerConfig iceServers = new IceServerConfig();

    /**
     * RTSP转WebRTC插件配置
     */
    private RtspPluginConfig rtspPlugin = new RtspPluginConfig();

    @Data
    public static class IceServerConfig {
        /**
         * 是否使用STUN服务器
         */
        private boolean useStun = true;
        
        /**
         * STUN服务器地址
         */
        private String stunServer = "stun:stun.l.google.com:19302";
        
        /**
         * 是否使用TURN服务器
         */
        private boolean useTurn = false;
        
        /**
         * TURN服务器地址
         */
        private String turnServer;
        
        /**
         * TURN用户名
         */
        private String turnUsername;
        
        /**
         * TURN密码
         */
        private String turnPassword;
    }

    @Data
    public static class RtspPluginConfig {
        /**
         * 是否启用RTSP转WebRTC
         */
        private boolean enabled = true;
        
        /**
         * 默认视频编解码器
         */
        private String videoCodec = "h264";
        
        /**
         * 默认音频编解码器
         */
        private String audioCodec = "opus";
        
        /**
         * 视频端口范围
         */
        private String videoPortRange = "20000-40000";
        
        /**
         * 音频端口范围
         */
        private String audioPortRange = "20000-40000";
    }
}
