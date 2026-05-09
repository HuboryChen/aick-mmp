-- =====================================================
-- Camera Failover Events Table
-- 边缘节点故障转移事件记录表
-- =====================================================

CREATE TABLE IF NOT EXISTS camera_failover_events (
    id BIGSERIAL PRIMARY KEY,
    source_edge_node_id BIGINT NOT NULL,
    total_count INT NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('AUTO', 'MANUAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'PARTIAL', 'FAILED')),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- 目标节点关联表（一对多）
CREATE TABLE IF NOT EXISTS failover_event_target_nodes (
    event_id BIGINT NOT NULL REFERENCES camera_failover_events(id) ON DELETE CASCADE,
    target_edge_node_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, target_edge_node_id)
);

-- 摄像头关联表（一对多）
CREATE TABLE IF NOT EXISTS failover_event_cameras (
    event_id BIGINT NOT NULL REFERENCES camera_failover_events(id) ON DELETE CASCADE,
    camera_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, camera_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_failover_events_source_node ON camera_failover_events(source_edge_node_id);
CREATE INDEX IF NOT EXISTS idx_failover_events_trigger_type ON camera_failover_events(trigger_type);
CREATE INDEX IF NOT EXISTS idx_failover_events_status ON camera_failover_events(status);
CREATE INDEX IF NOT EXISTS idx_failover_events_created_at ON camera_failover_events(created_at DESC);

-- 注：CameraStatus 枚举新增 PENDING_ALLOCATION 值
-- 由于 JPA ddl-auto=update 会自动处理枚举值变更，无需手动迁移
