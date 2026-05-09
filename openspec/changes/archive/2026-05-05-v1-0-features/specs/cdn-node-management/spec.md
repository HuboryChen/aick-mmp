# CDN节点管理

## ADDED Requirements

### Requirement: CDN节点注册

系统应允许管理员注册CDN节点用于内容分发。

#### Scenario: 注册CDN节点
- **WHEN** 管理员发送 POST /api/v1/cdn-nodes 并携带节点信息
- **THEN** 系统应创建新的CDN节点
- **AND** 系统应验证node_id唯一
- **AND** 系统应设置初始状态为'pending'
- **AND** 系统应执行到节点的连通性测试

#### Scenario: 验证CDN节点ID唯一性
- **WHEN** 管理员尝试注册使用已存在node_id的CDN节点
- **THEN** 系统应拒绝注册
- **AND** 系统应返回错误，指示node_id已存在

### Requirement: CDN节点健康监控

系统应持续监控CDN节点的健康状态。

#### Scenario: 健康检查通过
- **WHEN** CDN节点在超时期限内响应健康检查
- **THEN** 节点状态应设置为'online'
- **AND** last_health_check时间戳应被更新

#### Scenario: 健康检查失败
- **WHEN** CDN节点在3次连续尝试后未能响应健康检查
- **THEN** 节点状态应设置为'offline'
- **AND** 系统应生成节点离线告警

#### Scenario: 健康检查频率
- **WHEN** CDN健康监控服务运行
- **THEN** 它应每30秒执行一次健康检查
- **AND** 它应根据响应更新节点状态

### Requirement: CDN节点负载信息

系统应收集并跟踪CDN节点的负载信息。

#### Scenario: 上报CDN节点负载
- **WHEN** CDN节点通过API上报其负载信息
- **THEN** 系统应存储负载指标，包括：
  - CPU利用率百分比
  - 内存利用率百分比
  - 磁盘利用率百分比
  - 当前连接数
  - 网络带宽使用

#### Scenario: 获取CDN节点负载
- **WHEN** 用户发送 GET /api/v1/cdn-nodes/{id}/load
- **THEN** 系统应返回节点的最新负载信息

### Requirement: CDN节点选择算法

系统应使用地理邻近性和加权最小连接数算法为视频流分发选择合适的CDN节点。

#### Scenario: 按地理邻近性选择CDN节点
- **WHEN** 来自边缘节点的视频流请求
- **THEN** 系统应优先选择同一地理区域内的CDN节点
- **AND** 如果本地节点不可用，系统应选择最近的可用节点

#### Scenario: 应用加权最小连接数
- **WHEN** 同一地理区域内有多个CDN节点可用
- **THEN** 系统应选择（当前连接数 / 权重）比率最低的节点
- **AND** 选择后系统应更新节点的连接数

#### Scenario: 处理CDN节点不可用
- **WHEN** 选定的CDN节点离线或过载
- **THEN** 系统应选择下一个最佳可用CDN节点
- **AND** 系统应记录故障转移事件

### Requirement: CDN节点CRUD操作

系统应提供完整的CDN节点CRUD操作。

#### Scenario: 创建CDN节点
- **WHEN** 管理员发送 POST /api/v1/cdn-nodes
- **THEN** 系统应创建新的CDN节点
- **AND** 系统应返回创建的节点详情

#### Scenario: 列出CDN节点
- **WHEN** 用户发送 GET /api/v1/cdn-nodes
- **THEN** 系统应返回分页的CDN节点列表
- **AND** 响应应包括节点状态、位置和负载信息

#### Scenario: 获取CDN节点详情
- **WHEN** 用户发送 GET /api/v1/cdn-nodes/{id}
- **THEN** 系统应返回指定CDN节点的详细信息
- **AND** 响应应包括最近的负载历史

#### Scenario: 更新CDN节点
- **WHEN** 管理员发送 PUT /api/v1/cdn-nodes/{id} 并携带更新信息
- **THEN** 系统应更新CDN节点配置
- **AND** 如果权重变更，系统应保留现有连接

#### Scenario: 删除CDN节点
- **WHEN** 管理员发送 DELETE /api/v1/cdn-nodes/{id}
- **THEN** 系统应将CDN节点标记为已删除
- **AND** 系统应优雅地将现有连接迁移到其他节点
- **AND** 删除应为软删除

### Requirement: CDN节点权重配置

系统应允许管理员配置CDN节点权重用于负载均衡。

#### Scenario: 设置CDN节点权重
- **WHEN** 管理员更新CDN节点的权重
- **THEN** 系统应更新权重值
- **AND** 系统应在后续负载均衡决策中使用新权重
- **AND** 如果未指定，权重应默认为1

#### Scenario: 权重影响负载分布
- **WHEN** 两个CDN节点的权重分别为2和1
- **THEN** 权重为2的节点应接收大约两倍的连接
- **AND** 分布应基于WLC算法

### Requirement: CDN节点容量管理

系统应强制执行CDN节点的容量限制。

#### Scenario: 强制执行连接限制
- **WHEN** CDN节点达到其最大连接限制
- **THEN** 系统应停止向该节点分配新连接
- **AND** 系统应选择备用CDN节点

#### Scenario: 高负载告警
- **WHEN** CDN节点的负载超过80%阈值
- **THEN** 系统应生成警告告警
- **AND** 告警应指示哪个资源过载（CPU/内存/磁盘）

### Requirement: CDN节点统计

系统应提供CDN节点的使用统计。

#### Scenario: 获取CDN节点统计
- **WHEN** 用户发送 GET /api/v1/cdn-nodes/{id}/stats
- **THEN** 系统应返回统计信息，包括：
  - 传输的总字节数
  - 平均带宽
  - 峰值带宽
  - 随时间变化的连接数
  - 正常运行时间百分比

#### Scenario: 汇总CDN统计
- **WHEN** 用户发送 GET /api/v1/cdn-nodes/stats
- **THEN** 系统应返回所有CDN节点的汇总统计
- **AND** 响应应包括总带宽和表现最佳的节点