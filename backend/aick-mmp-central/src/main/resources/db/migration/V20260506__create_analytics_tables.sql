-- =====================================================
-- Analytics and Report Subscription Tables
-- 数据分析和报表订阅表
-- =====================================================

-- 分析数据表
CREATE TABLE IF NOT EXISTS analytics_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analytics_type VARCHAR(30) NOT NULL,
    aggregation_level VARCHAR(20) NOT NULL,
    dimension VARCHAR(50),
    dimension_value VARCHAR(100),
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,4) NOT NULL,
    extra_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analytics_type (analytics_type),
    INDEX idx_analytics_dimension (dimension),
    INDEX idx_analytics_period (period_start),
    INDEX idx_analytics_metric (metric_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析数据表';

-- 报表订阅表
CREATE TABLE IF NOT EXISTS report_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    report_type VARCHAR(30) NOT NULL,
    format VARCHAR(20) NOT NULL DEFAULT 'EXCEL',
    dimensions TEXT,
    filters TEXT,
    recipients TEXT,
    next_send_time TIMESTAMP,
    last_send_time TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subscription_report_type (report_type),
    INDEX idx_subscription_enabled (enabled),
    INDEX idx_subscription_next_send (next_send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表订阅表';

-- 报表发送历史表
CREATE TABLE IF NOT EXISTS report_send_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    send_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_data LONGTEXT,
    recipients TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message VARCHAR(500),
    INDEX idx_send_subscription (subscription_id),
    INDEX idx_send_time (send_time),
    FOREIGN KEY (subscription_id) REFERENCES report_subscriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表发送历史表';
