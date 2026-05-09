package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.SystemAppCredentialsResponseDTO;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.SystemAppRepository;
import com.aick.mmp.central.service.SystemAppService;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.OwnerType;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import com.aick.mmp.shared.util.AESEncryptionUtil;
import com.aick.mmp.shared.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of SystemAppService for managing system applications.
 * Credentials are now managed directly within the SystemApp entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAppServiceImpl implements SystemAppService {
    
    private static final String APP_KEY_PREFIX = "ak_";
    private static final String SECRET_KEY_PREFIX = "sk_";
    private static final int APP_KEY_LENGTH = 32;
    private static final int SECRET_KEY_LENGTH = 32;
    private static final String CACHE_KEY_PREFIX = "aick:systemapp:sk:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    
    private final SystemAppRepository systemAppRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final AESEncryptionUtil encryptionUtil;
    private final SignatureUtil signatureUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${security.signature.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;
    
    @Override
    @Transactional
    public SystemAppCredentialsResponseDTO createSystemAppWithCredentials(String name, String description,
                                                                          Set<SystemAppPermission> permissions,
                                                                          String ownerType, Long ownerId, Long createdBy) {
        log.info("Creating system app with credentials: {}", name);
        
        // Validate permissions
        validatePermissions(permissions);
        
        // Generate app key and secret
        String appKey = generateAppKey();
        String appSecret = generateSecretKey();
        String encryptedSecret = encryptionUtil.encrypt(appSecret);
        
        SystemApp app = SystemApp.builder()
                .appKey(appKey)
                .name(name)
                .description(description)
                .ownerType(OwnerType.valueOf(ownerType.toUpperCase()))
                .ownerId(ownerId)
                .permissions(permissions)
                .status(SystemApp.AppStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
        
        // Note: encryptedSecret is stored separately for security
        // In a real implementation, you might want to use @PrePersist
        // For now, we'll set it after save using direct update
        
        app = systemAppRepository.save(app);
        
        // Update with encrypted secret
        app.setEncryptedSecret(encryptedSecret);
        systemAppRepository.save(app);
        
        log.info("Created system app with credentials - ID: {}, appKey: {}", app.getId(), app.getAppKey());
        
        return SystemAppCredentialsResponseDTO.builder()
                .id(app.getId())
                .name(app.getName())
                .appKey(appKey)
                .appSecret(appSecret) // Only shown once
                .createdAt(app.getCreatedAt())
                .warning("请妥善保管密钥，Secret Key 仅显示一次")
                .build();
    }
    
    @Override
    @Transactional
    public SystemApp createSystemApp(String name, String description, Set<SystemAppPermission> permissions,
                                     String ownerType, Long ownerId, Long createdBy) {
        log.info("Creating system app: {}", name);
        
        // Validate permissions
        validatePermissions(permissions);
        
        SystemApp app = SystemApp.builder()
                .name(name)
                .description(description)
                .ownerType(OwnerType.valueOf(ownerType.toUpperCase()))
                .ownerId(ownerId)
                .permissions(permissions)
                .status(SystemApp.AppStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
        
        app = systemAppRepository.save(app);
        
        log.info("Created system app with ID: {}", app.getId());
        
        return app;
    }
    
    @Override
    public Optional<SystemApp> getSystemApp(Long id) {
        return systemAppRepository.findById(id);
    }
    
    @Override
    public Optional<SystemApp> getSystemAppByKey(String appKey) {
        return systemAppRepository.findByAppKey(appKey);
    }
    
    @Override
    public Page<SystemApp> listSystemApps(int page, int size, String ownerType, String status) {
        PageRequest pageRequest = PageRequest.of(page, size);
        
        if (ownerType != null && status != null) {
            OwnerType ot = OwnerType.valueOf(ownerType.toUpperCase());
            SystemApp.AppStatus as = SystemApp.AppStatus.valueOf(status.toUpperCase());
            return systemAppRepository.findByOwnerTypeAndStatus(ot, as, pageRequest);
        } else if (ownerType != null) {
            OwnerType ot = OwnerType.valueOf(ownerType.toUpperCase());
            return systemAppRepository.findByOwnerType(ot, pageRequest);
        } else if (status != null) {
            SystemApp.AppStatus as = SystemApp.AppStatus.valueOf(status.toUpperCase());
            return systemAppRepository.findByStatus(as, pageRequest);
        } else {
            return systemAppRepository.findAll(pageRequest);
        }
    }
    
    @Override
    @Transactional
    public SystemApp updateSystemApp(Long id, String name, String description,
                                     Set<SystemAppPermission> permissions, String status) {
        log.info("Updating system app: {}", id);
        
        SystemApp app = systemAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("System app not found: " + id));
        
        if (name != null) {
            app.setName(name);
        }
        if (description != null) {
            app.setDescription(description);
        }
        if (permissions != null) {
            validatePermissions(permissions);
            app.setPermissions(permissions);
        }
        if (status != null) {
            app.setStatus(SystemApp.AppStatus.valueOf(status.toUpperCase()));
        }
        
        app = systemAppRepository.save(app);
        
        log.info("Updated system app: {}", id);
        
        return app;
    }
    
    @Override
    @Transactional
    public void deleteSystemApp(Long id) {
        log.info("Deleting system app: {}", id);
        
        SystemApp app = systemAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("System app not found: " + id));
        
        // Check for associated Edge nodes
        List<EdgeNode> associatedNodes = edgeNodeRepository.findAll().stream()
                .filter(node -> node.getSystemApp() != null && node.getSystemApp().getId().equals(id))
                .toList();
        
        if (!associatedNodes.isEmpty()) {
            throw new RuntimeException("Cannot delete system app with associated Edge nodes. Disassociate Edge nodes first.");
        }
        
        // Invalidate cache
        if (app.getAppKey() != null) {
            invalidateCache(app.getAppKey());
        }
        
        systemAppRepository.delete(app);
        
        log.info("Deleted system app: {}", id);
    }
    
    @Override
    public void validatePermissions(Set<SystemAppPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        
        Set<SystemAppPermission> validPermissions = new HashSet<>(Arrays.asList(SystemAppPermission.values()));
        
        for (SystemAppPermission permission : permissions) {
            if (!validPermissions.contains(permission)) {
                throw new IllegalArgumentException("Invalid permission: " + permission + 
                        ". Valid permissions are: " + validPermissions);
            }
        }
    }
    
    @Override
    public Optional<String> getDecryptedAppSecret(Long appId) {
        SystemApp app = systemAppRepository.findById(appId)
                .orElse(null);
        
        if (app == null || app.getAppKey() == null) {
            return Optional.empty();
        }
        
        return getDecryptedAppSecretByKey(app.getAppKey());
    }
    
    @Override
    public Optional<String> getDecryptedAppSecretByKey(String appKey) {
        // Check cache first
        String cacheKey = CACHE_KEY_PREFIX + appKey;
        String cachedSecret = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSecret != null) {
            log.debug("Cache hit for app key: {}", appKey);
            return Optional.of(cachedSecret);
        }
        
        // Cache miss - decrypt from database
        SystemApp app = systemAppRepository.findByAppKey(appKey).orElse(null);
        if (app == null || app.getEncryptedSecret() == null) {
            return Optional.empty();
        }
        
        if (!app.isActive()) {
            log.warn("System app is not active: {}", appKey);
            return Optional.empty();
        }
        
        try {
            String decryptedSecret = encryptionUtil.decrypt(app.getEncryptedSecret());
            
            // Cache the decrypted secret
            redisTemplate.opsForValue().set(cacheKey, decryptedSecret, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            
            return Optional.of(decryptedSecret);
        } catch (Exception e) {
            log.error("Failed to decrypt secret key for app key: {}", appKey, e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional
    public SystemAppCredentialsResponseDTO regenerateCredentials(Long appId) {
        log.info("Regenerating credentials for system app: {}", appId);
        
        SystemApp app = systemAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("System app not found: " + appId));
        
        if (app.getAppKey() == null) {
            throw new RuntimeException("App key not found. Cannot regenerate credentials.");
        }
        
        // Generate new secret
        String appSecret = generateSecretKey();
        String encryptedSecret = encryptionUtil.encrypt(appSecret);
        
        // Invalidate old cache
        invalidateCache(app.getAppKey());
        
        // Update with new encrypted secret
        app.setEncryptedSecret(encryptedSecret);
        systemAppRepository.save(app);
        
        log.info("Regenerated credentials for system app: {}", appId);
        
        return SystemAppCredentialsResponseDTO.builder()
                .id(app.getId())
                .name(app.getName())
                .appKey(app.getAppKey())
                .appSecret(appSecret) // Only shown once
                .createdAt(app.getCreatedAt())
                .warning("请妥善保管密钥，Secret Key 仅显示一次。旧密钥已失效。")
                .build();
    }
    
    @Override
    public boolean validateAppCredentials(String appKey, String signature, String timestamp,
                                          String method, String path) {
        // Validate timestamp first
        if (!signatureUtil.isTimestampValid(timestamp)) {
            log.warn("Invalid timestamp for app key: {}", appKey);
            return false;
        }
        
        // Get decrypted secret key
        Optional<String> secretKeyOpt = getDecryptedAppSecretByKey(appKey);
        if (secretKeyOpt.isEmpty()) {
            log.warn("Could not get secret key for app key: {}", appKey);
            return false;
        }
        
        String secretKey = secretKeyOpt.get();
        
        // Build string to sign and verify
        String stringToSign = signatureUtil.buildStringToSign(method, path, timestamp);
        boolean valid = signatureUtil.verifySignature(stringToSign, signature, secretKey);
        
        if (valid) {
            // Update last used timestamp
            systemAppRepository.findByAppKey(appKey).ifPresent(app -> {
                app.markUsed();
                systemAppRepository.save(app);
            });
        }
        
        return valid;
    }
    
    private String generateAppKey() {
        byte[] bytes = new byte[APP_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return APP_KEY_PREFIX + encoded;
    }
    
    private String generateSecretKey() {
        byte[] bytes = new byte[SECRET_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return SECRET_KEY_PREFIX + encoded;
    }
    
    private void invalidateCache(String appKey) {
        String cacheKey = CACHE_KEY_PREFIX + appKey;
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated cache for app key: {}", appKey);
    }
}
