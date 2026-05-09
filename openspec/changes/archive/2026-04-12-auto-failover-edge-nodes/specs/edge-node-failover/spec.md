# Edge Node Failover Specification

## ADDED Requirements

### Requirement: 自动故障转移触发

当边缘节点被标记为 OFFLINE 状态时，系统 SHALL 自动触发该节点上所有在线摄像头的故障转移流程。此行为可通过配置启用或禁用。

#### Scenario: 节点离线时自动触发故障转移
- **WHEN** `EdgeNodeHealthService` 将某个边缘节点的状态从 ONLINE 变更为 OFFLINE
- **AND** 配置项 `edge.failover.enabled` 为 `true`
- **THEN** 系统 SHALL 自动调用故障转移服务，将该节点上所有 `status=ONLINE` 的摄像头列入迁移候选列表

#### Scenario: 故障转移功能被禁用时不触发
- **WHEN** 边缘节点变为 OFFLINE 状态
- **AND** 配置项 `edge.failover.enabled` 为 `false`
- **THEN** 系统 SHALL NOT 触发故障转移，摄像头保持在原节点上（与当前行为一致）

#### Scenario: 手动触发指定节点的故障转移
- **WHEN** 管理员调用 `POST /api/edge-nodes/{nodeId}/trigger-failover`
- **AND** 该节点存在且状态为 OFFLINE
- **THEN** 系统 SHALL 对该节点上的在线摄像头执行一次完整的故障转移流程
- **AND** 返回包含故障转移事件ID的响应

---

### Requirement: 摄像头迁移分配策略

故障转移过程中，系统 SHALL 基于增强的负载均衡算法为每个待迁移摄像头选择最优的目标边缘节点。算法在故障转移模式下 SHALL 优先考虑地域亲和性。

#### Scenario: 成功分配到同区域健康节点
- **WHEN** 执行故障转移分配
- **AND** 存在与源节点同区域且状态为 ONLINE 的健康边缘节点
- **AND** 目标节点 `currentCameraCount < maxCameraSupport`
- **THEN** 系统 SHALL 优先将摄像头分配到同区域节点（获得30%评分加成）

#### Scenario: 无同区域节点时跨区域分配
- **WHEN** 执行故障转移分配
- **AND** 不存在同区域的在线健康节点
- **AND** 存在其他区域的在线健康节点
- **THEN** 系统 SHALL 将摄像头分配到其他区域的最优节点（按原始四因子权重排序）

#### Scenario: 无可用节点时进入待分配池
- **WHEN** 执行故障转移分配
- **AND** 所有在线节点的剩余容量均为0（`currentCameraCount >= maxCameraSupport`）
- **OR** 不存在任何 ONLINE 状态的边缘节点
- **THEN** 该摄像头的 `edgeNodeId` SHALL 被设置为 `NULL`
- **AND** 摄像头状态 SHALL 变更为 `PENDING_ALLOCATION`
- **AND** 摄像头进入待分配池等待后续分配

#### Scenario: 避免重复迁移
- **WHEN** 对某摄像头执行故障转移
- **AND** 该摄像头的当前 `edgeNodeId` 已经不是源节点ID（已被其他流程迁移）
- **THEN** 系统 SHALL 跳过该摄像头，不执行重复迁移

---

### Requirement: 故障转移并发控制和批次处理

系统 SHALL 对故障转移过程实施并发控制和批次处理，防止短时间内大规模迁移对系统造成冲击。

#### Scenario: 并发故障转移数量受限
- **WHEN** 多个节点几乎同时变为 OFFLINE 状态
- **THEN** 同时执行的故障转移任务数 SHALL NOT 超过 `edge.failover.max-concurrent-tasks` 配置值（默认值为3）

#### Scenario: 批次处理摄像头迁移
- **WHEN** 单个故障转移任务需要迁移N个摄像头
- **THEN** 系统 SHALL 按 `edge.failover.batch-size`（默认20）分批处理
- **AND** 每批次之间 SHALL 等待 `edge.failover.batch-delay-ms`（默认1000ms）

#### Scenario: 单个批次内事务原子性
- **WHEN** 批次处理中的一组摄像头正在写入新的 edgeNodeId
- **AND** 其中某个摄像头更新失败（如乐观锁冲突）
- **THEN** 该批次中已成功的更新 SHALL 回滚（整个批次在同一事务中）
- **AND** 该批次的所有摄像头将在下一轮重试

---

### Requirement: 故障转移事件记录

每次故障转移操作（无论自动触发还是手动触发）SHALL 生成一条完整的故障转移事件记录，用于审计和问题排查。

#### Scenario: 记录完整的故障转移事件
- **WHEN** 故障转移流程开始执行
- **THEN** 系统 SHALL 在 `camera_failover_events` 表中创建一条记录，包含：
  - `source_edge_node_id`: 源节点ID
  - `trigger_type`: "AUTO" 或 "MANUAL"
  - `total_count`: 待迁移摄像头总数
  - `status`: "IN_PROGRESS"
  - `created_at`: 开始时间戳

#### Scenario: 故障转移完成后更新事件状态
- **WHEN** 所有批次处理完成
- **THEN** 事件记录 SHALL 被更新：
  - `status`: "COMPLETED"（全部成功）/ "PARTIAL"（部分失败）/ "FAILED"（全部失败）
  - `success_count`: 成功迁移的数量
  - `failed_count`: 失败并进入待分配池的数量
  - `target_edge_node_ids`: 涉及的所有目标节点ID列表
  - `camera_ids`: 所有涉及迁移的摄像头ID列表
  - `completed_at`: 完成时间戳

#### Scenario: 查询故障转移历史
- **WHEN** 调用 `GET /api/cameras/failover-events`
- **THEN** 系统 SHALL 返回按时间倒序排列的故障转移事件列表
- **AND** 支持按 `sourceEdgeNodeId`、`triggerType`、`status` 筛选

---

### Requirement: 待分配池管理

无法在故障转移时立即分配的摄像头 SHALL 进入待分配池，并在条件允许时自动重新分配。

#### Scenario: 查询待分配池中的摄像头
- **WHEN** 调用 `GET /api/cameras/pending-allocation`
- **THEN** 系统 SHALL 返回所有 `status=PENDING_ALLOCATION` 且 `edgeNodeId IS NULL` 的摄像头列表

#### Scenario: 节点上线时自动分配待分配摄像头
- **WHEN** 边缘节点状态从 OFFLINE 变更为 ONLINE（或有新节点注册）
- **AND** 待分配池中存在摄像头
- **THEN** 系统 SHALL 尝试将待分配池中的摄像头分配到该节点
- **AND** 受限于节点的 `maxCameraSupport` 容量限制

#### Scenario: 节点容量释放时自动分配
- **WHEN** 某边缘节点的摄像头被删除或迁移走，导致 `currentCameraCount` 降低
- **AND** 待分配池中存在摄像头
- **THEN** 系统 SHALL 在下一个定时周期（`failover.retry-interval-seconds`，默认300秒）尝试分配

#### Scenario: 手动分配待分配池摄像头
- **WHEN** 管理员调用 `POST /cameras/auto-assign`（已有API）
- **THEN** 系统 SHALL 包含待分配池中的摄像头作为分配候选（即 `edgeNodeId IS NULL OR status=PENDING_ALLOCATION`）

---

### Requirement: 故障转移配置项

系统 SHALL 提供以下配置项来控制故障转移行为：

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `edge.failover.enabled` | boolean | true | 是否启用自动故障转移 |
| `edge.failover.mode` | string | "async" | 触发模式: "sync" 或 "async" |
| `edge.failover.delay-seconds` | int | 0 | 触发后延迟执行的秒数（用于等待节点可能的短暂恢复） |
| `edge.failover.max-concurrent-tasks` | int | 3 | 最大并发故障转移任务数 |
| `edge.failover.batch-size` | int | 20 | 每批处理的摄像头数量 |
| `edge.failover.batch-delay-ms` | int | 1000 | 批次间的延迟毫秒数 |
| `edge.failover.region-bonus` | double | 0.3 | 同区域节点的评分加成比例 |
| `edge.failover.retry-interval-seconds` | int | 300 | 待分配池重试分配间隔 |

#### Scenario: 配置项生效
- **WHEN** 管理员修改上述任一配置项
- **THEN** 配置 SHALL 在下次故障转移触发时生效（支持运行时刷新，无需重启）

#### Scenario: 延迟等待期间节点恢复
- **WHEN** 节点被标记为 OFFLINE 并触发了故障转移
- **AND** `edge.failover.delay-seconds > 0`
- **AND** 在延迟期内节点恢复了心跳并变回 ONLINE
- **THEN** 正在等待的故障转移任务 SHALL 被取消
- **AND** 不产生任何故障转移事件
