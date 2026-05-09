## 1. 数据层变更

- [x] 1.1 在 `CameraStatus` 枚举中新增 `PENDING_ALLOCATION` 状态值
- [x] 1.2 创建 `CameraFailoverEvent` 实体类，字段包含：sourceEdgeNodeId、targetEdgeNodeIds（集合）、cameraIds（集合）、totalCount、successCount、failedCount、triggerType（枚举 AUTO/MANUAL）、status（枚举 IN_PROGRESS/COMPLETED/PARTIAL/FAILED）、errorMessage、createdAt、completedAt
- [x] 1.3 创建 `CameraFailoverEventRepository` 接口，继承 `JpaRepository`
- [x] 1.4 编写数据库迁移 SQL（或 Flyway 脚本），创建 `camera_failover_events` 表

## 2. 配置属性

- [x] 2.1 创建 `EdgeFailoverProperties` 配置类，绑定 `edge.failover.*` 前缀的所有配置项（enabled、mode、delaySeconds、maxConcurrentTasks、batchSize、batchDelayMs、regionBonus、retryIntervalSeconds）
- [x] 2.2 在 `application.yml` 中添加默认配置值

## 3. 领域服务：故障转移核心逻辑

- [x] 3.1 创建 `EdgeNodeFailoverService` 接口，定义方法：`triggerFailover(Long sourceNodeId, FailoverTriggerType triggerType)` 和 `processPendingAllocationPool()`
- [x] 3.2 创建 `EdgeNodeFailoverServiceImpl` 实现类：
  - [x] 3.2.1 实现 `triggerFailover()` 方法主流程：查询候选摄像头 → 创建事件记录 → 分批处理 → 更新事件状态
  - [x] 3.2.2 实现 `selectTargetNodeForFailover()` 方法：复用现有四因子权重算法 + 地域bonus加成 + 容量校验
  - [x] 3.2.3 实现批次处理逻辑：按 batchSize 分割 → 事务内批量更新 edgeNodeId → 失败的设为 PENDING_ALLOCATION
  - [x] 3.2.4 实现重复迁移检测：跳过 edgeNodeId != sourceNodeId 的摄像头
  - [x] 3.2.5 实现 `processPendingAllocationPool()` 方法：查询待分配池 → 尝试分配到在线节点
- [x] 3.3 使用 `@Async` 和 `Semaphore` 实现异步执行和并发控制

## 4. 触发集成

- [x] 4.1 修改 `EdgeNodeHealthService.markNodeOffline()` 方法，在标记 OFFLINE 后调用 `edgeNodeFailoverService.triggerFailover(nodeId, AUTO)`（需注入服务依赖）
- [x] 4.2 实现延迟等待机制：当 `delaySeconds > 0` 时，延迟执行并在等待期间检测节点是否恢复（恢复则取消任务）

## 5. REST API 端点

- [x] 5.1 在 `EdgeNodeController` 中新增 `POST /{id}/trigger-failover` 端点，调用 `triggerFailover(id, MANUAL)` 并返回事件ID
- [x] 5.2 新建 `FailoverEventController`（或在现有 Controller 中添加），实现 `GET /failover-events` 查询接口，支持按 sourceEdgeNodeId、triggerType、status 筛选和分页
- [x] 5.3 在 `CameraController` 中确认 `GET /pending-allocation` 端点（或新增），返回待分配池摄像头列表

## 6. 定时任务

- [x] 6.1 创建定时任务 `PendingAllocationScheduler`，按 `retryIntervalSeconds` 间隔调用 `processPendingAllocationPool()`
- [x] 6.2 （可选增强）监听节点 ONLINE 事件，触发时立即尝试分配待分配池（本次通过定时任务覆盖，暂不单独实现）

## 7. 单元测试

- [x] 7.1 编写 `EdgeNodeFailoverServiceImplTest`：测试成功分配到同区域节点、跨区域分配回退、无可用节点进入待分配池、并发限制、批次处理等场景
- [x] 7.2 编写触发集成测试：验证 `markNodeOffline()` 正确调用故障转移服务
- [x] 7.3 编写 API 测试：验证手动触发端点和事件查询端点

## 8. 文档更新

- [x] 8.1 更新 `spec/AI2AI/后端架构信息.md`：添加 EdgeNodeFailoverService 模块说明
- [x] 8.2 更新 `spec/AI2AI/协议和数据.md`：添加 CameraFailoverEvent 表结构、新增API端点、CameraStatus 枚举新值
