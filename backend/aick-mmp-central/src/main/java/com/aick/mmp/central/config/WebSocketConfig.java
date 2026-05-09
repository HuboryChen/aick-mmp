package com.aick.mmp.central.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


/**
 * WebSocket 配置类
 * 用于配置告警通知等实时消息推送
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 配置消息代理前缀
        // /topic 用于广播消息（发布-订阅模式）
        // /queue 用于点对点消息
        config.enableSimpleBroker("/topic", "/queue");
        
        // 配置应用目的地前缀（客户端发送消息时使用）
        config.setApplicationDestinationPrefixes("/app");
        
        // 配置用户目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过该端点连接WebSocket
        registry.addEndpoint("/ws/alerts")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        registry.addEndpoint("/ws/stream")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        registry.addEndpoint("/ws/discovery")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws/import")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // 无SockJS的WebSocket端点
        registry.addEndpoint("/ws/alerts")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws/stream")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws/discovery")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws/import")
                .setAllowedOriginPatterns("*");
    }
}
