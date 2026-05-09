-- Video Wall Presets Table
-- Stores user-defined video wall presets

CREATE TABLE video_wall_presets (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    preset_name     VARCHAR(50) NOT NULL,
    layout          VARCHAR(10) NOT NULL DEFAULT '4',
    quality         VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate         INT DEFAULT 2048,
    camera_ids      JSON COMMENT 'Array of camera IDs',
    is_default      BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_video_wall_presets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_preset_name UNIQUE (user_id, preset_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Video wall presets';

-- Index for faster user preset lookups
CREATE INDEX idx_video_wall_presets_user_id ON video_wall_presets(user_id);
CREATE INDEX idx_video_wall_presets_sort ON video_wall_presets(user_id, sort_order);
