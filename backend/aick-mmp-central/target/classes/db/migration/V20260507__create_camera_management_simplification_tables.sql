-- Create camera_config_templates table
CREATE TABLE IF NOT EXISTS camera_config_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand VARCHAR(100) NOT NULL COMMENT 'Camera brand name',
    model VARCHAR(100) NOT NULL COMMENT 'Camera model name',
    protocol VARCHAR(20) NOT NULL COMMENT 'Protocol type: RTSP, ONVIF, GB28181, HTTP, RTMP',
    default_port INT NOT NULL COMMENT 'Default port for the protocol',
    url_path_template VARCHAR(500) NOT NULL COMMENT 'URL path template with placeholders like {ip}, {port}, {username}, {password}',
    preset_parameters JSON COMMENT 'Preset parameters as JSON',
    is_preset BOOLEAN DEFAULT FALSE COMMENT 'Whether this is a preset template',
    usage_count INT DEFAULT 0 COMMENT 'Number of times this template has been used',
    last_used_at TIMESTAMP NULL COMMENT 'Last time this template was used',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT 'Soft delete flag',
    deleted_at TIMESTAMP NULL COMMENT 'When the template was soft deleted',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    UNIQUE KEY uk_brand_model (brand, model, is_deleted),
    INDEX idx_brand (brand),
    INDEX idx_protocol (protocol),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Camera configuration templates';

-- Create camera_discovery_tasks table
CREATE TABLE IF NOT EXISTS camera_discovery_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT 'User who created the scan task',
    network_segment VARCHAR(50) NOT NULL COMMENT 'Network segment in CIDR format, e.g., 192.168.1.0/24',
    status VARCHAR(20) NOT NULL COMMENT 'Task status: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED',
    progress INT DEFAULT 0 COMMENT 'Scan progress percentage (0-100)',
    total_ips INT DEFAULT 0 COMMENT 'Total number of IPs to scan',
    found_devices JSON COMMENT 'Discovered devices as JSON array',
    started_at TIMESTAMP NULL COMMENT 'When the scan started',
    completed_at TIMESTAMP NULL COMMENT 'When the scan completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Camera network discovery scan tasks';

-- Create camera_batch_import_tasks table
CREATE TABLE IF NOT EXISTS camera_batch_import_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT 'User who initiated the import',
    file_name VARCHAR(255) NOT NULL COMMENT 'Uploaded file name',
    status VARCHAR(20) NOT NULL COMMENT 'Task status: PENDING, VALIDATING, IMPORTING, COMPLETED, FAILED, CANCELLED',
    total_records INT DEFAULT 0 COMMENT 'Total number of records to import',
    success_count INT DEFAULT 0 COMMENT 'Number of successfully imported records',
    fail_count INT DEFAULT 0 COMMENT 'Number of failed records',
    error_details JSON COMMENT 'Detailed error information as JSON',
    started_at TIMESTAMP NULL COMMENT 'When the import started',
    completed_at TIMESTAMP NULL COMMENT 'When the import completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Camera batch import tasks';
