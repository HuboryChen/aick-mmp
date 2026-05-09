package com.aick.mmp.central.channel;

import com.aick.mmp.shared.model.AlertRecord;

/**
 * 通知渠道接口
 * 定义通知发送的统一接口，所有通知渠道实现类都需要实现此接口
 */
public interface NotificationChannel {

    /**
     * 获取渠道类型标识
     */
    String getChannelType();

    /**
     * 检查渠道是否可用（已配置且启用）
     */
    boolean isAvailable();

    /**
     * 发送通知
     *
     * @param alert 告警记录
     * @param target 通知目标（如邮箱地址、手机号、webhook URL等）
     * @return 发送结果
     */
    NotificationResult send(AlertRecord alert, String target);

    /**
     * 发送通知（带额外配置）
     *
     * @param alert 告警记录
     * @param target 通知目标
     * @param extraConfig 额外配置（JSON格式）
     * @return 发送结果
     */
    NotificationResult send(AlertRecord alert, String target, String extraConfig);

    /**
     * 测试渠道连接/配置
     *
     * @param target 测试目标
     * @return 测试结果
     */
    NotificationResult testConnection(String target);
}
