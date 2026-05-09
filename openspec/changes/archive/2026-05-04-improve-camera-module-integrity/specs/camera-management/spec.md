# Camera Management Specification

## ADDED Requirements

### Requirement: Recording软删除支持

Recording实体 SHALL 支持软删除，通过 deletedAt 时间戳标记删除状态。

#### Scenario: 软删除录像
- **WHEN** 管理员调用 `DELETE /api/recordings/{recordingId}`
- **THEN** 系统 SHALL 设置该 Recording 的 deletedAt 为当前时间
- **AND** 查询接口自动过滤已删除记录

#### Scenario: 恢复已删除录像
- **WHEN** 管理员调用 `POST /api/recordings/{recordingId}/restore`
- **THEN** 系统 SHALL 清除 deletedAt 字段
- **AND** 录像恢复正常可见状态

#### Scenario: 查询时自动过滤软删除记录
- **WHEN** 调用 `GET /api/recordings?cameraId={id}`
- **THEN** 系统 SHALL 只返回 deletedAt IS NULL 的记录
- **AND** 除非显式传入 includeDeleted=true

---

### Requirement: 摄像头删除时级联处理录像

删除摄像头时，系统 SHALL 自动处理关联的录像记录，防止孤立数据产生。

#### Scenario: 删除摄像头时标记孤立录像
- **WHEN** 管理员调用 `DELETE /api/cameras/{cameraId}`
- **THEN** 系统 SHALL 将该摄像头关联的所有未删除录像标记为孤立状态
- **AND** 设置 orphanedAt 为当前时间
- **AND** 设置 orphanedBy=cameraId

#### Scenario: 孤立录像的展示
- **WHEN** 调用 `GET /api/recordings?cameraId={id}&includeOrphaned=true`
- **THEN** 系统 SHALL 返回孤立录像并标记 orphaned=true
- **AND** 提供清理或恢复入口

---

### Requirement: 摄像头统计聚合API

系统 SHALL 提供摄像头统计聚合接口，返回多维度的统计数据。

#### Scenario: 获取摄像头总体统计
- **WHEN** 管理员调用 `GET /api/cameras/statistics/summary`
- **THEN** 系统 SHALL 返回包含以下数据的统计结果：
  - total: 摄像头总数
  - byStatus: 按状态分布的数量
  - byEdgeNode: 按节点分布的数量
  - totalRecordings: 关联录像总数
  - storageUsedBytes: 录像存储总量

#### Scenario: 按区域获取统计
- **WHEN** 调用 `GET /api/cameras/statistics/summary?regionId={id}`
- **THEN** 系统 SHALL 只统计指定区域内的摄像头
- **AND** 返回该区域的统计数据

#### Scenario: 按节点获取统计
- **WHEN** 调用 `GET /api/cameras/statistics/summary?edgeNodeId={id}`
- **THEN** 系统 SHALL 只统计分配到该节点的摄像头
- **AND** 返回该节点的统计数据

---

## MODIFIED Requirements

### Requirement: 摄像头搜索API权限

摄像头搜索接口 SHALL 允许 ADMIN、OPERATOR 和 VIEWER 角色访问。

#### Scenario: VIEWER角色使用搜索
- **WHEN** VIEWER 角色用户调用 `GET /api/cameras/search?keyword={kw}`
- **THEN** 系统 SHALL 返回搜索结果
- **AND** VIEWER 可以查看摄像头基本信息

#### Scenario: 未认证用户无法搜索
- **WHEN** 未认证用户调用 `GET /api/cameras/search`
- **THEN** 系统 SHALL 返回 401 未授权错误
