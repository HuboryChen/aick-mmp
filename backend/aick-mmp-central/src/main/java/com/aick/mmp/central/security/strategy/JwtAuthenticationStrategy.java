package com.aick.mmp.central.security.strategy;

import com.aick.mmp.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

/**
 * JWT 认证策略
 * 优先级: 1 (最高优先级)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationStrategy implements AuthenticationStrategy {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public String getName() {
        return "JWT";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    @Override
    public Authentication authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.debug("JWT token validation failed");
                return null;
            }

            String username = jwtUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            log.debug("JWT authentication successful for user: {}", username);
            return authentication;

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getErrorMessage() {
        return "Invalid or expired JWT token";
    }
}
