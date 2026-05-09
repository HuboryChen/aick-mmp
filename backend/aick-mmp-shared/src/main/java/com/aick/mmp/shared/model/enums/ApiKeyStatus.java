package com.aick.mmp.shared.model.enums;

/**
 * Status of an API key.
 */
public enum ApiKeyStatus {
    /**
     * Key is active and can be used for authentication
     */
    ENABLED,
    
    /**
     * Key is disabled and cannot be used for authentication
     */
    DISABLED
}
