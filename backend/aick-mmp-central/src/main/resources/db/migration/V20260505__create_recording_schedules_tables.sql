-- V20260505__create_recording_schedules_tables.sql
-- Create recording schedule tables for camera video recording management
-- Part of recording-schedule-management change

-- ============================================================================
-- Table: recording_schedules (录像计划主表)
-- ============================================================================
CREATE TABLE recording_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '计划名称',
    camera_id BIGINT NOT NULL COMMENT '摄像头ID',
    schedule_type VARCHAR(20) NOT NULL COMMENT '录像类型: CONTINUOUS/TIMED/MOTION/EVENT/SMART',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    motion_sensitivity INT DEFAULT 50 COMMENT '移动侦测灵敏度 (0-100)',
    retention_days INT DEFAULT 30 COMMENT '录像保留天数',
    description VARCHAR(500) COMMENT '备注说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像计划主表';

-- Indexes for recording_schedules
CREATE INDEX idx_recording_schedule_camera ON recording_schedules(camera_id);
CREATE INDEX idx_recording_schedule_enabled ON recording_schedules(enabled);
CREATE INDEX idx_recording_schedule_type ON recording_schedules(schedule_type);

-- ============================================================================
-- Table: recording_schedule_time_slots (录像时间段表)
-- ============================================================================
CREATE TABLE recording_schedule_time_slots (
    schedule_id BIGINT NOT NULL COMMENT '录像计划ID',
    slot_order INT NOT NULL COMMENT '时间段顺序',
    start_time TIME COMMENT '开始时间',
    end_time TIME COMMENT '结束时间',
    quality VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '录像质量: HIGH/MEDIUM/LOW',
    PRIMARY KEY (schedule_id, slot_order),
    CONSTRAINT fk_time_slots_schedule FOREIGN KEY (schedule_id) 
        REFERENCES recording_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像时间段表';

-- ============================================================================
-- Table: recording_schedule_days (录像日期表)
-- ============================================================================
CREATE TABLE recording_schedule_days (
    schedule_id BIGINT NOT NULL COMMENT '录像计划ID',
    day_of_week VARCHAR(10) NOT NULL COMMENT '星期几: MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY',
    PRIMARY KEY (schedule_id, day_of_week),
    CONSTRAINT fk_days_schedule FOREIGN KEY (schedule_id) 
        REFERENCES recording_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录像日期表';

-- Add comments for enum values (MySQL 8.0+)
ALTER TABLE recording_schedules 
    MODIFY schedule_type ENUM('CONTINUOUS', 'TIMED', 'MOTION', 'EVENT', 'SMART') NOT NULL 
    COMMENT '录像类型: CONTINUOUS-持续录像, TIMED-定时录像, MOTION-移动侦测, EVENT-事件录像, SMART-智能录像';

ALTER TABLE recording_schedule_days 
    MODIFY day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL 
    COMMENT '星期几: MONDAY/THURSDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY';

ALTER TABLE recording_schedule_time_slots 
    MODIFY quality ENUM('HIGH', 'MEDIUM', 'LOW') DEFAULT 'MEDIUM' 
    COMMENT '录像质量: HIGH-高质量, MEDIUM-中等质量, LOW-低质量';
