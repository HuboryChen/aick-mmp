-- 创建系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_name VARCHAR(100) NOT NULL,
    description TEXT,
    config_value TEXT,
    default_value TEXT,
    value_type VARCHAR(20),
    category VARCHAR(30),
    config_group VARCHAR(50),
    options TEXT,
    min_value DOUBLE,
    max_value DOUBLE,
    required BOOLEAN DEFAULT FALSE,
    editable BOOLEAN DEFAULT TRUE,
    `sensitive` BOOLEAN DEFAULT FALSE,
    validation_rule VARCHAR(500),
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 创建索引
CREATE INDEX idx_config_category ON system_configs(category);
CREATE INDEX idx_config_group ON system_configs(config_group);
CREATE INDEX idx_config_enabled ON system_configs(enabled);
