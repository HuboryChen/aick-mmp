# Edge Node Failover Specification - Delta

## MODIFIED Requirements

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

### Requirement: 手动分配待分配池摄像头（增强）

手动分配待分配池摄像头时，系统 SHALL 支持按地区优先分配策略。

#### Scenario: 手动分配时优先考虑地区
- **WHEN** 管理员调用 `POST /cameras/auto-assign` 并指定地区参数
- **THEN** 系统 SHALL 优先将该地区的待分配摄像头分配到同地区节点
- **AND** 如果同地区无可用节点，再考虑跨地区分配

#### Scenario: 全局手动分配
- **WHEN** 管理员调用 `POST /cameras/auto-assign` 不指定地区
- **THEN** 系统 SHALL 将所有待分配摄像头按照智能分配算法分配
- **AND** 优先考虑地区匹配度

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
