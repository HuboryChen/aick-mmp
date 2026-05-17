## ADDED Requirements

### Requirement: Kafka结果发布（AI Service → Central）

AI微服务 SHALL 作为Kafka消息生产者，将分析结果发布到消息队列供Java系统消费。

#### Scenario: 发布客流统计结果
- **WHEN** 客流统计完成时
- **THEN** 系统 SHALL 发布结果到 `ai-passenger-stats` Topic
- **AND** 包含cameraId、enterCount、exitCount、insideCount、timestamp
- **AND** 设置分区键为cameraId

#### Scenario: 发布行为告警
- **WHEN** 检测到异常行为时
- **THEN** 系统 SHALL 发布告警到 `ai-behavior-events` Topic
- **AND** 包含eventType、level、description、position、timestamp

#### Scenario: 发布车牌识别记录
- **WHEN** 车牌识别完成时
- **THEN** 系统 SHALL 发布记录到 `ai-vehicle-records` Topic
- **AND** 包含plateNumber、plateColor、confidence、timestamp

---

### Requirement: Kafka消息消费（Central侧）

Java中央服务 SHALL 作为Kafka消息消费者，消费AI分析结果并持久化存储。

#### Scenario: 消费客流统计
- **WHEN** 从 `ai-passenger-stats` Topic 消费到消息时
- **THEN** 系统 SHALL 解析JSON消息
- **AND** 保存到 `ai_passenger_stats` 表
- **AND** 更新Redis实时人数缓存（TTL: 5分钟）

#### Scenario: 消费行为告警
- **WHEN** 从 `ai-behavior-events` Topic 消费到消息时
- **THEN** 系统 SHALL 保存到 `ai_behavior_events` 表
- **AND** 触发 AlertNotificationService 发送通知
- **AND** 创建关联的 AlertRecord

#### Scenario: 消费车牌记录
- **WHEN** 从 `ai-vehicle-records` Topic 消费到消息时
- **THEN** 系统 SHALL 查询白名单匹配
- **AND** 保存到 `ai_vehicle_records` 表
- **AND** 标记是否为白名单车辆

#### Scenario: 消费者组配置
- **WHEN** 配置Kafka消费者时
- **THEN** 系统 SHALL 使用消费者组 `mmp-ai-group`
- **AND** 同一消费者组的实例分担分区
- **AND** 支持水平扩展

---

### Requirement: Topic设计

系统 SHALL 定义清晰的Topic划分。

#### Scenario: Topic列表
- **WHEN** 配置Kafka Topic时
- **THEN** 系统 SHALL 使用以下Topic：
  - `ai-passenger-stats`：客流统计结果（6分区）
  - `ai-behavior-events`：行为告警事件（6分区）
  - `ai-vehicle-records`：车牌识别记录（6分区）
  - `ai-dlq`：死信队列（3分区）

#### Scenario: 消息保留策略
- **WHEN** 配置Topic时
- **THEN** 系统 SHALL 设置以下保留策略：
  - `ai-passenger-stats`：保留30天
  - `ai-behavior-events`：保留90天
  - `ai-vehicle-records`：保留90天
  - `ai-dlq`：保留7天

---

### Requirement: 错误处理

系统 SHALL 实现可靠的消息处理机制。

#### Scenario: 消息处理失败
- **WHEN** 消息处理失败时
- **THEN** 系统 SHALL 记录错误日志
- **AND** 不可重试错误发送到DLQ

#### Scenario: 消费者健康检查
- **WHEN** 消费者心跳超时
- **THEN** 系统 SHALL 触发再平衡
- **AND** 重新分配分区给健康实例
