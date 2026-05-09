package com.aick.mmp.central.security;

import com.aick.mmp.central.security.strategy.AuthenticationStrategyFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 统一认证过滤器
 * 使用工厂+策略模式实现多种认证方式的自动切换
 * 认证优先级: JWT > AK/SK > Anonymous
 * 注意：不使用 @Component 注解，避免被自动注册为 Servlet Filter
 */
@Slf4j
public class UnifiedAuthFilter extends OncePerRequestFilter {

    private final AuthenticationStrategyFactory strategyFactory;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public UnifiedAuthFilter(AuthenticationStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    // 无需认证的公开端点 - 对于这些端点，我们创建一个特殊的认证对象
    // 使其通过Spring Security的permitAll检查，但仍允许Controller处理业务逻辑
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/validate",
        "/api/edge/register",
        "/actuator/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // 跳过 OPTIONS 请求 (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 对于公开端点，设置一个特殊的认证对象，使其可以通过permitAll检查
        if (isPublicEndpoint(requestUri)) {
            log.debug("Public endpoint accessed: {} {}", method, requestUri);
            // 创建一个特殊的认证对象，用于公开端点
            // 这个认证对象具有 PUBLIC_ACCESS 权限，可以通过 permitAll 检查
            Authentication publicAuth = new UsernamePasswordAuthenticationToken(
                "PUBLIC_USER",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PUBLIC"))
            );
            SecurityContextHolder.getContext().setAuthentication(publicAuth);
            filterChain.doFilter(request, response);
            return;
        }

        // 如果已有认证信息且有效，跳过认证
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
            !"anonymous".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            log.debug("Request already authenticated: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 使用工厂自动选择认证策略
        Authentication authentication = strategyFactory.authenticate(request);
        
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication set for request {} {}: {}", method, requestUri, authentication.getName());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestUri) {
        for (String pattern : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }
}
