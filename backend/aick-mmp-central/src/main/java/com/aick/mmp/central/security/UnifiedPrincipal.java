package com.aick.mmp.central.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Principal;
import java.util.Set;

/**
 * Unified principal that can represent both user and system app identities.
 * Used to store authentication information in SecurityContext.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedPrincipal implements Principal {
    
    /**
     * Unique identifier - user ID or app ID
     */
    private String identityId;
    
    /**
     * Type of identity
     */
    private IdentityType identityType;
    
    /**
     * Username (for user identity)
     */
    private String username;
    
    /**
     * User role (for user identity)
     */
    private String role;
    
    /**
     * Associated system app ID (for system identity)
     */
    private Long systemAppId;
    
    /**
     * Permissions (for system identity)
     */
    private Set<String> permissions;
    
    /**
     * Authentication method used
     */
    private AuthMethod authMethod;
    
    public enum IdentityType {
        USER,
        SYSTEM_APP,
        ANONYMOUS
    }
    
    public enum AuthMethod {
        JWT,
        API_KEY,
        ANONYMOUS
    }
    
    @Override
    public String getName() {
        return identityId;
    }
    
    /**
     * Check if this principal has a specific permission (for system apps)
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if this is a system app identity
     */
    public boolean isSystemApp() {
        return identityType == IdentityType.SYSTEM_APP;
    }
    
    /**
     * Check if this is a user identity
     */
    public boolean isUser() {
        return identityType == IdentityType.USER;
    }
}
