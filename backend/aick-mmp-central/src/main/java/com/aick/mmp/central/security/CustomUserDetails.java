package com.aick.mmp.central.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 自定义用户详情，包含用户ID
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    
    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public String toString() {
        return "CustomUserDetails{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}
