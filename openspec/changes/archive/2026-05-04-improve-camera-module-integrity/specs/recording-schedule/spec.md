# Recording Schedule Specification

## ADDED Requirements

### Requirement: 录像计划创建

管理员 SHALL 能够为摄像头创建录像计划，定义何时录制视频。录像计划包含录像模式、时间段配置和启用状态。

#### Scenario: 创建定时录像计划
- **WHEN** 管理员调用 `POST /api/recordings/schedules` 并提供有效的录像计划配置
- **AND** 配置包含 cameraId、mode=SCHEDULED、至少一个时间段
- **THEN** 系统 SHALL 在数据库中创建录像计划记录
- **AND** 返回包含计划ID的响应

#### Scenario: 创建连续录像计划
- **WHEN** 管理员调用 `POST /api/recordings/schedules` 并设置 mode=CONTINUOUS
- **THEN** 系统 SHALL 创建24小时全时段的录像计划
- **AND** 时间槽列表 SHALL 包含一个覆盖全天的时间段

#### Scenario: 创建失败 - 摄像头不存在
- **WHEN** 管理员尝试为不存在的 cameraId 创建录像计划
- **THEN** 系统 SHALL 返回 404 错误
- **AND** 不创建任何录像计划记录

---

### Requirement: 录像计划查询

系统 SHALL 提供查询录像计划的接口，支持按摄像头ID获取所有关联的录像计划。

#### Scenario: 查询摄像头关联的录像计划
- **WHEN** 调用 `GET /api/recordings/schedules?cameraId={id}`
- **THEN** 系统 SHALL 返回该摄像头所有未删除的录像计划
- **AND** 每条记录包含 id、cameraId、mode、timeSlots、enabled、createdAt

#### Scenario: 查询单个录像计划详情
- **WHEN** 调用 `GET /api/recordings/schedules/{scheduleId}`
- **THEN** 系统 SHALL 返回该录像计划的完整详情
- **AND** 包含所有时间段配置

---

### Requirement: 录像计划修改

管理员 SHALL 能够修改现有录像计划的配置，包括录像模式、时间段和启用状态。

#### Scenario: 更新录像计划时间段
- **WHEN** 管理员调用 `PUT /api/recordings/schedules/{scheduleId}`
- **AND** 提供新的 timeSlots 配置
- **THEN** 系统 SHALL 更新数据库中的时间段配置
- **AND** 边缘节点在下一次同步时获取最新配置

#### Scenario: 启用/禁用录像计划
- **WHEN** 管理员调用 `PATCH /api/recordings/schedules/{scheduleId}/toggle`
- **AND** 提供 enabled=true/false
- **THEN** 系统 SHALL 更新录像计划的启用状态
- **AND** 边缘节点立即响应配置变更

---

### Requirement: 录像计划删除

录像计划 SHALL 支持软删除，确保配置历史可追溯且可恢复。

#### Scenario: 软删除录像计划
- **WHEN** 管理员调用 `DELETE /api/recordings/schedules/{scheduleId}`
- **THEN** 系统 SHALL 设置该计划的 deletedAt 为当前时间
- **AND** 边缘节点在下一次同步时移除该计划

#### Scenario: 恢复已删除的录像计划
- **WHEN** 管理员调用 `POST /api/recordings/schedules/{scheduleId}/restore`
- **THEN** 系统 SHALL 清除 deletedAt 字段
- **AND** 录像计划恢复正常可用状态

---

### Requirement: 录像计划同步

边缘节点 SHALL 定期从中央服务同步录像计划配置，确保本地执行与中心配置一致。

#### Scenario: 边缘节点同步录像计划
- **WHEN** 边缘节点调用 `GET /api/edge/recordings/schedules?nodeId={id}`
- **THEN** 中央服务 SHALL 返回分配给该节点的摄像头的所有有效录像计划
- **AND** 响应包含完整的配置详情

#### Scenario: 录像计划变更通知
- **WHEN** 录像计划配置发生变更
- **AND** 下一次边缘节点心跳时
- **THEN** 中央服务 SHALL 在心跳响应中标记配置变更
- **AND** 边缘节点主动拉取最新配置
