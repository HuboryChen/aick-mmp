## Why

摄像头管理模块存在多个严重缺陷，影响系统安全性和数据完整性：
- 逻辑运算符优先级错误导致高负载节点未被正确过滤
- 认证注解格式错误导致权限验证失效
- 软删除功能未实现，违反架构规范

## What Changes

### Critical Fixes
- 修复 `CameraServiceImpl.calculateNodeWeight()` 中的逻辑运算符优先级问题
- 修正 `CameraController` 中 `@PreAuthorize` 注解的格式错误
- 实现软删除功能，替代硬删除

### Quality Improvements
- 抽取重复的节点权重计算逻辑到共享服务
- 优化 N+1 查询问题，使用 JOIN FETCH 预加载关联数据
- 修复批量更新时的容量检查逻辑缺陷
- 实现未完成的 `getOnlineCameras`、`stopCameraStream`、`testConnection` 功能

### Minor Fixes
- 规范 HTTP 状态码（创建资源返回 201）
- 规范化异常类型，使用业务异常替代 RuntimeException

## Capabilities

### New Capabilities

- `camera-soft-delete`: 实现摄像头的软删除功能，包括：
  - 添加 `deletedAt` 时间戳字段
  - 查询时自动过滤已删除记录
  - 提供恢复已删除摄像头的能力

- `node-weight-calculation`: 节点权重计算服务：
  - 抽取 `calculateNodeWeight` 逻辑到独立服务
  - 统一边缘节点和摄像头服务的权重计算逻辑
  - 修复逻辑运算符优先级问题

### Modified Capabilities

- `edge-node-failover`: 
  - 修改节点选择逻辑，修复权重计算错误
  - 移除重复的权重计算代码，改用共享服务

## Impact

### Affected Code
- `CameraServiceImpl.java`: 修复逻辑错误、实现软删除、优化查询
- `CameraController.java`: 修复认证注解、规范状态码
- `EdgeNodeFailoverServiceImpl.java`: 移除重复代码、使用共享权重服务
- `Camera.java` 实体: 添加 `deletedAt` 字段

### Database Changes
- `cameras` 表添加 `deleted_at` 字段 (TIMESTAMP, nullable)

### API Changes
- 所有查询接口自动过滤已删除的摄像头
- 创建摄像头返回 201 状态码
