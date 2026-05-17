## ADDED Requirements

### Requirement: AI分析结果存储

系统 SHALL 存储AI分析结果到MySQL数据库，支持历史查询和统计分析。

#### Scenario: 客流统计数据存储
- **WHEN** 收到客流统计结果时
- **THEN** 系统 SHALL 创建 `ai_passenger_stats` 记录
- **AND** 包含cameraId、startTime、endTime、enterCount、exitCount、insideCount

#### Scenario: 行为事件存储
- **WHEN** 检测到行为事件时
- **THEN** 系统 SHALL 创建 `ai_behavior_events` 记录
- **AND** 包含cameraId、eventType、level、description、eventTime、status

#### Scenario: 车牌识别记录存储
- **WHEN** 识别到车牌时
- **THEN** 系统 SHALL 创建 `ai_vehicle_records` 记录
- **AND** 包含plateNumber、cameraId、detectTime、plateColor、confidence、isWhitelisted

#### Scenario: 快照文件存储
- **WHEN** 需要存储分析快照时
- **THEN** 系统 SHALL 预留 snapshotUrl 字段
- **AND** 对象存储（MinIO/OSS）列为后续优化项

---

### Requirement: 数据库表设计

系统 SHALL 使用MySQL 8.0存储结构化数据，遵循现有数据库规范。`spring.jpa.hibernate.ddl-auto=update` 自动建表，迁移脚本见 `docs/sql/V20260510__create_ai_tables.sql`。

#### Scenario: 客流统计表
- **WHEN** 设计数据库表时
- **THEN** 系统 SHALL 创建 `ai_passenger_stats` 表：
  ```sql
  CREATE TABLE ai_passenger_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    enter_count INT DEFAULT 0,
    exit_count INT DEFAULT 0,
    inside_count INT DEFAULT 0,
    max_inside_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_camera_time (camera_id, start_time)
  );
  ```

#### Scenario: 行为事件表
- **WHEN** 设计数据库表时
- **THEN** 系统 SHALL 创建 `ai_behavior_events` 表：
  ```sql
  CREATE TABLE ai_behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    level VARCHAR(20) NOT NULL,
    position_data JSON,
    snapshot_url VARCHAR(500),
    description VARCHAR(500),
    event_time DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'UNRESOLVED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_camera_time (camera_id, event_time),
    INDEX idx_type_status (event_type, status)
  );
  ```

#### Scenario: 车牌记录表
- **WHEN** 设计数据库表时
- **THEN** 系统 SHALL 创建 `ai_vehicle_records` 表：
  ```sql
  CREATE TABLE ai_vehicle_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    plate_color VARCHAR(20),
    confidence DECIMAL(5,4),
    snapshot_url VARCHAR(500),
    is_whitelisted BOOLEAN DEFAULT FALSE,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    detect_time DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plate (plate_number),
    INDEX idx_camera_time (camera_id, detect_time)
  );
  ```

#### Scenario: 车牌白名单表
- **WHEN** 设计数据库表时
- **THEN** 系统 SHALL 创建 `ai_vehicle_whitelist` 表：
  ```sql
  CREATE TABLE ai_vehicle_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    plate_color VARCHAR(20),
    owner_name VARCHAR(100),
    description VARCHAR(200),
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  ```

---

### Requirement: 缓存策略

系统 SHALL 使用Redis缓存热点数据，减少数据库压力。

#### Scenario: 实时人数缓存
- **WHEN** 更新实时人数时
- **THEN** 系统 SHALL 缓存到Redis
- **AND** Key格式：`ai:passenger:realtime:{cameraId}`
- **AND** Value：当前人数字符串
- **AND** TTL：5分钟
