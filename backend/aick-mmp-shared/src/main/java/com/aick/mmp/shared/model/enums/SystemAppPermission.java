package com.aick.mmp.shared.model.enums;

/**
 * Predefined permissions for system applications.
 */
public enum SystemAppPermission {
    /**
     * Allows Edge node registration
     */
    EDGE_REGISTER,

    /**
     * Allows heartbeat reporting
     */
    EDGE_HEARTBEAT,

    /**
     * Allows receiving configuration updates
     */
    EDGE_CONFIG_UPDATE
}
