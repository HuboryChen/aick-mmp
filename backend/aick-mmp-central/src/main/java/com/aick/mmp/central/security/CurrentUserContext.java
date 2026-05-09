package com.aick.mmp.central.security;

import com.aick.mmp.shared.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 当前用户上下文工具类
 * 用于获取当前登录用户的信息
 */
@Component
@Slf4j
public class CurrentUserContext {
    
    /**
     * 获取当前登录用户的用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }
    
    /**
     * 获取当前登录用户的ID
     * 通过从token中解析的用户ID获取
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            } else if (principal instanceof String) {
                try {
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    log.warn("Unable to parse user ID from principal: {}", principal);
                }
            } else if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
        }
        return null;
    }
    
    /**
     * 获取当前用户是否已认证
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * 获取当前用户的角色
     */
    public Optional<User.UserRole> getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .map(auth -> {
                        String role = auth.getAuthority();
                        if (role.startsWith("ROLE_")) {
                            role = role.substring(5);
                        }
                        try {
                            return User.UserRole.valueOf(role);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .findFirst();
        }
        return Optional.empty();
    }
}
