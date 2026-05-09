-- =====================================================
-- Alert Rules and Records Tables
-- 告警规则和告警记录表
-- =====================================================

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    alert_type VARCHAR(30) NOT NULL,
    level VARCHAR(20) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT,
    threshold_expression VARCHAR(255),
    warning_threshold DECIMAL(10,2),
    critical_threshold DECIMAL(10,2),
    duration_seconds INT DEFAULT 300,
    cooldown_seconds INT DEFAULT 600,
    alert_schedule VARCHAR(100),
    is_enabled BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by BIGINT,
    last_triggered_at TIMESTAMP,
    notification_method VARCHAR(20) DEFAULT 'IN_APP',
    notification_target VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    level VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000),
    alert_time TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED',
    target_type VARCHAR(20),
    target_id BIGINT,
    target_name VARCHAR(200),
    actual_value DECIMAL(10,2),
    threshold_value DECIMAL(10,2),
    edge_node_id BIGINT,
    camera_id BIGINT,
    camera_name VARCHAR(200),
    region_id BIGINT,
    source VARCHAR(100),
    extra_data TEXT,
    resolved_by BIGINT,
    resolved_by_username VARCHAR(100),
    resolved_at TIMESTAMP,
    resolution_note VARCHAR(500),
    duration_seconds INT,
    notification_sent BOOLEAN DEFAULT FALSE,
    notification_sent_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    acknowledged_by BIGINT,
    acknowledged_by_username VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 告警规则索引
CREATE INDEX IF NOT EXISTS idx_alert_rules_name ON alert_rules(name);
CREATE INDEX IF NOT EXISTS idx_alert_rules_type ON alert_rules(alert_type);
CREATE INDEX IF NOT EXISTS idx_alert_rules_level ON alert_rules(level);
CREATE INDEX IF NOT EXISTS idx_alert_rules_status ON alert_rules(status);
CREATE INDEX IF NOT EXISTS idx_alert_rules_target ON alert_rules(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(is_enabled);

-- 告警记录索引
CREATE INDEX IF NOT EXISTS idx_alert_records_rule_id ON alert_records(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_records_level ON alert_records(level);
CREATE INDEX IF NOT EXISTS idx_alert_records_status ON alert_records(status);
CREATE INDEX IF NOT EXISTS idx_alert_records_alert_time ON alert_records(alert_time DESC);
CREATE INDEX IF NOT EXISTS idx_alert_records_target ON alert_records(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_alert_records_camera ON alert_records(camera_id);
CREATE INDEX IF NOT EXISTS idx_alert_records_edge_node ON alert_records(edge_node_id);
