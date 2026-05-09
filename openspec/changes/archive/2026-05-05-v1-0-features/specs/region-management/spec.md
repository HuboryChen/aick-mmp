# 区域管理

## ADDED Requirements

### Requirement: 区域层级树结构

系统应支持分层树结构来组织区域，支持无限嵌套层级。

#### Scenario: 创建根级区域
- **WHEN** 管理员创建一个不指定父级的新区域
- **THEN** 该区域应被创建为根级区域
- **AND** 该区域的parent_id应设置为NULL

#### Scenario: 创建子区域
- **WHEN** 管理员创建一个指定父级区域的新区域
- **THEN** 该区域应被创建为父级区域的子区域
- **AND** 该区域的parent_id应设置为父级区域的ID

#### Scenario: 获取区域树
- **WHEN** 用户请求区域列表
- **THEN** 系统应以树结构格式返回区域
- **AND** 响应应递归包含所有嵌套的子区域

### Requirement: 区域CRUD操作

系统应提供完整的区域CRUD操作。

#### Scenario: 创建区域
- **WHEN** 管理员发送 POST /api/v1/regions 并携带区域数据
- **THEN** 系统应创建一个新区域
- **AND** 系统应返回创建的区域及其ID

#### Scenario: 读取区域
- **WHEN** 用户发送 GET /api/v1/regions/{id}
- **THEN** 系统应返回区域详情
- **AND** 响应应包括父级区域信息（如适用）

#### Scenario: 更新区域
- **WHEN** 管理员发送 PUT /api/v1/regions/{id} 并携带更新数据
- **THEN** 系统应更新区域
- **AND** 系统应防止在层级中创建循环引用

#### Scenario: 删除空区域
- **WHEN** 管理员发送 DELETE /api/v1/regions/{id} 删除空区域
- **THEN** 系统应删除区域
- **AND** 删除应为软删除（设置deleted_at时间戳）

#### Scenario: 带确认删除非空区域
- **WHEN** 管理员发送 DELETE /api/v1/regions/{id} 并使用 force=true 删除包含子区域或摄像头的区域
- **THEN** 系统应删除区域
- **AND** 所有子区域应被递归删除
- **AND** 区域中的所有摄像头应被重新分配到其父区域或标记为未分配

#### Scenario: 不带确认删除非空区域
- **WHEN** 管理员发送 DELETE /api/v1/regions/{id} 不带force参数删除非空区域
- **THEN** 系统应拒绝删除
- **AND** 系统应返回错误，指示区域非空

### Requirement: 区域移动

系统应允许将区域移动到树中的不同父级区域。

#### Scenario: 移动区域到新父级
- **WHEN** 管理员发送 PATCH /api/v1/regions/{id}/move 并携带 new_parent_id
- **THEN** 系统应更新区域的parent_id
- **AND** 系统应验证新父级存在
- **AND** 系统应防止创建循环引用

#### Scenario: 防止循环引用
- **WHEN** 管理员尝试将区域移动为其自身或其后代的子区域
- **THEN** 系统应拒绝移动
- **AND** 系统应返回错误，指示不允许循环引用

### Requirement: 区域查询和搜索

系统应支持查询和搜索区域。

#### Scenario: 平铺列出所有区域
- **WHEN** 用户发送 GET /api/v1/regions 并使用 flat=true
- **THEN** 系统应以平铺列表返回所有区域
- **AND** 列表应包括parent_id以表示层级关系

#### Scenario: 按名称搜索区域
- **WHEN** 用户发送 GET /api/v1/regions 并使用 search={keyword}
- **THEN** 系统应返回匹配关键词的区域
- **AND** 搜索应不区分大小写

#### Scenario: 按层级过滤区域
- **WHEN** 用户发送 GET /api/v1/regions 并使用 level={number}
- **THEN** 系统应仅返回该层级的区域
- **AND** 级别1应代表根级区域

### Requirement: 区域摄像头分配

系统应支持将摄像头分配给区域。

#### Scenario: 分配摄像头到区域
- **WHEN** 管理员更新摄像头的region_id
- **THEN** 摄像头应与指定区域关联
- **AND** 摄像头应被计入该区域的摄像头统计

#### Scenario: 获取区域摄像头
- **WHEN** 用户发送 GET /api/v1/regions/{id}/cameras
- **THEN** 系统应返回分配给该区域的所有摄像头
- **AND** 如果recursive=true，响应应包括所有后代区域的摄像头

### Requirement: 区域统计

系统应提供区域统计信息。

#### Scenario: 获取区域统计
- **WHEN** 用户发送 GET /api/v1/regions/{id}/stats
- **THEN** 系统应返回统计信息，包括：
  - 区域内总摄像头数
  - 在线摄像头数
  - 离线摄像头数
  - 子区域数

#### Scenario: 获取区域汇总统计
- **WHEN** 用户发送 GET /api/v1/regions/stats
- **THEN** 系统应返回所有区域的汇总统计
- **AND** 响应应按区域级别分组统计