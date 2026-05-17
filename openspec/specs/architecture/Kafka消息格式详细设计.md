# AI智能分析系统 - Kafka消息格式详细设计

> 版本: 2.0
> 更新时间: 2026-05-12
> 状态: **已实现**

---

## 1. 概述

### 1.1 设计原则

AI 分析结果采用**单向消息流**设计：AI 服务（Python） → Kafka → 中央服务（Java）。

- AI 服务不消费任何 Kafka 消息，仅作为生产者
- 中央服务不生产 AI 相关 Kafka 消息，仅作为消费者
- 帧数据传输使用 gRPC 流（非 Kafka），避免大消息对 Kafka 集群的压力

### 1.2 数据流总览

```
边缘节点 (Edge Node)
    │  RTSP → FFmpegFrameGrabber → JPEG
    │
    │  gRPC 流 (HandleFrame)
    ▼
AI 服务 (Python FastAPI)
    │
    │  YOLOv8 + ByteTrack / BehaviorEngine / LPRNet
    │
    ├──► Kafka: ai-passenger-stats  ──► 中央服务 Consumer → MySQL + Redis
    ├──► Kafka: ai-behavior-events  ──► 中央服务 Consumer → MySQL + Alert 系统
    ├──► Kafka: ai-vehicle-records  ──► 中央服务 Consumer → MySQL + 白名单检查
    └──► Kafka: ai-tasks-dlq        ──► 死信队列
```

---

## 2. Topic 架构

### 2.1 Topic 列表

| Topic 名称 | 分区数 | 副本数 | 保留时间 | 消息格式 | 生产者 | 消费者 |
|-----------|--------|--------|----------|----------|--------|--------|
| `ai-passenger-stats` | 3 | 1 | 7天 | JSON | AI Service | Central Service |
| `ai-behavior-events` | 3 | 1 | 30天 | JSON | AI Service | Central Service |
| `ai-vehicle-records` | 3 | 1 | 7天 | JSON | AI Service | Central Service |
| `ai-tasks-dlq` | 1 | 1 | 7天 | JSON | AI Service | (手动消费) |

### 2.2 Topic 命名规范

```
ai-{分析类型}-{数据类型}

示例:
- ai-passenger-stats      # 客流统计数据
- ai-behavior-events      # 行为告警事件
- ai-vehicle-records      # 车牌识别记录
- ai-tasks-dlq            # 死信队列
```

---

## 3. 消息格式

### 3.1 Topic: ai-passenger-stats

客流统计结果，由 AI 服务周期性（每帧或每 N 秒）发送。

```json
{
  "camera_id": "camera_001",
  "edge_node_id": "edge_node_001",
  "start_time": "2026-05-10T22:00:00Z",
  "end_time": "2026-05-10T22:05:00Z",
  "enter_count": 15,
  "exit_count": 12,
  "inside_count": 23,
  "max_inside_count": 28
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `camera_id` | string | 是 | 摄像头标识 |
| `edge_node_id` | string | 是 | 边缘节点标识 |
| `start_time` | datetime | 是 | 统计开始时间（ISO8601） |
| `end_time` | datetime | 是 | 统计结束时间（ISO8601） |
| `enter_count` | int | 是 | 进入人数 |
| `exit_count` | int | 是 | 离开人数 |
| `inside_count` | int | 是 | 当前在区域内人数 |
| `max_inside_count` | int | 是 | 统计周期内最大同时人数 |

**消费者处理（AiPassengerStatsConsumer）**:
1. 将记录写入 `ai_passenger_stats` 表
2. 更新 Redis 实时缓存 `ai:passenger:realtime:{cameraId}`（5 分钟 TTL）

### 3.2 Topic: ai-behavior-events

行为告警事件，AI 服务检测到异常行为时发送。

```json
{
  "camera_id": "camera_001",
  "event_type": "LOITERING",
  "level": "WARNING",
  "position_data": {
    "x": 320,
    "y": 240,
    "width": 100,
    "height": 200
  },
  "snapshot_url": "",
  "description": "检测到徘徊行为",
  "event_time": "2026-05-10T22:05:15Z"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `camera_id` | string | 是 | 摄像头标识 |
| `event_type` | string | 是 | 事件类型：LOITERING / INTRUSION / GATHERING / FALL |
| `level` | string | 是 | 告警级别：INFO / WARNING / CRITICAL |
| `position_data` | object | 否 | 事件位置信息（JSON，包含 x/y/width/height） |
| `snapshot_url` | string | 否 | 事件快照 URL |
| `description` | string | 是 | 事件描述 |
| `event_time` | datetime | 是 | 事件发生时间（ISO8601） |

**消费者处理（AiBehaviorEventConsumer）**:
1. 将记录写入 `ai_behavior_events` 表
2. 调用 `AlertNotificationService.sendAlertNotification()` 创建告警记录并推送通知
   - ruleId = 0（系统内置 AI 行为规则）
   - ruleName = "AI Behavior: {event_type}"
   - targetType = CAMERA
   - targetId = camera_id

**事件类型说明**:

| event_type | 说明 | 检测方法 |
|-----------|------|----------|
| `LOITERING` | 徘徊检测 | 单个目标在区域内停留超过阈值时间（默认 30s） |
| `INTRUSION` | 区域入侵 | 目标进入禁区 ROI |
| `GATHERING` | 人群聚集 | 一组目标间距小于阈值（默认 2m）且数量超过阈值（默认 3人） |
| `FALL` | 跌倒检测 | 目标框宽高比 > 阈值（默认 1.2），判定为倒地姿势 |

### 3.3 Topic: ai-vehicle-records

车牌识别记录，检测到车牌时发送。

```json
{
  "camera_id": "camera_001",
  "plate_number": "京A12345",
  "plate_color": "蓝色",
  "confidence": 0.96,
  "is_whitelisted": false,
  "is_blacklisted": false,
  "detect_time": "2026-05-10T22:05:15Z"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `camera_id` | string | 是 | 摄像头标识 |
| `plate_number` | string | 是 | 识别到的车牌号码 |
| `plate_color` | string | 否 | 车牌颜色 |
| `confidence` | number | 是 | 识别置信度（0-1） |
| `is_whitelisted` | boolean | 是 | 是否在白名单中 |
| `is_blacklisted` | boolean | 是 | 是否在黑名单中 |
| `detect_time` | datetime | 是 | 检测时间（ISO8601） |

**消费者处理（AiVehicleRecordConsumer）**:
1. 查询 `ai_vehicle_whitelist` 表匹配车牌号码
2. 同步设置 `is_whitelisted` / `is_blacklisted` 状态
3. 将完整记录写入 `ai_vehicle_records` 表

---

## 4. Topic: ai-tasks-dlq（死信队列）

消息处理失败后发送到 DLQ 进行人工排查。

```json
{
  "original_topic": "ai-behavior-events",
  "original_message": { /* 原始消息内容 */ },
  "error_info": {
    "error_message": "数据库连接超时",
    "timestamp": "2026-05-10T22:05:16Z",
    "retry_count": 3
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `original_topic` | string | 是 | 原始消息所属 Topic |
| `original_message` | object | 是 | 原始消息内容 |
| `error_info` | object | 是 | 错误信息 |
| `error_info.error_message` | string | 是 | 错误描述 |
| `error_info.timestamp` | datetime | 是 | 错误时间 |
| `error_info.retry_count` | int | 是 | 已重试次数 |

---

## 5. 消费者配置（Central Service）

### 5.1 Kafka 消费者配置

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: kafka:9092
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "*"
```

### 5.2 消费者组

| 消费者类 | 订阅 Topic | 消费者组 |
|----------|-----------|----------|
| `AiPassengerStatsConsumer` | ai-passenger-stats | ai-passenger-consumer-group |
| `AiBehaviorEventConsumer` | ai-behavior-events | ai-behavior-consumer-group |
| `AiVehicleRecordConsumer` | ai-vehicle-records | ai-vehicle-consumer-group |

---

## 6. 幂等性与错误处理

### 6.1 幂等性保证

- **数据库唯一约束**：AI 分析结果通过业务主键（时间 + 摄像头）去重
- **AiBehaviorEventConsumer**：写入失败时将消息发送到 DLQ，不阻塞消费

### 6.2 重试策略

| 错误类型 | 处理策略 |
|----------|----------|
| 数据库临时错误（连接超时） | 自动重试 3 次，指数退避 |
| 数据格式错误 | 记录错误日志，跳过消息 |
| 业务处理异常 | 发送到 DLQ，人工介入 |
