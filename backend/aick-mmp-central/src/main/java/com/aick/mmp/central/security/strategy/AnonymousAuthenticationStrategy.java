package com.aick.mmp.central.security.strategy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 匿名认证策略
 * 优先级: 3 (最低优先级，作为后备)
 */
@Component
@Slf4j
public class AnonymousAuthenticationStrategy implements AuthenticationStrategy {

    private static final AnonymousAuthenticationToken ANONYMOUS_AUTH =
            new AnonymousAuthenticationToken(
                    "anonymous",
                    "anonymous",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );

    static {
        ANONYMOUS_AUTH.setAuthenticated(true);
    }

    @Override
    public String getName() {
        return "ANONYMOUS";
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        // 此策略始终支持，作为其他策略失败后的后备
        return true;
    }

    @Override
    public Authentication authenticate(HttpServletRequest request) {
        // 匿名认证始终成功，返回匿名令牌
        log.debug("Anonymous authentication applied");
        return ANONYMOUS_AUTH;
    }

    @Override
    public String getErrorMessage() {
        return "Anonymous access";
    }
}
