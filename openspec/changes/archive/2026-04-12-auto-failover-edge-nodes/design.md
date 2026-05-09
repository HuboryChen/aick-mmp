## Context

**当前状态：**
- 系统采用中心-边缘架构，摄像头(Camera)以 N:1 关系归属于边缘节点(EdgeNode)
- `EdgeNodeHealthService` 每60秒扫描节点心跳，3分钟无响应将节点标记为 OFFLINE
- 节点离线后，其上的摄像头 `edgeNodeId` 仍指向已离线节点，成为"孤儿"
- 已有手动迁移 API (`POST /cameras/batch-update-edge-node`) 和自动分配 API (`POST /cameras/auto-assign`)，但仅处理 `edgeNodeId IS NULL` 的摄像头
- 负载均衡算法 `selectOptimalEdgeNode()` 基于四因子加权（CPU/内存/磁盘/摄像头数），但未考虑地域亲和性

**约束条件：**
- 遵循 DDD 分层：领域层 → 应用层 → 基础设施层
- 技术栈：Spring Boot + JPA + PostgreSQL
- 数据库变更通过重新初始化即可（开发阶段）
- 不修改边缘节点端代码
- 复用现有负载均衡算法，在其上增强

## Goals / Non-Goals

**Goals:**
1. 边缘节点 OFFLINE 时自动将其上的在线摄像头迁移到健康节点
2. 迁移过程可配置、可观测、可手动干预
3. 无健康节点可接收时，摄像头安全进入待分配池
4. 故障转移事件完整记录，支持审计和回溯
5. 地域亲和性：优先选择同区域的备用节点

**Non-Goals:**
1. **不涉及**活跃视频流会话的实时切换（StreamSession 由客户端重连自然恢复）
2. **不涉及**前端 UI 改造（后续迭代添加监控面板）
3. **不涉及**边缘节点端的任何代码变更
4. **不实现**跨数据中心级别的故障转移（单区域内的节点间转移）
5. **不修改**现有手动批量迁移 API 的行为

## Decisions

### 决策1: 故障转移触发方式 — 在 markNodeOffline() 中同步触发

**选择**: 在 `EdgeNodeHealthService.markNodeOffline()` 方法末尾直接调用故障转移服务

**替代方案**:
| 方案 | 描述 | 优缺点 |
|------|------|--------|
| A. 同步触发 | 标记OFFLINE后立即执行转移 | ✅ 简单直接；⚠️ 可能延长健康检查线程的执行时间 |
| B. Spring Event 异步触发 | 发布 `NodeOfflineEvent`，监听器异步处理 | ✅ 解耦，不阻塞健康检查；⚠️ 增加复杂度 |
| C. 定时任务扫描 | 单独的定时任务扫描OFFLINE节点的孤儿摄像头 | ✅ 完全解耦；⚠️ 有延迟（最多一个扫描周期） |

**最终选择方案A（同步触发）+ 配置化开关**:
- 默认行为：同步触发，但通过 `@Async` 将实际转移逻辑放到异步线程池
- 配置项 `failover.enabled=true` 可完全禁用
- 配置项 `failover.mode=async` 控制同步/异步模式
- 原因：保持简单性，同时避免阻塞健康检查主循环

### 决策2: 待分配池实现 — 基于数据库状态，非内存队列

**选择**: 摄像头的 `edgeNodeId = NULL` 且 `status = PENDING_ALLOCATION` 即表示在待分配池中

**替代方案**:
| 方案 | 描述 | 优缺点 |
|------|------|--------|
| A. 数据库状态标记 | 新增 CameraStatus 枚举值 | ✅ 持久化，重启不丢失；⚠️ 需要DB schema变更 |
| B. Redis 队列 | 内存队列存储待分配cameraId | ✅ 高性能；⚠️ 引入新依赖，重启丢失 |
| C. 内存 ConcurrentHashMap | 服务内维护 | ✅ 最快；⚠️ 重启丢失，多实例不兼容 |

**最终选择方案A**:
- 新增枚举值 `CameraStatus.PENDING_ALLOCATION`
- 通过 `GET /cameras/pending-allocation` 查询
- 节点上线或容量释放时由定时任务尝试分配
- 原因：符合现有数据模型风格，无需引入新依赖，天然持久化

### 决策3: 地域亲和性增强 — 扩展现有权重算法

**选择**: 在 `selectOptimalEdgeNode()` 方法中增加地域bonus因子，而非独立方法

**实现方式**:
```
原始得分 = W_load × W_cpu × W_mem × W_disk
故障转移模式得分 = 原始得分 × (1 + region_bonus)

其中:
- 同区域 → region_bonus = 0.3 (30%加成)
- 同城市不同区域 → region_bonus = 0.1 (10%加成)
- 不同城市 → region_bonus = 0
```

**原因**: 
- 最小改动原则，复用现有算法
- bonus机制保证地域优先但不绝对（极端情况下仍可跨区域）
- 通过方法参数 `preferRegionId` 控制是否启用（普通分配时传null）

### 决策4: 并发控制 — 数据库级乐观锁 + 应用级信号量

**选择**: 
- 使用 `@Version` 乐观锁防止并发更新冲突
- 应用层使用 `Semaphore(5)` 限制同时进行中的故障转移任务数（避免瞬间大量DB操作）
- 每次故障转移以批次为单位（每批20个摄像头），批次间有1秒间隔

**原因**: 系统不是高并发场景，简单的信号量+批次处理足够

### 决策5: 事件记录 — 独立表 + 实体

**选择**: 新建 `CameraFailoverEvent` 实体和 `camera_failover_events` 表

```sql
CREATE TABLE camera_failover_events (
    id BIGSERIAL PRIMARY KEY,
    source_edge_node_id BIGINT NOT NULL,
    target_edge_node_id BIGINT,
    camera_ids BIGINT[] NOT NULL,          -- 本次迁移的摄像头ID列表
    total_count INT NOT NULL,              -- 迁移总数
    success_count INT NOT NULL DEFAULT 0,  -- 成功数
    failed_count INT NOT NULL DEFAULT 0,   -- 失败数
    trigger_type VARCHAR(20) NOT NULL,     -- AUTO / MANUAL
    status VARCHAR(20) NOT NULL,           -- IN_PROGRESS / COMPLETED / PARTIAL / FAILED
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);
```

**原因**: 
- 数组类型存储 camera_ids 避免关联表复杂查询
- 支持 `trigger_type` 区分自动/手动触发
- `status` 支持跟踪长时间运行的任务进度

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| **级联故障转移风暴** | 多个节点同时离线导致大量摄像头同时迁移 | Semaphore限制并发数 + 批次间隔 + 配置开关 |
| **目标节点过载** | 接收迁移摄像头后超过容量上限 | 分配前严格校验 `currentCameraCount < maxCameraSupport` |
| **网络分区脑裂** | 节点暂时不可达被误判为离线，触发不必要的转移 | 心跳超时设为3分钟（较保守）；转移前可选延迟等待期 |
| **部分失败** | 一批摄像头中部分分配成功、部分失败 | 事件记录 success_count / failed_count；失败的进入待分配池 |
| **重复转移** | 节点短暂抖动导致 ON/OFF 反复触发 | 检查摄像头当前 edgeNodeId 是否仍指向源节点再决定是否迁移 |
| **数据一致性** | 迁移过程中有新的摄像头绑定到该节点 | 事务边界：每次批次在一个事务内完成 |

## Migration Plan

1. **数据库变更**: 执行 SQL 添加 `camera_failover_events` 表和 `CameraStatus.PENDING_ALLOCATION` 枚举值映射
2. **配置新增**: 在 `application.yml` 中添加 `edge.failover.*` 配置组
3. **代码部署**: 标准 Spring Boot 重启，无特殊迁移步骤
4. **回滚策略**: 设置 `edge.failover.enabled=false` 即可禁用功能回退到原有行为

## Open Questions

1. ~~故障转移应该同步还是异步？~~ → **已决策**: 可配置，默认异步
2. ~~待分配池如何实现？~~ → **已决策**: 数据库状态标记
3. 是否需要通知机制（邮件/webhook）通知管理员故障转移发生？→ **本次不实现**，留作后续迭代
4. 摄像头从待分配池自动重新分配的频率？→ **建议复用现有的5分钟重连周期**，或单独配置 `failover.retry-interval-seconds=300`
