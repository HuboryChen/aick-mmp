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

#### Scenario: 权重计算使用共享服务
- **WHEN** 需要计算节点权重
- **THEN** 系统 SHALL 调用 `NodeWeightCalculator.calculateWeight()` 方法
- **AND** SHALL NOT 在 `EdgeNodeFailoverServiceImpl` 中内嵌权重计算逻辑

#### Scenario: 添加摄像头时自动分配（新增）
- **WHEN** 管理员创建摄像头时未指定边缘节点
- **AND** 摄像头有地区信息
- **THEN** 系统 SHALL 自动为摄像头分配最优边缘节点
- **AND** 优先选择同地区且负载较低的在线节点

#### Scenario: 自动分配推荐最优节点（新增）
- **WHEN** 管理员在添加摄像头表单中选择地区
- **THEN** 系统 SHALL 显示推荐的边缘节点
- **AND** 推荐算法综合考虑：地区匹配度、节点状态、CPU负载、摄像头容量、带宽
- **AND** 管理员可以选择使用推荐节点或手动选择其他节点

#### Scenario: 批量导入时自动分配（新增）
- **WHEN** 执行批量导入摄像头操作
- **AND** 导入数据包含地区信息
- **THEN** 系统 SHALL 根据地区自动分配边缘节点
- **AND** 如果同地区无可用节点， SHALL 分配到其他地区节点或标记为待分配

---

### Requirement: 基于地区的智能分配算法

系统 SHALL 实现基于地区的智能分配算法，在分配摄像头时优先考虑地区匹配度。

#### Scenario: 地区匹配度评分计算
- **WHEN** 计算节点权重
- **AND** 摄像头有地区信息
- **THEN** 同地区节点 SHALL 获得额外的地区匹配加分（配置项 `camera.assignment.region-bonus`，默认 0.3）
- **AND** 跨地区节点不获得额外加分

#### Scenario: 多维度综合评分
- **WHEN** 计算节点权重
- **THEN** 系统 SHALL 综合以下因素计算得分：
  - 地区匹配度（如有加分）
  - 节点在线状态（ONLINE 得满分，OFFLINE 得 0 分）
  - CPU 负载（负载越低得分越高）
  - 摄像头容量使用率（使用率越低得分越高）
  - 带宽可用性（可用带宽越大得分越高）
- **AND** 每个因素的权重可配置

#### Scenario: 排除不可用节点
- **WHEN** 计算候选节点
- **THEN** 系统 SHALL 排除以下节点：
  - 状态为 OFFLINE 或 MAINTENANCE 的节点
  - 容量已满（`currentCameraCount >= maxCameraSupport`）的节点
  - CPU 负载超过阈值（配置项 `camera.assignment.max-cpu-load`，默认 90%）的节点

#### Scenario: 无可用节点时的处理
- **WHEN** 计算候选节点后可用节点列表为空
- **THEN** 系统 SHALL 将摄像头标记为待分配状态
- **AND** `edgeNodeId` 设置为 `NULL`
- **AND** `status` 设置为 `PENDING_ALLOCATION`

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

#### Scenario: 手动分配时优先考虑地区
- **WHEN** 管理员调用 `POST /cameras/auto-assign` 并指定地区参数
- **THEN** 系统 SHALL 优先将该地区的待分配摄像头分配到同地区节点
- **AND** 如果同地区无可用节点，再考虑跨地区分配

#### Scenario: 全局手动分配
- **WHEN** 管理员调用 `POST /cameras/auto-assign` 不指定地区
- **THEN** 系统 SHALL 将所有待分配摄像头按照智能分配算法分配
- **AND** 优先考虑地区匹配度

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

---

### Requirement: 分配配置项（扩展）

系统 SHALL 提供额外的配置项来控制基于地区的智能分配行为。

#### Scenario: 地区加分配置
- **WHEN** 管理员配置 `camera.assignment.region-bonus`
- **THEN** 配置 SHALL 控制同地区节点的评分加成比例
- **AND** 默认值为 0.3（30% 加成）
- **AND** 范围限制在 0-1 之间

#### Scenario: CPU 负载阈值配置
- **WHEN** 管理员配置 `camera.assignment.max-cpu-load`
- **THEN** 配置 SHALL 控制排除节点的 CPU 负载阈值
- **AND** 默认值为 90（%）
- **AND** 超过该阈值的节点不会被选为分配目标

#### Scenario: 容量余量配置
- **WHEN** 管理员配置 `camera.assignment.capacity-buffer`
- **THEN** 配置 SHALL 控制节点容量保留的余量比例
- **AND** 默认值为 0.1（10%）
- **AND** 可用容量 = maxCameraSupport * (1 - capacity-buffer) - currentCameraCount

---

## MODIFIED Requirements

### Requirement: 边缘节点心跳包含摄像头状态

边缘节点心跳请求 SHALL 包含分配给该节点的摄像头状态信息，实现双向状态同步。

#### Scenario: 心跳上报摄像头状态
- **WHEN** 边缘节点调用 `POST /api/edge/heartbeat`
- **THEN** 请求 SHALL 包含 cameraStatuses 数组
- **AND** 每个摄像头状态包含：
  - cameraId: 摄像头ID
  - status: ONLINE/OFFLINE/ERROR
  - failureReason: 失败原因（如适用）
  - bitrate: 当前码率
  - fps: 当前帧率

#### Scenario: 中央服务更新摄像头状态
- **WHEN** 边缘节点心跳包含摄像头状态更新
- **AND** 摄像头状态与中央记录不一致
- **THEN** 中央服务 SHALL 采用边缘节点上报的状态
- **AND** 记录状态变更历史

#### Scenario: 边缘节点上报与本地检测冲突时优先使用上报状态
- **WHEN** 中央服务本地检测到摄像头异常（如本地检测报告OFFLINE）
- **AND** 边缘节点在心跳中报告摄像头 ONLINE
- **THEN** 系统 SHALL 信任边缘节点的上报状态
- **AND** 将摄像头标记为 ONLINE

#### Scenario: 心跳超时未上报摄像头状态
- **WHEN** 边缘节点心跳超时未到达
- **AND** 上次心跳中未包含摄像头状态
- **THEN** 系统 SHALL 将该节点关联的摄像头状态标记为 UNKNOWN
- **AND** 触发状态重新检测流程

---

### Requirement: 摄像头级别健康检查

系统 SHALL 支持摄像头级别的健康检查，不仅依赖节点心跳，还监控实际视频流状态。

#### Scenario: 定期摄像头健康检查
- **WHEN** 边缘节点检测到摄像头异常（如视频流中断）
- **THEN** 节点 SHALL 立即更新本地状态
- **AND** 在下次心跳时上报到中央服务

#### Scenario: 健康检查异常触发告警
- **WHEN** 摄像头状态变为 ERROR
- **AND** 持续时间超过告警阈值（默认5分钟）
- **THEN** 系统 SHALL 发送告警通知
- **AND** 记录告警事件
