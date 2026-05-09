# Camera Soft Delete Specification

## ADDED Requirements

### Requirement: 软删除字段记录

摄像头实体 SHALL 包含 `deletedAt` 字段（TIMESTAMP, nullable）用于记录软删除时间。

#### Scenario: 正常摄像头无删除时间
- **WHEN** 查询一个未被删除的摄像头
- **THEN** 该摄像头的 `deletedAt` 字段 SHALL 为 NULL

#### Scenario: 已删除摄像头记录删除时间
- **WHEN** 软删除一个摄像头
- **THEN** 该摄像头的 `deletedAt` 字段 SHALL 被设置为当前时间戳

---

### Requirement: 查询自动过滤已删除记录

所有摄像头列表查询 SHALL 自动过滤已删除的摄像头（`deletedAt IS NOT NULL`）。

#### Scenario: 列表查询排除已删除记录
- **WHEN** 调用 `GET /api/cameras`
- **THEN** 返回结果 SHALL NOT 包含任何 `deletedAt IS NOT NULL` 的摄像头

#### Scenario: 详情查询排除已删除记录
- **WHEN** 调用 `GET /api/cameras/{id}`
- **AND** 该摄像头已被软删除
- **THEN** 系统 SHALL 返回 404 Not Found

#### Scenario: 分页查询排除已删除记录
- **WHEN** 调用 `GET /api/cameras?page=0&size=10`
- **THEN** 分页计数 SHALL 仅统计未删除的摄像头
- **AND** 返回结果 SHALL NOT 包含已删除的摄像头

---

### Requirement: 软删除操作

删除摄像头时 SHALL 执行软删除，设置 `deletedAt` 为当前时间戳，而非物理删除。

#### Scenario: 软删除成功
- **WHEN** 调用 `DELETE /api/cameras/{id}`
- **AND** 该摄像头存在且未被删除
- **THEN** 系统 SHALL 设置该摄像头的 `deletedAt` 为当前时间戳
- **AND** 返回 204 No Content

#### Scenario: 删除已删除的摄像头
- **WHEN** 调用 `DELETE /api/cameras/{id}`
- **AND** 该摄像头已被软删除
- **THEN** 系统 SHALL 返回 404 Not Found

---

### Requirement: 批量软删除

系统 SHALL 支持批量软删除摄像头。

#### Scenario: 批量软删除成功
- **WHEN** 调用 `POST /api/cameras/batch-delete` 并传入摄像头ID列表
- **THEN** 系统 SHALL 为所有存在的、未被删除的摄像头设置 `deletedAt` 时间戳
- **AND** 返回每个摄像头的删除结果

#### Scenario: 批量删除包含已删除记录
- **WHEN** 批量删除请求中包含已被软删除的摄像头ID
- **THEN** 系统 SHALL 跳过已删除的记录
- **AND** 继续处理其他有效ID
- **AND** 返回结果中标记跳过项

---

### Requirement: 管理员可查看已删除记录

管理员 SHALL 能够查看已被软删除的摄像头记录（用于审计和恢复）。

#### Scenario: 查看所有记录包括已删除
- **WHEN** 管理员调用 `GET /api/cameras?includeDeleted=true`
- **THEN** 返回结果 SHALL 包含已删除和未删除的摄像头

#### Scenario: 查看单个已删除摄像头详情
- **WHEN** 管理员调用 `GET /api/cameras/{id}?includeDeleted=true`
- **AND** 该摄像头已被软删除
- **THEN** 系统 SHALL 返回该摄像头的完整信息
- **AND** 包含 `deletedAt` 时间戳

---

### Requirement: 恢复已删除摄像头

管理员 SHALL 能够恢复已被软删除的摄像头。

#### Scenario: 恢复成功
- **WHEN** 管理员调用 `POST /api/cameras/{id}/restore`
- **AND** 该摄像头存在且已被软删除
- **THEN** 系统 SHALL 将该摄像头的 `deletedAt` 设置为 NULL
- **AND** 返回恢复后的摄像头信息

#### Scenario: 恢复未删除的摄像头
- **WHEN** 管理员调用 `POST /api/cameras/{id}/restore`
- **AND** 该摄像头未被删除
- **THEN** 系统 SHALL 返回 400 Bad Request
- **AND** 提示该摄像头未被删除
