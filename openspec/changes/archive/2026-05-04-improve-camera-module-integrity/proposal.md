## Why

摄像头管理模块存在业务闭环缺陷，影响系统数据一致性和运维效率。当前问题包括：Recording实体缺少软删除能力导致录像数据无法恢复、删除摄像头时未级联处理关联录像、API权限设计不合理、缺乏摄像头统计能力，以及边缘节点与摄像头状态同步机制不完善。这些问题在生产环境中可能导致数据丢失风险和运维困难。

## What Changes

### 核心数据模型增强
- **Recording软删除支持**：为Recording实体添加deletedAt时间戳字段，支持软删除和恢复
- **级联删除逻辑**：删除摄像头时自动处理关联录像（软删除或标记无效）

### API层优化
- **权限设计调整**：优化搜索等API的权限要求，从OPERATOR扩展到ADMIN/OPERATOR/VIEWER
- **批量操作标准化**：统一批量操作返回值格式

### 业务功能增强
- **摄像头统计聚合API**：新增按状态、节点、区域的摄像头统计接口
- **边缘节点↔摄像头状态双向同步**：边缘节点上报本地摄像头实际状态，解决心跳正常但视频流不可用的问题

### 录像计划管理
- **录像计划能力**：支持定时录像配置（NEW capability）
- **移动侦测录像**：基于运动检测触发录像（NEW capability）

## Capabilities

### New Capabilities

- `recording-schedule`: 录像计划管理，支持定时录像配置、时间段设置、录像模式选择
- `motion-detection-recording`: 移动侦测录像，基于帧差分算法检测运动并触发录像

### Modified Capabilities

- `camera-management`: 扩展删除流程，增加录像级联处理；新增统计聚合查询
- `edge-node-failover`: 扩展状态上报机制，增加摄像头级别状态同步

## Impact

### 后端影响
- `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Recording.java`：新增deletedAt字段
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java`：增强删除逻辑
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraController.java`：新增统计API、调整权限
- `backend/aick-mmp-edge/`：边缘节点状态上报增强

### 前端影响
- `frontend/src/pages/CameraManagement.js`：增强筛选和统计展示
- 新增录像计划配置页面

### 数据库迁移
- Recording表新增deleted_at列
