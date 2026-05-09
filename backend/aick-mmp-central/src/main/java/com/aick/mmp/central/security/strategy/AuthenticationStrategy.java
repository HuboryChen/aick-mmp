package com.aick.mmp.central.security.strategy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * 认证策略接口
 * 定义各种认证方式的统一接口
 */
public interface AuthenticationStrategy {

    /**
     * 获取策略名称
     */
    String getName();

    /**
     * 获取策略优先级 (数值越小优先级越高)
     */
    int getPriority();

    /**
     * 检查是否支持当前请求
     * @param request HTTP 请求
     * @return true 如果此策略适用于当前请求
     */
    boolean supports(HttpServletRequest request);

    /**
     * 执行认证
     * @param request HTTP 请求
     * @return 认证结果，如果认证失败返回 null
     */
    Authentication authenticate(HttpServletRequest request);

    /**
     * 获取认证失败时的错误信息
     */
    default String getErrorMessage() {
        return "Authentication failed";
    }
}
