-- Video Wall User Preferences Table
-- Stores individual user video wall preferences

CREATE TABLE video_wall_preferences (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE,
    layout          VARCHAR(10) NOT NULL DEFAULT '4',
    quality         VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate         INT DEFAULT 2048,
    camera_ids      JSON COMMENT 'Array of selected camera IDs',
    auto_apply      BOOLEAN DEFAULT TRUE,
    last_preset_id  BIGINT COMMENT 'Last applied preset ID',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_video_wall_prefs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_wall_prefs_preset FOREIGN KEY (last_preset_id) REFERENCES video_wall_presets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Video wall user preferences';

-- Index for faster preference lookups
CREATE INDEX idx_video_wall_preferences_user_id ON video_wall_preferences(user_id);
