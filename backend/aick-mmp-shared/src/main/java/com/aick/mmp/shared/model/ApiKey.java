package com.aick.mmp.shared.model;

import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * API Key entity for AK/SK authentication (USER type only).
 * System-level credentials are now stored directly in SystemApp entity.
 * Access Key (AK) is stored as plain text, Secret Key (SK) is encrypted.
 */
@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_key_access_key", columnList = "access_key", unique = true),
    @Index(name = "idx_api_key_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Access Key (AK) - unique identifier, starts with "ak_"
     */
    @Column(name = "access_key", nullable = false, unique = true, length = 64)
    private String accessKey;
    
    /**
     * Encrypted Secret Key (SK) - encrypted with AES-256-GCM
     */
    @Column(name = "encrypted_secret", nullable = false, length = 256)
    private String encryptedSecret;
    
    /**
     * Associated user (for USER type keys only)
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * Type of API key - only USER type remains
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false)
    @Builder.Default
    private ApiKeyType type = ApiKeyType.USER;
    
    /**
     * Human-readable name for this key
     */
    @Column(length = 100)
    private String name;
    
    /**
     * Current status of this key
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApiKeyStatus status = ApiKeyStatus.ENABLED;
    
    /**
     * Last time this key was used for authentication
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    /**
     * When this key expires (null means never expires)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Check if this key is currently valid for use
     */
    public boolean isValid() {
        if (status != ApiKeyStatus.ENABLED) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
    
    /**
     * Update last used timestamp
     */
    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
