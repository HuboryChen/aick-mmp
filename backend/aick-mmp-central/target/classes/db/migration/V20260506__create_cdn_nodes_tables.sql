-- =====================================================
-- CDN Nodes and Load History Tables
-- CDN节点和负载历史表
-- =====================================================

-- CDN节点表
CREATE TABLE IF NOT EXISTS cdn_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    location VARCHAR(255),
    region_id BIGINT,
    capacity INT DEFAULT 100,
    current_load INT DEFAULT 0,
    last_heartbeat TIMESTAMP,
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    bandwidth_usage DECIMAL(5,2),
    storage_usage DECIMAL(5,2),
    up_bandwidth INT,
    down_bandwidth INT,
    weight INT DEFAULT 100,
    priority INT DEFAULT 100,
    health_check_url VARCHAR(500),
    connect_timeout INT DEFAULT 5000,
    read_timeout INT DEFAULT 10000,
    is_enabled BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cdn_nodes_status (status),
    INDEX idx_cdn_nodes_region (region_id),
    INDEX idx_cdn_nodes_enabled (is_enabled),
    INDEX idx_cdn_nodes_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN节点表';

-- CDN节点负载历史表
CREATE TABLE IF NOT EXISTS cdn_node_load_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cdn_node_id BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_load INT DEFAULT 0,
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    bandwidth_usage DECIMAL(5,2),
    storage_usage DECIMAL(5,2),
    up_bandwidth INT,
    down_bandwidth INT,
    active_connections INT DEFAULT 0,
    request_rate DECIMAL(10,2),
    bandwidth_throughput DECIMAL(15,2),
    cache_hit_rate DECIMAL(5,2),
    avg_response_time DECIMAL(10,2),
    error_rate DECIMAL(5,2),
    status VARCHAR(20),
    load_percentage DECIMAL(5,2),
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_load_node (cdn_node_id),
    INDEX idx_load_recorded (recorded_at),
    INDEX idx_load_status (status),
    FOREIGN KEY (cdn_node_id) REFERENCES cdn_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN节点负载历史表';

-- CDN节点通知日志表
CREATE TABLE IF NOT EXISTS cdn_node_notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cdn_node_id BIGINT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    notification_level VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_node (cdn_node_id),
    INDEX idx_notification_sent (sent_at),
    FOREIGN KEY (cdn_node_id) REFERENCES cdn_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN节点通知日志表';
