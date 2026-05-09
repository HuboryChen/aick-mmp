## Why

当前边缘节点离线后，其管理的摄像头不会自动迁移到健康的备用节点，导致这些摄像头变成"孤儿"状态——用户无法观看视频流，必须由管理员手动介入进行批量迁移。这违反了需求文档中对系统高可用性的要求，在生产环境中会造成服务中断。

## What Changes

- **新增自动故障转移机制**：当 `EdgeNodeHealthService` 检测到节点变为 OFFLINE 状态时，自动触发该节点上所有在线摄像头的重新分配
- **新增故障转移策略配置**：支持配置故障转移行为（立即转移 / 延迟等待 / 禁用），以及最大并发转移数量
- **新增故障转移事件记录**：每次自动转移生成事件日志，包含源节点、目标节点、迁移的摄像头列表、时间戳
- **增强负载均衡算法**：在故障转移场景下优先选择同区域/同城市的健康节点（地域亲和性）
- **新增 REST API**：`POST /edge-nodes/{id}/trigger-failover` 支持管理员手动触发指定节点的故障转移
- **新增摄像头待分配池概念**：无法分配的摄像头进入"待分配"队列，当有新节点上线或已有节点容量释放时自动分配

### New Capabilities

- `edge-node-failover`: 边缘节点故障时的自动摄像头迁移机制，包括触发条件、分配策略、事件记录和恢复流程

### Modified Capabilities

（无现有spec需要修改）

## Impact

**受影响的后端模块：**
- `EdgeNodeHealthService` — 需要在 `markNodeOffline()` 方法中集成故障转移触发逻辑
- `CameraServiceImpl` — 新增 `failoverCamerasForNode()` 方法，复用现有负载均衡算法
- `EdgeNodeController` — 新增手动触发故障转移的端点
- 数据库 — 新增 `camera_failover_events` 表记录转移历史

**受影响的API：**
- 新增: `POST /api/edge-nodes/{id}/trigger-failover`
- 新增: `GET /api/cameras/pending-allocation` (查询待分配池)
- 修改: 节点状态变更事件可能需要扩展事件payload

**不受影响的部分：**
- 前端UI（本次不涉及，后续迭代可添加故障转移监控面板）
- 边缘节点代码（故障转移是中心服务器侧的行为）
- 视频流服务（StreamSession 不受影响，客户端重连时自然获取新地址）
