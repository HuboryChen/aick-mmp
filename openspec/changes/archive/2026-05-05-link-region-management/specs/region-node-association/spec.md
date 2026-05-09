# 区域节点关联

## ADDED Requirements

### Requirement: 边缘节点区域关联

边缘节点 SHALL 支持通过 `region_id` 与区域管理进行关联，`location` 字段保留作为详细地址。创建、更新、查询时均支持指定区域。

#### Scenario: 创建边缘节点时指定区域
- **WHEN** 管理员发送 POST /api/v1/edge-nodes 并携带 regionId
- **THEN** 系统应将边缘节点与指定区域关联
- **AND** 系统应验证 regionId 对应的区域存在

#### Scenario: 更新边缘节点区域
- **WHEN** 管理员发送 PUT /api/v1/edge-nodes/{id} 并修改 regionId
- **THEN** 系统应更新边缘节点的区域关联
- **AND** 系统应验证新 regionId 有效

#### Scenario: 按区域查询边缘节点
- **WHEN** 用户发送 GET /api/v1/edge-nodes 并携带 regionId 参数
- **THEN** 系统应返回属于该区域的边缘节点
- **AND** 如果 recursive=true，应返回该区域及所有子区域的边缘节点

#### Scenario: 按区域查询边缘节点（包含子区域）
- **WHEN** 用户发送 GET /api/v1/edge-nodes 并携带 regionId 和 recursive=true
- **THEN** 系统应返回该区域及其所有子区域的边缘节点
- **AND** 响应应包含节点所属的具体区域信息

### Requirement: CDN节点区域关联

CDN节点 SHALL 支持通过 `region_id` 与区域管理进行关联，移除原有的自由文本 region 和 region_code 字段。

#### Scenario: 创建CDN节点时指定区域
- **WHEN** 管理员发送 POST /api/v1/cdn-nodes 并携带 regionId
- **THEN** 系统应将CDN节点与指定区域关联
- **AND** 系统应验证 regionId 对应的区域存在

#### Scenario: 更新CDN节点区域
- **WHEN** 管理员发送 PUT /api/v1/cdn-nodes/{id} 并修改 regionId
- **THEN** 系统应更新CDN节点的区域关联
- **AND** 系统应移除旧的 region 和 region_code 字段数据

#### Scenario: 按区域查询CDN节点
- **WHEN** 用户发送 GET /api/v1/cdn-nodes 并携带 regionId 参数
- **THEN** 系统应返回属于该区域的CDN节点
- **AND** 如果 recursive=true，应返回该区域及所有子区域的CDN节点

### Requirement: 区域统计扩展

区域统计接口 SHALL 支持边缘节点数和CDN节点数的递归统计（包含所有子区域）。

#### Scenario: 获取区域节点统计（递归）
- **WHEN** 用户发送 GET /api/v1/regions/{id}/stats
- **THEN** 系统应返回统计信息，包括：
  - edgeNodeCount: 该区域及所有子区域的边缘节点数量
  - cdnNodeCount: 该区域及所有子区域的CDN节点数量
  - directEdgeNodeCount: 该区域的直接边缘节点数量（不含子区域）
  - directCdnNodeCount: 该区域的直接CDN节点数量（不含子区域）

#### Scenario: 获取区域汇总节点统计
- **WHEN** 用户发送 GET /api/v1/regions/stats
- **THEN** 系统应在每个区域的统计中包含边缘节点数和CDN节点数（递归）
- **AND** 统计应包含该区域及所有子区域的节点

### Requirement: 摄像头区域关联校验

系统 SHALL 校验摄像头管理页面的区域选择功能已正确实现。

#### Scenario: 创建摄像头时选择区域
- **WHEN** 管理员发送 POST /api/v1/cameras 并携带 regionId
- **THEN** 系统应将摄像头与指定区域关联
- **AND** 前端应使用区域树形选择器

#### Scenario: 按区域查询摄像头
- **WHEN** 用户发送 GET /api/v1/cameras 并携带 regionId 参数
- **THEN** 系统应返回属于该区域的摄像头
- **AND** 如果 recursive=true，应返回该区域及所有子区域的摄像头
