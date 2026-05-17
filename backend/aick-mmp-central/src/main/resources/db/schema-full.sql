-- ============================================================
-- Database: aick_mmp_central
-- Dialect: MySQL 8.0
-- Description: Complete DDL snapshot — all 41 tables
-- Generated: 2026-05-17
--
-- This file is the single source of truth for the database
-- schema. When making schema changes:
--   1. Update this file first
--   2. Then add an incremental migration script
-- ============================================================

-- ============================================================
-- Module 1: Shared Domain (核心业务实体)
-- Tables in: aick-mmp-shared module
-- ============================================================

-- ----------------------------------------
-- users — 用户表
-- ----------------------------------------
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    phone           VARCHAR(255),
    department      VARCHAR(255),
    role            VARCHAR(255) NOT NULL COMMENT 'ADMIN / OPERATOR / VIEWER',
    status          VARCHAR(255) NOT NULL COMMENT 'ACTIVE / INACTIVE / LOCKED / EXPIRED',
    last_login_time  DATETIME,
    last_login_ip   VARCHAR(255),
    is_enabled      TINYINT(1) DEFAULT 1,
    login_failed_count INT DEFAULT 0,
    locked_until    DATETIME,
    created_at      DATETIME,
    updated_at      DATETIME,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------------------
-- system_apps — 系统应用表
-- ----------------------------------------
CREATE TABLE system_apps (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_key          VARCHAR(64) UNIQUE,
    encrypted_secret VARCHAR(256) COMMENT 'AES-256-GCM encrypted',
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500),
    owner_type       VARCHAR(255) NOT NULL COMMENT 'SYSTEM / USER',
    owner_id         BIGINT,
    status           VARCHAR(255) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE / SUSPENDED',
    created_by       BIGINT,
    created_at       DATETIME,
    updated_at       DATETIME,
    last_used_at     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统应用表';

CREATE INDEX idx_system_apps_app_key ON system_apps(app_key);

-- ----------------------------------------
-- system_app_permissions — 系统应用权限关联表
-- ----------------------------------------
CREATE TABLE system_app_permissions (
    app_id      BIGINT NOT NULL,
    permission  VARCHAR(255) NOT NULL COMMENT 'EDGE_REGISTER / EDGE_HEARTBEAT / EDGE_CONFIG_UPDATE',
    FOREIGN KEY (app_id) REFERENCES system_apps(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统应用权限关联表';

-- ----------------------------------------
-- api_keys — API 密钥表 (AK/SK for USER type)
-- ----------------------------------------
CREATE TABLE api_keys (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    access_key       VARCHAR(64) NOT NULL,
    encrypted_secret VARCHAR(256) NOT NULL COMMENT 'AES-256-GCM encrypted',
    user_id          BIGINT,
    key_type         VARCHAR(255) NOT NULL COMMENT 'USER / SYSTEM',
    name             VARCHAR(100),
    status           VARCHAR(255) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
    last_used_at     DATETIME,
    expires_at       DATETIME,
    created_at       DATETIME,
    UNIQUE KEY idx_api_key_access_key (access_key),
    INDEX idx_api_key_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 密钥表';

-- ----------------------------------------
-- cameras — 摄像头表
-- ----------------------------------------
CREATE TABLE cameras (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    location            VARCHAR(255),
    edge_node_id        BIGINT,
    protocol            VARCHAR(255) NOT NULL COMMENT 'RTSP / ONVIF / GB28181 / HTTP / RTMP',
    connection_url      VARCHAR(255) NOT NULL,
    username            VARCHAR(255),
    password            VARCHAR(255),
    resolution          VARCHAR(255),
    frame_rate          INT,
    bitrate             INT,
    current_bitrate     INT COMMENT '当前实际码率',
    current_fps         DOUBLE COMMENT '当前实际帧率',
    last_error_code     VARCHAR(255),
    last_error_message  VARCHAR(255),
    last_heartbeat_time DATETIME,
    compression         VARCHAR(255) COMMENT 'H.264 / H.265',
    audio_enabled       TINYINT(1),
    status              VARCHAR(255) NOT NULL COMMENT 'ONLINE / OFFLINE / CONNECTING / ERROR / MAINTENANCE / PENDING_ALLOCATION',
    last_active_time    DATETIME,
    is_enabled          TINYINT(1) DEFAULT 1,
    region_id           BIGINT,
    is_deleted          TINYINT(1) DEFAULT 0,
    deleted_at          TIMESTAMP NULL,
    created_at          DATETIME,
    updated_at          DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头表';

CREATE INDEX idx_cameras_deleted_at ON cameras(deleted_at);

-- ----------------------------------------
-- edge_nodes — 边缘节点表
-- ----------------------------------------
CREATE TABLE edge_nodes (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    location            VARCHAR(255) NOT NULL COMMENT '详细地址',
    region_id           BIGINT,
    ip_address          VARCHAR(255) NOT NULL,
    port                INT NOT NULL,
    app_id              BIGINT COMMENT '关联系统应用ID',
    registered_at       DATETIME,
    status              VARCHAR(255) NOT NULL COMMENT 'ONLINE / OFFLINE / CONNECTING / ERROR / MAINTENANCE / UPGRADING',
    last_heartbeat_time DATETIME,
    cpu_usage           DOUBLE,
    memory_usage        DOUBLE,
    storage_usage       DOUBLE,
    max_camera_support  INT,
    current_camera_count INT,
    software_version    VARCHAR(255),
    hardware_info       VARCHAR(255),
    network_bandwidth   VARCHAR(255),
    system_metrics      JSON,
    is_enabled          TINYINT(1) DEFAULT 1,
    created_at          DATETIME,
    updated_at          DATETIME,
    UNIQUE KEY uk_edge_node_uuid (uuid),
    UNIQUE KEY uk_edge_node_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='边缘节点表';

-- ----------------------------------------
-- recordings — 录像记录表
-- ----------------------------------------
CREATE TABLE recordings (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id        BIGINT NOT NULL,
    name             VARCHAR(255) NOT NULL,
    file_path        VARCHAR(255) NOT NULL,
    file_size        BIGINT,
    md5              VARCHAR(32),
    storage_path     VARCHAR(500),
    integrity_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING / COMPLETED / CORRUPTED / DELETED',
    lock_status      TINYINT(1) DEFAULT 0,
    start_time       DATETIME NOT NULL,
    end_time         DATETIME,
    duration         INT COMMENT '录像时长(秒)',
    recording_type   VARCHAR(20) COMMENT 'continuous / timed / motion / alert',
    status           VARCHAR(20) COMMENT 'recording / completed / failed / deleted',
    format           VARCHAR(10),
    resolution       VARCHAR(20),
    created_at       DATETIME,
    updated_at       DATETIME,
    is_deleted       TINYINT(1) DEFAULT 0,
    deleted_at       TIMESTAMP NULL,
    orphaned_at      TIMESTAMP NULL COMMENT '关联摄像头被删除时标记',
    orphaned_by      BIGINT COMMENT '触发孤立的摄像头ID',
    INDEX idx_recording_camera (camera_id),
    INDEX idx_recording_start_time (start_time),
    INDEX idx_recording_status (status),
    INDEX idx_recording_integrity_status (integrity_status),
    INDEX idx_recording_lock_status (lock_status),
    INDEX idx_recordings_is_deleted (is_deleted),
    INDEX idx_recordings_deleted_at (is_deleted, deleted_at),
    INDEX idx_recordings_orphaned_at (orphaned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像记录表';

-- ----------------------------------------
-- stream_sessions — 流会话表
-- ----------------------------------------
CREATE TABLE stream_sessions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id                BIGINT NOT NULL,
    edge_node_id             BIGINT,
    edge_node_status         VARCHAR(255),
    cdn_node_id              VARCHAR(255),
    has_motion_detected      TINYINT(1) DEFAULT 0,
    ai_event_count           INT DEFAULT 0,
    motion_detection_enabled TINYINT(1) DEFAULT 0,
    ai_processing_enabled    TINYINT(1) DEFAULT 0,
    last_network_update      DATETIME,
    session_id               VARCHAR(255) NOT NULL,
    stream_url               VARCHAR(255),
    status                   VARCHAR(255) NOT NULL COMMENT 'INITIATED / CONNECTING / STREAMING / PAUSED / DISCONNECTED / ERROR',
    protocol                 VARCHAR(255),
    bitrate                  INT,
    resolution               VARCHAR(255),
    frame_rate               INT,
    bytes_transferred        BIGINT DEFAULT 0,
    is_recording             TINYINT(1) DEFAULT 0,
    start_time               DATETIME,
    end_time                 DATETIME,
    created_at               DATETIME,
    updated_at               DATETIME,
    UNIQUE KEY uk_stream_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流会话表';


-- ============================================================
-- Module 2: Central Management (中心管理)
-- Tables in: aick-mmp-central module
-- ============================================================

-- ----------------------------------------
-- regions — 区域表
-- ----------------------------------------
CREATE TABLE regions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    parent_id   BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_region_code (code),
    FOREIGN KEY (parent_id) REFERENCES regions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区域表';

-- ----------------------------------------
-- camera_failover_events — 摄像头故障转移事件表
-- ----------------------------------------
CREATE TABLE camera_failover_events (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_edge_node_id BIGINT NOT NULL,
    total_count         INT NOT NULL,
    success_count       INT NOT NULL DEFAULT 0,
    failed_count        INT NOT NULL DEFAULT 0,
    trigger_type        VARCHAR(20) NOT NULL COMMENT 'AUTO / MANUAL',
    status              VARCHAR(20) NOT NULL COMMENT 'IN_PROGRESS / COMPLETED / PARTIAL / FAILED',
    error_message       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP NULL,
    INDEX idx_failover_events_source_node (source_edge_node_id),
    INDEX idx_failover_events_trigger_type (trigger_type),
    INDEX idx_failover_events_status (status),
    INDEX idx_failover_events_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头故障转移事件表';

-- ----------------------------------------
-- failover_event_target_nodes — 故障转移目标节点关联表
-- ----------------------------------------
CREATE TABLE failover_event_target_nodes (
    event_id            BIGINT NOT NULL,
    target_edge_node_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, target_edge_node_id),
    FOREIGN KEY (event_id) REFERENCES camera_failover_events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故障转移目标节点关联表';

-- ----------------------------------------
-- failover_event_cameras — 故障转移摄像头关联表
-- ----------------------------------------
CREATE TABLE failover_event_cameras (
    event_id   BIGINT NOT NULL,
    camera_id  BIGINT NOT NULL,
    PRIMARY KEY (event_id, camera_id),
    FOREIGN KEY (event_id) REFERENCES camera_failover_events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故障转移摄像头关联表';

-- ----------------------------------------
-- alert_rules — 告警规则表
-- ----------------------------------------
CREATE TABLE alert_rules (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    alert_type          VARCHAR(30) NOT NULL,
    level               VARCHAR(20) NOT NULL,
    target_type         VARCHAR(20) NOT NULL,
    target_id           BIGINT,
    threshold_expression VARCHAR(255),
    warning_threshold   DECIMAL(10,2),
    critical_threshold  DECIMAL(10,2),
    duration_seconds    INT DEFAULT 300,
    cooldown_seconds    INT DEFAULT 600,
    alert_schedule      VARCHAR(100),
    is_enabled          TINYINT(1) DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by          BIGINT,
    last_triggered_at   TIMESTAMP NULL,
    notification_method VARCHAR(20) DEFAULT 'IN_APP',
    notification_target VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_alert_rule_name (name),
    INDEX idx_alert_rules_type (alert_type),
    INDEX idx_alert_rules_level (level),
    INDEX idx_alert_rules_status (status),
    INDEX idx_alert_rules_target (target_type, target_id),
    INDEX idx_alert_rules_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- ----------------------------------------
-- alert_records — 告警记录表
-- ----------------------------------------
CREATE TABLE alert_records (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id                 BIGINT NOT NULL,
    rule_name               VARCHAR(100) NOT NULL,
    alert_type              VARCHAR(30) NOT NULL,
    level                   VARCHAR(20) NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    message                 VARCHAR(1000),
    alert_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status                  VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED',
    target_type             VARCHAR(20),
    target_id               BIGINT,
    target_name             VARCHAR(200),
    actual_value            DECIMAL(10,2),
    threshold_value         DECIMAL(10,2),
    edge_node_id            BIGINT,
    camera_id               BIGINT,
    camera_name             VARCHAR(200),
    region_id               BIGINT,
    source                  VARCHAR(100),
    extra_data              TEXT,
    resolved_by             BIGINT,
    resolved_by_username    VARCHAR(100),
    resolved_at             TIMESTAMP NULL,
    resolution_note         VARCHAR(500),
    duration_seconds        INT,
    notification_sent       TINYINT(1) DEFAULT 0,
    notification_sent_at    TIMESTAMP NULL,
    acknowledged_at         TIMESTAMP NULL,
    acknowledged_by         BIGINT,
    acknowledged_by_username VARCHAR(100),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alert_records_rule_id (rule_id),
    INDEX idx_alert_records_level (level),
    INDEX idx_alert_records_status (status),
    INDEX idx_alert_records_alert_time (alert_time),
    INDEX idx_alert_records_target (target_type, target_id),
    INDEX idx_alert_records_camera (camera_id),
    INDEX idx_alert_records_edge_node (edge_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

-- ----------------------------------------
-- system_configs — 系统配置表
-- ----------------------------------------
CREATE TABLE system_configs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key       VARCHAR(100) NOT NULL,
    config_name      VARCHAR(100) NOT NULL,
    description      TEXT,
    config_value     TEXT,
    default_value    TEXT,
    value_type       VARCHAR(20),
    category         VARCHAR(30),
    config_group     VARCHAR(50),
    options          TEXT,
    min_value        DOUBLE,
    max_value        DOUBLE,
    required         TINYINT(1) DEFAULT 0,
    editable         TINYINT(1) DEFAULT 1,
    `sensitive`      TINYINT(1) DEFAULT 0,
    validation_rule  VARCHAR(500),
    sort_order       INT DEFAULT 0,
    enabled          TINYINT(1) DEFAULT 1,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT,
    updated_by       BIGINT,
    UNIQUE KEY uk_config_key (config_key),
    INDEX idx_config_category (category),
    INDEX idx_config_group (config_group),
    INDEX idx_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ----------------------------------------
-- recording_schedules — 录像计划主表
-- ----------------------------------------
CREATE TABLE recording_schedules (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL COMMENT '计划名称',
    camera_id         BIGINT NOT NULL COMMENT '摄像头ID',
    schedule_type     VARCHAR(20) NOT NULL COMMENT 'CONTINUOUS / TIMED / MOTION / EVENT / SMART',
    enabled           TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    motion_sensitivity INT DEFAULT 50 COMMENT '移动侦测灵敏度 (0-100)',
    retention_days    INT DEFAULT 30 COMMENT '录像保留天数',
    description       VARCHAR(500) COMMENT '备注说明',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recording_schedule_camera (camera_id),
    INDEX idx_recording_schedule_enabled (enabled),
    INDEX idx_recording_schedule_type (schedule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像计划主表';

-- ----------------------------------------
-- recording_schedule_time_slots — 录像时间段表
-- ----------------------------------------
CREATE TABLE recording_schedule_time_slots (
    schedule_id BIGINT NOT NULL COMMENT '录像计划ID',
    slot_order  INT NOT NULL COMMENT '时间段顺序',
    start_time  TIME,
    end_time    TIME,
    quality     VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'HIGH / MEDIUM / LOW',
    PRIMARY KEY (schedule_id, slot_order),
    FOREIGN KEY (schedule_id) REFERENCES recording_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像时间段表';

-- ----------------------------------------
-- recording_schedule_days — 录像日期表
-- ----------------------------------------
CREATE TABLE recording_schedule_days (
    schedule_id BIGINT NOT NULL COMMENT '录像计划ID',
    day_of_week VARCHAR(10) NOT NULL COMMENT 'MONDAY / TUESDAY / WEDNESDAY / THURSDAY / FRIDAY / SATURDAY / SUNDAY',
    PRIMARY KEY (schedule_id, day_of_week),
    FOREIGN KEY (schedule_id) REFERENCES recording_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像日期表';

-- ----------------------------------------
-- alert_conditions — 告警条件表
-- ----------------------------------------
CREATE TABLE alert_conditions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    condition_type  VARCHAR(30) NOT NULL,
    operator        VARCHAR(10) NOT NULL,
    threshold_value DECIMAL(15,4) NOT NULL,
    metric_name     VARCHAR(100),
    condition_order INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_condition_rule (rule_id),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警条件表';

-- ----------------------------------------
-- alert_notifications — 告警通知配置表
-- ----------------------------------------
CREATE TABLE alert_notifications (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id                BIGINT NOT NULL,
    channel                VARCHAR(30) NOT NULL,
    target                 VARCHAR(500),
    template_id            VARCHAR(100),
    is_enabled             TINYINT(1) DEFAULT 1,
    retry_count            INT DEFAULT 3,
    retry_interval_seconds INT DEFAULT 60,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_rule (rule_id),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警通知配置表';

-- ----------------------------------------
-- alert_escalations — 告警升级规则表
-- ----------------------------------------
CREATE TABLE alert_escalations (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id                   BIGINT NOT NULL,
    escalation_level          INT NOT NULL,
    delay_minutes             INT NOT NULL DEFAULT 30,
    escalation_type           VARCHAR(30) NOT NULL,
    escalation_target         VARCHAR(500),
    escalation_template       VARCHAR(100),
    notify_original_responders TINYINT(1) DEFAULT 1,
    is_enabled                TINYINT(1) DEFAULT 1,
    created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_escalation_rule (rule_id),
    INDEX idx_escalation_level (escalation_level),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警升级规则表';

-- ----------------------------------------
-- alert_rule_templates — 告警规则模板表
-- ----------------------------------------
CREATE TABLE alert_rule_templates (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code     VARCHAR(50) NOT NULL,
    template_name     VARCHAR(100) NOT NULL,
    description       VARCHAR(500),
    alert_type        VARCHAR(30) NOT NULL,
    default_level     VARCHAR(20) NOT NULL,
    default_conditions TEXT,
    default_actions   TEXT,
    is_system         TINYINT(1) DEFAULT 0,
    is_active         TINYINT(1) DEFAULT 1,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_template_type (alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则模板表';

-- ----------------------------------------
-- notification_send_logs — 通知发送日志表
-- ----------------------------------------
CREATE TABLE notification_send_logs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id   BIGINT NOT NULL,
    channel           VARCHAR(30) NOT NULL,
    recipient         VARCHAR(500),
    content           TEXT,
    send_status       VARCHAR(20) NOT NULL,
    send_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_message  VARCHAR(500),
    retry_count       INT DEFAULT 0,
    INDEX idx_sendlog_notification (notification_id),
    INDEX idx_sendlog_status (send_status),
    INDEX idx_sendlog_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知发送日志表';

-- ----------------------------------------
-- config_history — 配置变更历史表
-- ----------------------------------------
CREATE TABLE config_history (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id         BIGINT NOT NULL,
    config_key        VARCHAR(100) NOT NULL,
    old_value         TEXT,
    new_value         TEXT,
    change_type       VARCHAR(20) NOT NULL,
    changed_by        BIGINT,
    changed_by_username VARCHAR(100),
    change_reason     VARCHAR(500),
    changed_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_config (config_id),
    INDEX idx_history_key (config_key),
    INDEX idx_history_time (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置变更历史表';

-- ----------------------------------------
-- analytics_data — 分析数据表
-- ----------------------------------------
CREATE TABLE analytics_data (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    analytics_type    VARCHAR(30) NOT NULL,
    aggregation_level VARCHAR(20) NOT NULL,
    dimension         VARCHAR(50),
    dimension_value   VARCHAR(100),
    period_start      TIMESTAMP NOT NULL,
    period_end        TIMESTAMP NOT NULL,
    metric_name       VARCHAR(100) NOT NULL,
    metric_value      DECIMAL(20,4) NOT NULL,
    extra_data        TEXT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analytics_type (analytics_type),
    INDEX idx_analytics_dimension (dimension),
    INDEX idx_analytics_period (period_start),
    INDEX idx_analytics_metric (metric_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析数据表';

-- ----------------------------------------
-- report_subscriptions — 报表订阅表
-- ----------------------------------------
CREATE TABLE report_subscriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    report_type     VARCHAR(30) NOT NULL,
    format          VARCHAR(20) NOT NULL DEFAULT 'EXCEL',
    dimensions      TEXT,
    filters         TEXT,
    recipients      TEXT,
    next_send_time  TIMESTAMP NULL,
    last_send_time  TIMESTAMP NULL,
    enabled         TINYINT(1) DEFAULT 1,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subscription_report_type (report_type),
    INDEX idx_subscription_enabled (enabled),
    INDEX idx_subscription_next_send (next_send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表订阅表';

-- ----------------------------------------
-- report_send_history — 报表发送历史表
-- ----------------------------------------
CREATE TABLE report_send_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    send_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_data     LONGTEXT,
    recipients      TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message   VARCHAR(500),
    INDEX idx_send_subscription (subscription_id),
    INDEX idx_send_time (send_time),
    FOREIGN KEY (subscription_id) REFERENCES report_subscriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表发送历史表';

-- ----------------------------------------
-- cdn_nodes — CDN 节点表
-- ----------------------------------------
CREATE TABLE cdn_nodes (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id           VARCHAR(50) NOT NULL,
    name              VARCHAR(100) NOT NULL,
    ip_address        VARCHAR(45) NOT NULL,
    port              INT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    location          VARCHAR(255),
    region_id         BIGINT,
    capacity          INT DEFAULT 100,
    current_load      INT DEFAULT 0,
    last_heartbeat    TIMESTAMP NULL,
    cpu_usage         DECIMAL(5,2),
    memory_usage      DECIMAL(5,2),
    bandwidth_usage   DECIMAL(5,2),
    storage_usage     DECIMAL(5,2),
    up_bandwidth      INT,
    down_bandwidth    INT,
    weight            INT DEFAULT 100,
    priority          INT DEFAULT 100,
    health_check_url  VARCHAR(500),
    connect_timeout   INT DEFAULT 5000,
    read_timeout      INT DEFAULT 10000,
    is_enabled        TINYINT(1) DEFAULT 1,
    is_deleted        TINYINT(1) DEFAULT 0,
    deleted_at        TIMESTAMP NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdn_node_id (node_id),
    INDEX idx_cdn_nodes_status (status),
    INDEX idx_cdn_nodes_region (region_id),
    INDEX idx_cdn_nodes_enabled (is_enabled),
    INDEX idx_cdn_nodes_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN 节点表';

-- ----------------------------------------
-- cdn_node_load_history — CDN 节点负载历史表
-- ----------------------------------------
CREATE TABLE cdn_node_load_history (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    cdn_node_id          BIGINT NOT NULL,
    recorded_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_load         INT DEFAULT 0,
    cpu_usage            DECIMAL(5,2),
    memory_usage         DECIMAL(5,2),
    bandwidth_usage      DECIMAL(5,2),
    storage_usage        DECIMAL(5,2),
    up_bandwidth         INT,
    down_bandwidth       INT,
    active_connections   INT DEFAULT 0,
    request_rate         DECIMAL(10,2),
    bandwidth_throughput DECIMAL(15,2),
    cache_hit_rate       DECIMAL(5,2),
    avg_response_time    DECIMAL(10,2),
    error_rate           DECIMAL(5,2),
    status               VARCHAR(20),
    load_percentage      DECIMAL(5,2),
    extra_data           TEXT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_load_node (cdn_node_id),
    INDEX idx_load_recorded (recorded_at),
    INDEX idx_load_status (status),
    FOREIGN KEY (cdn_node_id) REFERENCES cdn_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN 节点负载历史表';

-- ----------------------------------------
-- cdn_node_notification_logs — CDN 节点通知日志表
-- ----------------------------------------
CREATE TABLE cdn_node_notification_logs (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    cdn_node_id        BIGINT NOT NULL,
    notification_type  VARCHAR(20) NOT NULL,
    notification_level VARCHAR(20) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    message            VARCHAR(1000),
    sent_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_node (cdn_node_id),
    INDEX idx_notification_sent (sent_at),
    FOREIGN KEY (cdn_node_id) REFERENCES cdn_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDN 节点通知日志表';

-- ----------------------------------------
-- camera_config_templates — 摄像头配置模板表
-- ----------------------------------------
CREATE TABLE camera_config_templates (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand              VARCHAR(100) NOT NULL COMMENT '摄像头品牌',
    model              VARCHAR(100) NOT NULL COMMENT '摄像头型号',
    protocol           VARCHAR(20) NOT NULL COMMENT 'RTSP / ONVIF / GB28181 / HTTP / RTMP',
    default_port       INT NOT NULL COMMENT '协议默认端口',
    url_path_template  VARCHAR(500) NOT NULL COMMENT 'URL路径模板',
    preset_parameters  JSON COMMENT '预设参数',
    is_preset          TINYINT(1) DEFAULT 0 COMMENT '是否为预设模板',
    usage_count        INT DEFAULT 0 COMMENT '使用次数',
    last_used_at       TIMESTAMP NULL COMMENT '最后使用时间',
    is_deleted         TINYINT(1) DEFAULT 0,
    deleted_at         TIMESTAMP NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_brand_model (brand, model, is_deleted),
    INDEX idx_brand (brand),
    INDEX idx_protocol (protocol),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头配置模板表';

-- ----------------------------------------
-- camera_discovery_tasks — 摄像头发现扫描任务表
-- ----------------------------------------
CREATE TABLE camera_discovery_tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT COMMENT '创建扫描任务的用户',
    network_segment VARCHAR(50) NOT NULL COMMENT '网段(CIDR格式)',
    status          VARCHAR(20) NOT NULL COMMENT 'PENDING / RUNNING / COMPLETED / FAILED / CANCELLED',
    progress        INT DEFAULT 0 COMMENT '扫描进度(0-100)',
    total_ips       INT DEFAULT 0 COMMENT '待扫描IP总数',
    found_devices   JSON COMMENT '发现的设备(JSON数组)',
    started_at      TIMESTAMP NULL,
    completed_at    TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头发现扫描任务表';

-- ----------------------------------------
-- camera_batch_import_tasks — 摄像头批量导入任务表
-- ----------------------------------------
CREATE TABLE camera_batch_import_tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT COMMENT '发起导入的用户',
    file_name       VARCHAR(255) NOT NULL COMMENT '上传文件名',
    status          VARCHAR(20) NOT NULL COMMENT 'PENDING / VALIDATING / IMPORTING / COMPLETED / FAILED / CANCELLED',
    total_records   INT DEFAULT 0 COMMENT '待导入记录数',
    success_count   INT DEFAULT 0 COMMENT '成功导入数',
    fail_count      INT DEFAULT 0 COMMENT '失败数',
    error_details   JSON COMMENT '错误详情',
    started_at      TIMESTAMP NULL,
    completed_at    TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摄像头批量导入任务表';

-- ----------------------------------------
-- video_wall_preferences — 视频墙用户偏好表
-- ----------------------------------------
CREATE TABLE video_wall_preferences (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    layout         VARCHAR(10) NOT NULL DEFAULT '4',
    quality        VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate        INT DEFAULT 2048,
    camera_ids     JSON COMMENT '选中的摄像头ID数组',
    auto_apply     TINYINT(1) DEFAULT 1,
    last_preset_id BIGINT,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_video_wall_prefs_user (user_id),
    INDEX idx_video_wall_preferences_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频墙用户偏好表';

-- ----------------------------------------
-- video_wall_presets — 视频墙预设表
-- ----------------------------------------
CREATE TABLE video_wall_presets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    preset_name VARCHAR(50) NOT NULL,
    layout      VARCHAR(10) NOT NULL DEFAULT '4',
    quality     VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate     INT DEFAULT 2048,
    camera_ids  JSON COMMENT '摄像头ID数组',
    is_default  TINYINT(1) DEFAULT 0,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_preset_name (user_id, preset_name),
    INDEX idx_video_wall_presets_user_id (user_id),
    INDEX idx_video_wall_presets_sort (user_id, sort_order),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频墙预设表';


-- ============================================================
-- Module 3: AI Analysis (AI 分析)
-- Tables in: aick-mmp-central (AI feature)
-- ============================================================

-- ----------------------------------------
-- ai_passenger_stats — 客流统计表
-- ----------------------------------------
CREATE TABLE ai_passenger_stats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id       BIGINT NOT NULL,
    edge_node_id    BIGINT,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    enter_count     INT DEFAULT 0,
    exit_count      INT DEFAULT 0,
    inside_count    INT DEFAULT 0,
    max_inside_count INT DEFAULT 0,
    INDEX idx_camera_time (camera_id, start_time),
    INDEX idx_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客流统计表';

-- ----------------------------------------
-- ai_behavior_events — 行为事件表
-- ----------------------------------------
CREATE TABLE ai_behavior_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id       BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    level           VARCHAR(20),
    position_data   JSON,
    snapshot_url    VARCHAR(500),
    description     TEXT,
    event_time      DATETIME NOT NULL,
    status          VARCHAR(20) DEFAULT 'UNRESOLVED',
    alert_record_id BIGINT,
    INDEX idx_camera_event (camera_id, event_time),
    INDEX idx_event_type (event_type, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行为事件表';

-- ----------------------------------------
-- ai_vehicle_records — 车辆记录表
-- ----------------------------------------
CREATE TABLE ai_vehicle_records (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id      BIGINT NOT NULL,
    plate_number   VARCHAR(50) NOT NULL,
    plate_color    VARCHAR(20),
    confidence     DECIMAL(5,4),
    snapshot_url   VARCHAR(500),
    is_whitelisted TINYINT(1) DEFAULT 0,
    is_blacklisted TINYINT(1) DEFAULT 0,
    detect_time    DATETIME NOT NULL,
    INDEX idx_plate (plate_number),
    INDEX idx_time (detect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆记录表';

-- ----------------------------------------
-- ai_vehicle_whitelist — 车辆白名单表
-- ----------------------------------------
CREATE TABLE ai_vehicle_whitelist (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL,
    plate_color  VARCHAR(20),
    owner_name   VARCHAR(100),
    description  TEXT,
    enabled      TINYINT(1) DEFAULT 1,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_whitelist_plate (plate_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆白名单表';

-- ----------------------------------------
-- ai_analysis_config — AI 分析配置表
-- ----------------------------------------
CREATE TABLE ai_analysis_config (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id                  BIGINT NOT NULL,
    enable_passenger           TINYINT(1) DEFAULT 1,
    enable_behavior            TINYINT(1) DEFAULT 1,
    enable_plate               TINYINT(1) DEFAULT 1,
    passenger_frame_rate       INT DEFAULT 1,
    behavior_frame_rate        INT DEFAULT 2,
    plate_frame_rate           INT DEFAULT 5,
    loitering_threshold_seconds INT DEFAULT 30,
    gathering_min_people       INT DEFAULT 5,
    enabled                    TINYINT(1) DEFAULT 1,
    updated_at                 DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_config_camera (camera_id),
    INDEX idx_camera (camera_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 分析配置表';

-- ----------------------------------------
-- ai_vehicle_blacklist — 车辆黑名单表
-- ----------------------------------------
CREATE TABLE ai_vehicle_blacklist (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL,
    plate_color  VARCHAR(20),
    reason       TEXT,
    enabled      TINYINT(1) DEFAULT 1,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blacklist_plate (plate_number),
    INDEX idx_blacklist_plate (plate_number),
    INDEX idx_blacklist_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆黑名单表';
