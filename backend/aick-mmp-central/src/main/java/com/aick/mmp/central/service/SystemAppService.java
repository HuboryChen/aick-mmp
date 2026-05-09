package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.SystemAppCredentialsResponseDTO;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.Set;

/**
 * Service for managing system applications.
 */
public interface SystemAppService {
    
    /**
     * Create a new system application with credentials.
     *
     * @param name the app name
     * @param description the app description
     * @param permissions the permissions to grant
     * @param ownerType the owner type
     * @param ownerId the owner ID (for USER type)
     * @param createdBy the ID of the user creating this app
     * @return the credentials (with secret shown only once)
     */
    SystemAppCredentialsResponseDTO createSystemAppWithCredentials(String name, String description, 
                                                                     Set<SystemAppPermission> permissions,
                                                                     String ownerType, Long ownerId, Long createdBy);
    
    /**
     * Create a new system application.
     *
     * @param name the app name
     * @param description the app description
     * @param permissions the permissions to grant
     * @param ownerType the owner type
     * @param ownerId the owner ID (for USER type)
     * @param createdBy the ID of the user creating this app
     * @return the created system app
     */
    SystemApp createSystemApp(String name, String description, Set<SystemAppPermission> permissions,
                               String ownerType, Long ownerId, Long createdBy);
    
    /**
     * Get system app by ID.
     *
     * @param id the app ID
     * @return the system app
     */
    Optional<SystemApp> getSystemApp(Long id);
    
    /**
     * Get system app by app key.
     *
     * @param appKey the app key
     * @return the system app
     */
    Optional<SystemApp> getSystemAppByKey(String appKey);
    
    /**
     * List system apps with pagination.
     *
     * @param page page number
     * @param size page size
     * @param ownerType optional filter by owner type
     * @param status optional filter by status
     * @return paginated list of system apps
     */
    Page<SystemApp> listSystemApps(int page, int size, String ownerType, String status);
    
    /**
     * Update system app.
     *
     * @param id the app ID
     * @param name the new name
     * @param description the new description
     * @param permissions the new permissions
     * @param status the new status
     * @return the updated system app
     */
    SystemApp updateSystemApp(Long id, String name, String description, 
                               Set<SystemAppPermission> permissions, String status);
    
    /**
     * Delete system app.
     *
     * @param id the app ID
     */
    void deleteSystemApp(Long id);
    
    /**
     * Validate permissions against predefined set.
     *
     * @param permissions the permissions to validate
     * @throws IllegalArgumentException if invalid permissions found
     */
    void validatePermissions(Set<SystemAppPermission> permissions);
    
    /**
     * Get decrypted app secret for a system app.
     *
     * @param appId the app ID
     * @return the decrypted secret key
     */
    Optional<String> getDecryptedAppSecret(Long appId);
    
    /**
     * Get decrypted app secret by app key.
     *
     * @param appKey the app key
     * @return the decrypted secret key
     */
    Optional<String> getDecryptedAppSecretByKey(String appKey);
    
    /**
     * Regenerate credentials for a system app.
     *
     * @param appId the app ID
     * @return the new credentials (with secret shown only once)
     */
    SystemAppCredentialsResponseDTO regenerateCredentials(Long appId);
    
    /**
     * Validate system app credentials for authentication.
     *
     * @param appKey the app key
     * @param signature the provided signature
     * @param timestamp the request timestamp
     * @param method HTTP method
     * @param path request path
     * @return true if credentials are valid
     */
    boolean validateAppCredentials(String appKey, String signature, String timestamp, String method, String path);
}
