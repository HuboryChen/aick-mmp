package com.aick.mmp.central.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知发送结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 响应消息（如API返回的消息）
     */
    private String responseMessage;

    /**
     * 发送耗时（毫秒）
     */
    private Long costTime;

    /**
     * 是否需要重试
     */
    private boolean retryable;

    /**
     * 创建成功结果
     */
    public static NotificationResult success() {
        return NotificationResult.builder()
                .success(true)
                .retryable(false)
                .build();
    }

    /**
     * 创建成功结果（带响应消息）
     */
    public static NotificationResult success(String responseMessage, long costTime) {
        return NotificationResult.builder()
                .success(true)
                .responseMessage(responseMessage)
                .costTime(costTime)
                .retryable(false)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NotificationResult failure(String errorCode, String errorMessage) {
        return NotificationResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(true)
                .build();
    }

    /**
     * 创建失败结果（不可重试）
     */
    public static NotificationResult failureNonRetryable(String errorCode, String errorMessage) {
        return NotificationResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(false)
                .build();
    }
}
