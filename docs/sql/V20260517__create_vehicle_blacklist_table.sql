-- AiVehicleBlacklist table for managing blocked license plates
-- When a blacklisted vehicle is detected, a CRITICAL alert is triggered.

CREATE TABLE IF NOT EXISTS ai_vehicle_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    plate_color VARCHAR(20),
    reason TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blacklist_plate ON ai_vehicle_blacklist(plate_number);
CREATE INDEX idx_blacklist_enabled ON ai_vehicle_blacklist(enabled);
