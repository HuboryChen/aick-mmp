CREATE TABLE IF NOT EXISTS ai_analysis_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL UNIQUE,
    enable_passenger BOOLEAN DEFAULT TRUE,
    enable_behavior BOOLEAN DEFAULT TRUE,
    enable_plate BOOLEAN DEFAULT TRUE,
    passenger_frame_rate INT DEFAULT 1,
    behavior_frame_rate INT DEFAULT 2,
    plate_frame_rate INT DEFAULT 5,
    loitering_threshold_seconds INT DEFAULT 30,
    gathering_min_people INT DEFAULT 5,
    enabled BOOLEAN DEFAULT TRUE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_camera (camera_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
