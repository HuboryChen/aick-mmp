## REMOVED Requirements

### Requirement: 移动侦测参数

**Reason**: 移动侦测参数配置已升级为AI分析配置，包含更多分析类型和参数

---

## ADDED Requirements

### Requirement: AI分析结果查询

系统 SHALL 提供统一的AI分析结果查询接口（REST API，前端5秒轮询）。

#### Scenario: 查询实时客流
- **WHEN** 调用 `GET /api/v1/ai/stats/passenger/realtime/{cameraId}`
- **THEN** 系统 SHALL 返回当前实时人数
- **AND** 数据来自Redis缓存

#### Scenario: 查询历史客流统计
- **WHEN** 调用 `GET /api/v1/ai/stats/passenger?cameraId={}&startTime={}&endTime={}`
- **THEN** 系统 SHALL 返回客流统计列表
- **AND** 支持按时间范围筛选

#### Scenario: 查询行为告警
- **WHEN** 调用 `GET /api/v1/ai/alerts/behavior?cameraId={}&eventType={}&status={}`
- **THEN** 系统 SHALL 返回行为告警列表
- **AND** 支持按类型、状态筛选

#### Scenario: 处理告警
- **WHEN** 调用 `PUT /api/v1/ai/alerts/behavior/{id}/status?status={}`
- **THEN** 系统 SHALL 更新告警状态（UNRESOLVED/ACKNOWLEDGED/RESOLVED）

#### Scenario: 查询车牌记录
- **WHEN** 调用 `GET /api/v1/ai/vehicles/records?plateNumber={}&cameraId={}`
- **THEN** 系统 SHALL 返回车牌识别记录
- **AND** 支持车牌号精确查询

---

### Requirement: AI配置与录像关联（待实现）

> **状态：待实现** — 以下功能为后续迭代规划

AI分析配置 SHALL 与录像配置关联，支持基于分析结果的录像触发。

#### Scenario: 告警触发录像
- **WHEN** 收到AI告警且严重级别为 WARNING 或 CRITICAL 时
- **AND** 摄像头配置了 alertRecordingEnabled=true
- **THEN** 系统 SHALL 自动触发录像
- **AND** 录像记录关联到对应告警
