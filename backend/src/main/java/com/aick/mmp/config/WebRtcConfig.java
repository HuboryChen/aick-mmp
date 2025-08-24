package com.aick.mmp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * WebRTC配置类，配置WebRTC相关参数
 */
@Configuration
public class WebRtcConfig {

    @Value("${webrtc.config.janus.server-url:http://localhost:8088}")
    private String janusServerUrl;

    @Value("${webrtc.config.janus.timeout:5000}")
    private int janusTimeout;

    @Bean
    public Map<String, Object> webRtcConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // ICE服务器配置
        List<Map<String, String>> iceServers = Arrays.asList(
            createIceServer("stun:stun.l.google.com:19302"),
            createIceServer("stun:stun1.l.google.com:19302")
        );
        config.put("iceServers", iceServers);
        
        // Janus Gateway配置
        Map<String, Object> janusConfig = new HashMap<>();
        janusConfig.put("serverUrl", janusServerUrl);
        janusConfig.put("timeout", janusTimeout);
        config.put("janus", janusConfig);
        
        return config;
    }

    private Map<String, String> createIceServer(String url) {
        Map<String, String> iceServer = new HashMap<>();
        iceServer.put("urls", url);
        return iceServer;
    }
}