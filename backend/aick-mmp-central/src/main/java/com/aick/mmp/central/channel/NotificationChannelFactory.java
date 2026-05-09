package com.aick.mmp.central.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知渠道工厂
 * 用于管理所有通知渠道实例，提供统一的渠道获取接口
 */
@Component
@Slf4j
public class NotificationChannelFactory {

    private final Map<String, NotificationChannel> channels = new HashMap<>();

    public NotificationChannelFactory(List<NotificationChannel> channelList) {
        // 自动注册所有实现了 NotificationChannel 接口的 Bean
        for (NotificationChannel channel : channelList) {
            String channelType = channel.getChannelType();
            channels.put(channelType, channel);
            log.info("注册通知渠道: type={}, available={}", channelType, channel.isAvailable());
        }
        log.info("通知渠道工厂初始化完成, 共注册 {} 个渠道", channels.size());
    }

    /**
     * 根据渠道类型获取通知渠道
     *
     * @param channelType 渠道类型 (如 EMAIL, SMS, IN_APP, WEBHOOK)
     * @return 通知渠道实例
     */
    public Optional<NotificationChannel> getChannel(String channelType) {
        if (channelType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(channels.get(channelType.toUpperCase()));
    }

    /**
     * 根据渠道类型获取通知渠道，如果不存在则抛出异常
     *
     * @param channelType 渠道类型
     * @return 通知渠道实例
     * @throws IllegalArgumentException 如果渠道不存在
     */
    public NotificationChannel getChannelOrThrow(String channelType) {
        return getChannel(channelType)
                .orElseThrow(() -> new IllegalArgumentException("未知的通知渠道类型: " + channelType));
    }

    /**
     * 检查渠道是否可用
     *
     * @param channelType 渠道类型
     * @return 是否可用
     */
    public boolean isChannelAvailable(String channelType) {
        return getChannel(channelType)
                .map(NotificationChannel::isAvailable)
                .orElse(false);
    }

    /**
     * 获取所有已注册的渠道
     *
     * @return 渠道类型到实例的映射
     */
    public Map<String, NotificationChannel> getAllChannels() {
        return new HashMap<>(channels);
    }

    /**
     * 获取所有可用的渠道
     *
     * @return 可用的渠道类型列表
     */
    public Map<String, NotificationChannel> getAvailableChannels() {
        Map<String, NotificationChannel> available = new HashMap<>();
        for (Map.Entry<String, NotificationChannel> entry : channels.entrySet()) {
            if (entry.getValue().isAvailable()) {
                available.put(entry.getKey(), entry.getValue());
            }
        }
        return available;
    }

    /**
     * 获取所有已注册的渠道类型
     *
     * @return 渠道类型列表
     */
    public java.util.Set<String> getRegisteredChannelTypes() {
        return channels.keySet();
    }
}
