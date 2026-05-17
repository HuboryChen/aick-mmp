CREATE TABLE IF NOT EXISTS ai_passenger_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    edge_node_id BIGINT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    enter_count INT DEFAULT 0,
    exit_count INT DEFAULT 0,
    inside_count INT DEFAULT 0,
    max_inside_count INT DEFAULT 0,
    INDEX idx_camera_time (camera_id, start_time),
    INDEX idx_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    level VARCHAR(20),
    position_data JSON,
    snapshot_url VARCHAR(500),
    description TEXT,
    event_time DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'UNRESOLVED',
    alert_record_id BIGINT,
    INDEX idx_camera_event (camera_id, event_time),
    INDEX idx_event_type (event_type, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_vehicle_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    plate_number VARCHAR(50) NOT NULL,
    plate_color VARCHAR(20),
    confidence DECIMAL(5,4),
    snapshot_url VARCHAR(500),
    is_whitelisted BOOLEAN DEFAULT FALSE,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    detect_time DATETIME NOT NULL,
    INDEX idx_plate (plate_number),
    INDEX idx_time (detect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_vehicle_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    plate_color VARCHAR(20),
    owner_name VARCHAR(100),
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
