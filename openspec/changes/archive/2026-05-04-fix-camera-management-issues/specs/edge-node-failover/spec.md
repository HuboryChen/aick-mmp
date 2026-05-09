# Edge Node Failover - Delta Specification

## MODIFIED Requirements

### Requirement: 摄像头迁移分配策略

故障转移过程中，系统 SHALL 基于增强的负载均衡算法为每个待迁移摄像头选择最优的目标边缘节点。算法在故障转移模式下 SHALL 优先考虑地域亲和性。

**变更说明**: 修改节点权重计算方式，使用共享的 `NodeWeightCalculator` 服务替代内嵌实现，确保逻辑运算符优先级正确。

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

#### Scenario: 高负载节点不被选为目标
- **WHEN** 执行故障转移分配
- **AND** 某节点的 CPU 使用率 >= 80%
- **OR** 某节点的内存使用率 >= 85%
- **THEN** 该节点 SHALL NOT 被选为迁移目标节点
- **AND** 系统 SHALL 选择下一个权重最高的健康节点

#### Scenario: 权重计算使用共享服务
- **WHEN** 需要计算节点权重
- **THEN** 系统 SHALL 调用 `NodeWeightCalculator.calculateWeight()` 方法
- **AND** SHALL NOT 在 `EdgeNodeFailoverServiceImpl` 中内嵌权重计算逻辑
