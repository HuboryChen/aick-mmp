package com.aick.mmp.shared.model.enums;

/**
 * Type of API key - USER level or SYSTEM level.
 */
public enum ApiKeyType {
    /**
     * User-level API key, inherits user's role and permissions
     */
    USER,
    
    /**
     * System-level API key, associated with a SystemApp
     */
    SYSTEM
}
