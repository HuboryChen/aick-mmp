package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.ApiKeyCreatedResponseDTO;
import com.aick.mmp.central.dto.ApiKeyDTO;
import com.aick.mmp.central.dto.CreateApiKeyRequestDTO;
import com.aick.mmp.central.repository.ApiKeyRepository;
import com.aick.mmp.central.service.ApiKeyService;
import com.aick.mmp.shared.model.ApiKey;
import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import com.aick.mmp.shared.util.AESEncryptionUtil;
import com.aick.mmp.shared.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of ApiKeyService for managing API keys (USER type only).
 * System-level credentials are now managed in SystemAppService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {
    
    private static final String ACCESS_KEY_PREFIX = "ak_";
    private static final String SECRET_KEY_PREFIX = "sk_";
    private static final int ACCESS_KEY_LENGTH = 32;
    private static final int SECRET_KEY_LENGTH = 32;
    private static final String CACHE_KEY_PREFIX = "aick:apikey:sk:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    
    private final ApiKeyRepository apiKeyRepository;
    private final AESEncryptionUtil encryptionUtil;
    private final SignatureUtil signatureUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${security.signature.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;
    
    @Override
    @Transactional
    public ApiKeyCreatedResponseDTO createApiKeyForUser(Long userId, CreateApiKeyRequestDTO request) {
        log.info("Creating API key for user: {}", userId);
        
        String accessKey = generateAccessKey();
        String secretKey = generateSecretKey();
        String encryptedSecret = encryptionUtil.encrypt(secretKey);
        
        ApiKey apiKey = ApiKey.builder()
                .accessKey(accessKey)
                .encryptedSecret(encryptedSecret)
                .userId(userId)
                .type(ApiKeyType.USER)
                .name(request.getName())
                .status(ApiKeyStatus.ENABLED)
                .expiresAt(request.getExpiresAt())
                .build();
        
        apiKeyRepository.save(apiKey);
        
        log.info("Created API key {} for user {}", accessKey, userId);
        
        return ApiKeyCreatedResponseDTO.builder()
                .accessKey(accessKey)
                .secretKey(secretKey) // One-time display
                .name(request.getName())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
    
    @Override
    public Optional<String> getDecryptedSecretKey(String accessKey) {
        // Check cache first
        String cacheKey = CACHE_KEY_PREFIX + accessKey;
        String cachedSecret = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSecret != null) {
            log.debug("Cache hit for access key: {}", accessKey);
            return Optional.of(cachedSecret);
        }
        
        // Cache miss - decrypt from database
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByAccessKey(accessKey);
        if (apiKeyOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        if (!apiKey.isValid()) {
            log.warn("API key is not valid: {}", accessKey);
            return Optional.empty();
        }
        
        try {
            String decryptedSecret = encryptionUtil.decrypt(apiKey.getEncryptedSecret());
            
            // Cache the decrypted secret
            redisTemplate.opsForValue().set(cacheKey, decryptedSecret, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            
            return Optional.of(decryptedSecret);
        } catch (Exception e) {
            log.error("Failed to decrypt secret key for access key: {}", accessKey, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<ApiKeyDTO> listApiKeysByUser(Long userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void updateKeyStatus(Long keyId, ApiKeyStatus status) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + keyId));
        
        apiKey.setStatus(status);
        apiKeyRepository.save(apiKey);
        
        // Invalidate cache
        invalidateCache(apiKey.getAccessKey());
        
        log.info("Updated API key {} status to {}", apiKey.getAccessKey(), status);
    }
    
    @Override
    @Transactional
    public void deleteApiKey(Long keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + keyId));
        
        // Invalidate cache
        invalidateCache(apiKey.getAccessKey());
        
        apiKeyRepository.delete(apiKey);
        
        log.info("Deleted API key {}", apiKey.getAccessKey());
    }
    
    @Override
    @Transactional
    public void deleteApiKeyForUser(Long keyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + keyId));
        
        if (!userId.equals(apiKey.getUserId())) {
            throw new RuntimeException("API key does not belong to user");
        }
        
        // Invalidate cache
        invalidateCache(apiKey.getAccessKey());
        
        apiKeyRepository.delete(apiKey);
        
        log.info("User {} deleted API key {}", userId, apiKey.getAccessKey());
    }
    
    @Override
    public boolean validateApiKey(String accessKey, String signature, String timestamp, 
                                   String method, String path) {
        // Validate timestamp first
        if (!signatureUtil.isTimestampValid(timestamp)) {
            log.warn("Invalid timestamp for access key: {}", accessKey);
            return false;
        }
        
        // Get decrypted secret key
        Optional<String> secretKeyOpt = getDecryptedSecretKey(accessKey);
        if (secretKeyOpt.isEmpty()) {
            log.warn("Could not get secret key for access key: {}", accessKey);
            return false;
        }
        
        String secretKey = secretKeyOpt.get();
        
        // Build string to sign and verify
        String stringToSign = signatureUtil.buildStringToSign(method, path, timestamp);
        boolean valid = signatureUtil.verifySignature(stringToSign, signature, secretKey);
        
        if (valid) {
            // Update last used timestamp
            apiKeyRepository.findByAccessKey(accessKey).ifPresent(apiKey -> {
                apiKey.markUsed();
                apiKeyRepository.save(apiKey);
            });
        }
        
        return valid;
    }
    
    @Override
    public Optional<ApiKeyDTO> getApiKeyById(Long keyId) {
        return apiKeyRepository.findById(keyId).map(this::toDTO);
    }
    
    private String generateAccessKey() {
        byte[] bytes = new byte[ACCESS_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        return ACCESS_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateSecretKey() {
        byte[] bytes = new byte[SECRET_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        return SECRET_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private void invalidateCache(String accessKey) {
        String cacheKey = CACHE_KEY_PREFIX + accessKey;
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated cache for access key: {}", accessKey);
    }
    
    private ApiKeyDTO toDTO(ApiKey apiKey) {
        return ApiKeyDTO.builder()
                .id(apiKey.getId())
                .accessKey(apiKey.getAccessKey())
                .name(apiKey.getName())
                .type(apiKey.getType())
                .status(apiKey.getStatus())
                .userId(apiKey.getUserId())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}
