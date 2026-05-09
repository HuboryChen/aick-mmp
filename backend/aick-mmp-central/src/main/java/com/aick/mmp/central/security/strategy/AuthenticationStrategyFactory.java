package com.aick.mmp.central.security.strategy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证策略工厂
 * 负责管理和调度各种认证策略
 */
@Component
@Slf4j
public class AuthenticationStrategyFactory {

    private final List<AuthenticationStrategy> strategies;
    private final Map<String, AuthenticationStrategy> strategyMap;

    public AuthenticationStrategyFactory(List<AuthenticationStrategy> strategyList) {
        // 按优先级排序 (priority 小的先尝试)
        this.strategies = strategyList.stream()
                .sorted(Comparator.comparingInt(AuthenticationStrategy::getPriority))
                .collect(Collectors.toList());

        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        AuthenticationStrategy::getName,
                        s -> s,
                        (a, b) -> a
                ));

        log.info("Authentication strategies initialized (in priority order): {}", 
                strategies.stream()
                        .map(AuthenticationStrategy::getName)
                        .collect(Collectors.joining(" -> ")));
    }

    /**
     * 根据请求自动选择并执行认证策略
     * @param request HTTP 请求
     * @return 认证结果 (不为 null)
     */
    public Authentication authenticate(HttpServletRequest request) {
        for (AuthenticationStrategy strategy : strategies) {
            if (strategy.supports(request)) {
                log.debug("Attempting authentication with strategy: {}", strategy.getName());
                Authentication result = strategy.authenticate(request);
                if (result != null) {
                    log.debug("Authentication successful with strategy: {}", strategy.getName());
                    return result;
                }
                // 当前策略不支持或失败，继续尝试下一个
                log.debug("Strategy {} failed or not applicable, trying next...", strategy.getName());
            }
        }

        // 所有策略都失败，使用匿名认证
        log.debug("All strategies failed, using anonymous authentication");
        AuthenticationStrategy anonymousStrategy = strategyMap.get("ANONYMOUS");
        return anonymousStrategy != null ? anonymousStrategy.authenticate(request) : null;
    }

    /**
     * 获取所有已注册策略的列表
     */
    public List<AuthenticationStrategy> getStrategies() {
        return strategies;
    }

    /**
     * 根据名称获取特定策略
     */
    public AuthenticationStrategy getStrategy(String name) {
        return strategyMap.get(name);
    }
}
