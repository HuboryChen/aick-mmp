package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ApiKeyCreatedResponseDTO;
import com.aick.mmp.central.dto.ApiKeyDTO;
import com.aick.mmp.central.dto.CreateApiKeyRequestDTO;
import com.aick.mmp.shared.model.enums.ApiKeyStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing API keys (AK/SK) - USER type only.
 * System-level credentials are managed in SystemAppService.
 */
public interface ApiKeyService {
    
    /**
     * Create a new API key for a user.
     *
     * @param userId the user ID
     * @param request the creation request
     * @return the created key with secret (one-time display)
     */
    ApiKeyCreatedResponseDTO createApiKeyForUser(Long userId, CreateApiKeyRequestDTO request);
    
    /**
     * Get decrypted secret key (uses Redis cache).
     *
     * @param accessKey the access key
     * @return the decrypted secret key, or empty if not found
     */
    Optional<String> getDecryptedSecretKey(String accessKey);
    
    /**
     * List all API keys for a user.
     *
     * @param userId the user ID
     * @return list of API keys (without secrets)
     */
    List<ApiKeyDTO> listApiKeysByUser(Long userId);
    
    /**
     * Update API key status.
     *
     * @param keyId the key ID
     * @param status the new status
     */
    void updateKeyStatus(Long keyId, ApiKeyStatus status);
    
    /**
     * Delete an API key.
     *
     * @param keyId the key ID
     */
    void deleteApiKey(Long keyId);
    
    /**
     * Delete an API key owned by a specific user.
     *
     * @param keyId the key ID
     * @param userId the user ID (for ownership verification)
     */
    void deleteApiKeyForUser(Long keyId, Long userId);
    
    /**
     * Validate an API key for authentication.
     *
     * @param accessKey the access key
     * @param signature the provided signature
     * @param timestamp the request timestamp
     * @param method HTTP method
     * @param path request path
     * @return true if the key is valid
     */
    boolean validateApiKey(String accessKey, String signature, String timestamp, String method, String path);
    
    /**
     * Get API key DTO by ID.
     *
     * @param keyId the key ID
     * @return the API key DTO
     */
    Optional<ApiKeyDTO> getApiKeyById(Long keyId);
}
