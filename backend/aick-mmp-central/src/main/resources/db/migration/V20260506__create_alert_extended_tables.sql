-- =====================================================
-- Alert Extended Tables
-- 告警扩展表
-- =====================================================

-- 告警条件表
CREATE TABLE IF NOT EXISTS alert_conditions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    condition_type VARCHAR(30) NOT NULL,
    operator VARCHAR(10) NOT NULL,
    threshold_value DECIMAL(15,4) NOT NULL,
    metric_name VARCHAR(100),
    condition_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_condition_rule (rule_id),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警条件表';

-- 告警通知配置表
CREATE TABLE IF NOT EXISTS alert_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    target VARCHAR(500),
    template_id VARCHAR(100),
    is_enabled BOOLEAN DEFAULT TRUE,
    retry_count INT DEFAULT 3,
    retry_interval_seconds INT DEFAULT 60,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_rule (rule_id),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警通知配置表';

-- 告警升级规则表
CREATE TABLE IF NOT EXISTS alert_escalations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    escalation_level INT NOT NULL,
    delay_minutes INT NOT NULL DEFAULT 30,
    escalation_type VARCHAR(30) NOT NULL,
    escalation_target VARCHAR(500),
    escalation_template VARCHAR(100),
    notify_original_responders BOOLEAN DEFAULT TRUE,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_escalation_rule (rule_id),
    INDEX idx_escalation_level (escalation_level),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警升级规则表';

-- 告警规则模板表
CREATE TABLE IF NOT EXISTS alert_rule_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    alert_type VARCHAR(30) NOT NULL,
    default_level VARCHAR(20) NOT NULL,
    default_conditions TEXT,
    default_actions TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_code (template_code),
    INDEX idx_template_type (alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则模板表';

-- 通知发送日志表
CREATE TABLE IF NOT EXISTS notification_send_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(500),
    content TEXT,
    send_status VARCHAR(20) NOT NULL,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_message VARCHAR(500),
    retry_count INT DEFAULT 0,
    INDEX idx_sendlog_notification (notification_id),
    INDEX idx_sendlog_status (send_status),
    INDEX idx_sendlog_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知发送日志表';

-- 配置变更历史表
CREATE TABLE IF NOT EXISTS config_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    change_type VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    changed_by_username VARCHAR(100),
    change_reason VARCHAR(500),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_config (config_id),
    INDEX idx_history_key (config_key),
    INDEX idx_history_time (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置变更历史表';
