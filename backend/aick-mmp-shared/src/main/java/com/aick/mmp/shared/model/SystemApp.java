package com.aick.mmp.shared.model;

import com.aick.mmp.shared.model.enums.OwnerType;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * System application entity for managing system-level credentials and permissions.
 * Used for Edge nodes and other system-to-system authentication.
 */
@Entity
@Table(name = "system_apps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique application key (UUID format) for referencing
     */
    @Column(name = "app_key", unique = true, length = 64)
    private String appKey;
    
    /**
     * Encrypted Secret Key (SK) - encrypted with AES-256-GCM
     * Only used for system-level authentication
     */
    @Column(name = "encrypted_secret", length = 256)
    private String encryptedSecret;
    
    /**
     * Application name
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Application description
     */
    @Column(length = 500)
    private String description;
    
    /**
     * Owner type - SYSTEM or USER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType;
    
    /**
     * Owner ID - user ID if ownerType is USER, null if SYSTEM
     */
    @Column(name = "owner_id")
    private Long ownerId;
    
    /**
     * Application status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppStatus status = AppStatus.ACTIVE;
    
    /**
     * Permissions granted to this application
     */
    @ElementCollection(targetClass = SystemAppPermission.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "system_app_permissions", joinColumns = @JoinColumn(name = "app_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @Builder.Default
    private Set<SystemAppPermission> permissions = new HashSet<>();
    
    /**
     * User who created this application
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Last time this app was used for authentication
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    public enum AppStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
    
    /**
     * Check if this app has a specific permission
     */
    public boolean hasPermission(SystemAppPermission permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if this app has credentials (for system-level authentication)
     */
    public boolean hasCredentials() {
        return appKey != null && encryptedSecret != null;
    }
    
    /**
     * Mark this app as used for authentication
     */
    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * Generate a new app key for this app
     */
    public void generateAppKey() {
        this.appKey = "ak_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Check if this is a system-level app
     */
    public boolean isSystemLevel() {
        return ownerType == OwnerType.SYSTEM && ownerId == null;
    }
    
    /**
     * Check if this app is active
     */
    public boolean isActive() {
        return status == AppStatus.ACTIVE;
    }
}
