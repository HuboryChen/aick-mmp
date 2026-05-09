package com.aick.mmp.central.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for API key authentication.
 */
public class UnifiedAuthenticationToken extends AbstractAuthenticationToken {
    
    private final UnifiedPrincipal principal;
    private final Object credentials;
    
    public UnifiedAuthenticationToken(UnifiedPrincipal principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }
    
    public UnifiedAuthenticationToken(UnifiedPrincipal principal, 
                                        Object credentials,
                                        Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return credentials;
    }
    
    @Override
    public UnifiedPrincipal getPrincipal() {
        return principal;
    }
}
