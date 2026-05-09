# Node Weight Calculation Specification

## ADDED Requirements

### Requirement: 节点权重计算服务

系统 SHALL 提供统一的 `NodeWeightCalculator` 服务来计算边缘节点的负载权重，用于节点选择决策。

#### Scenario: 使用服务计算节点权重
- **WHEN** 需要为摄像头选择目标节点时
- **THEN** 系统 SHALL 调用 `NodeWeightCalculator.calculateWeight(node, cpuUsage, memoryUsage)`
- **AND** 返回计算后的权重值

---

### Requirement: 高负载节点过滤

权重计算 SHALL 正确实现逻辑：只有当节点的 CPU 使用率 < 80% **且** 内存使用率 < 85% 时，节点才被视为健康的候选节点。

#### Scenario: 正确过滤高负载节点
- **WHEN** 调用权重计算方法
- **AND** CPU 使用率为 85%
- **AND** 内存使用率为 50%
- **THEN** 系统 SHALL 返回低权重值（节点不健康）
- **AND** 节点 SHALL NOT 被选为目标节点

#### Scenario: 正确过滤高内存节点
- **WHEN** 调用权重计算方法
- **AND** CPU 使用率为 60%
- **AND** 内存使用率为 90%
- **THEN** 系统 SHALL 返回低权重值（节点不健康）
- **AND** 节点 SHALL NOT 被选为目标节点

#### Scenario: 低负载节点正常计算
- **WHEN** 调用权重计算方法
- **AND** CPU 使用率为 60%
- **AND** 内存使用率为 70%
- **THEN** 系统 SHALL 返回正常计算的权重值
- **AND** 节点可作为候选目标

#### Scenario: 资源使用率为空时视为健康
- **WHEN** 调用权重计算方法
- **AND** CPU 使用率为 NULL
- **AND** 内存使用率为 NULL
- **THEN** 系统 SHALL 假设节点健康
- **AND** 返回正常的权重计算结果

#### Scenario: CPU 为空时检查内存
- **WHEN** 调用权重计算方法
- **AND** CPU 使用率为 NULL
- **AND** 内存使用率为 90%
- **THEN** 系统 SHALL 返回低权重值（内存过高）

---

### Requirement: 四因子权重计算

健康节点的权重 SHALL 基于以下四个因子计算：

| 因子 | 说明 | 权重影响 |
|------|------|----------|
| 容量因子 | 当前摄像头数量 / 最大容量 | 容量越低，权重越高 |
| CPU 因子 | CPU 使用率 | 使用率越低，权重越高 |
| 内存因子 | 内存使用率 | 使用率越低，权重越高 |
| 响应时间因子 | 最后响应时间 | 响应越快，权重越高 |

#### Scenario: 计算综合权重
- **WHEN** 调用权重计算方法
- **AND** 节点满足健康条件（CPU < 80% 且 内存 < 85%）
- **THEN** 系统 SHALL 计算四因子加权得分
- **AND** 返回 0-100 之间的权重值

---

### Requirement: 同区域加权

故障转移场景中，同区域的节点 SHALL 获得额外的权重加成。

#### Scenario: 同区域节点获得加成
- **WHEN** 计算故障转移目标节点的权重
- **AND** 目标节点与源节点属于同一区域
- **THEN** 系统 SHALL 在基础权重上增加区域加成（默认 30%）

#### Scenario: 跨区域节点无加成
- **WHEN** 计算故障转移目标节点的权重
- **AND** 目标节点与源节点属于不同区域
- **THEN** 系统 SHALL 使用基础权重，不增加区域加成

---

### Requirement: 权重排序选择

节点选择 SHALL 按权重降序排列，优先选择权重最高的节点。

#### Scenario: 选择权重最高的节点
- **WHEN** 需要为摄像头选择目标节点
- **AND** 存在多个健康的边缘节点
- **THEN** 系统 SHALL 选择权重值最高的节点

#### Scenario: 最高权重节点容量已满时顺延
- **WHEN** 选择权重最高的节点
- **AND** 该节点的 `currentCameraCount >= maxCameraSupport`
- **THEN** 系统 SHALL 跳过该节点
- **AND** 选择下一个权重最高的节点
