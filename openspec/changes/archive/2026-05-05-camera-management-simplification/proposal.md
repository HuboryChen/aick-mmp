## Why

当前摄像头管理操作复杂度较高，用户需要理解 5 种协议类型（RTSP、ONVIF、GB28181、HTTP、RTMP）及其对应的 URL 格式才能完成基本配置。在大规模批量部署场景下，手动选择边缘节点并考虑负载状态的效率低下，导致部署成本高、易出错。摄像头品牌多样化且需要快速扩展支持，但现有系统缺乏统一的配置模板管理机制。

## What Changes

### 配置模板系统
- 新增摄像头品牌/型号配置模板管理功能
- 支持模板的创建、查询、更新、删除
- 模板包含：协议类型、默认端口、URL 路径模板、预设参数
- 添加摄像头时支持从模板自动填充配置
- 支持常见品牌预置模板（海康威视、大华、宇视等）
- 支持用户自定义模板扩展

### 智能发现与扫描
- 新增网络段扫描发现功能
- 支持输入网段（如 192.168.1.0/24）进行批量扫描
- 自动识别常见摄像头设备
- 扫描结果支持批量选择和一键添加
- 提供 IP 连通性测试功能

### 批量导入功能
- 新增 Excel/CSV 批量导入功能
- 提供标准导入模板下载
- 导入时自动匹配配置模板
- 根据摄像头地区自动分配边缘节点
- 导入结果展示和错误提示

### 边缘节点智能分配增强
- 增强现有 `autoAssignCamerasToEdgeNodes` 功能，支持基于地区的智能分配
- 摄像头添加时根据地区自动推荐最优边缘节点
- 分配算法综合考虑：地区匹配度、节点在线状态、CPU 负载、摄像头容量、带宽
- 支持手动覆盖自动分配结果

## Capabilities

### New Capabilities
- `camera-config-templates`: 摄像头配置模板管理，支持品牌/型号到协议配置的映射和自动 URL 生成
- `camera-discovery`: 网络发现与扫描，支持网段扫描和设备识别
- `camera-batch-import`: 批量导入，支持 Excel/CSV 格式和模板匹配

### Modified Capabilities
- `edge-node-failover`: 增强自动分配算法，新增基于地区的智能分配策略，优先选择同区域的边缘节点

## Impact

**Frontend Changes**:
- 新增配置模板管理页面
- 新增网络发现页面
- 摄像头管理页面增加快速添加、批量导入入口
- 添加摄像头表单增加模板选择和智能分配推荐

**Backend Changes**:
- 新增 `CameraConfigTemplate` 实体和 Repository
- 新增 `CameraConfigTemplateService` 服务
- 新增 `CameraDiscoveryService` 服务
- 新增 `CameraBatchImportService` 服务
- 增强 `CameraService.autoAssignCamerasToEdgeNodes()` 支持地区维度
- 新增对应的 Controller 接口

**Database Changes**:
- 新增 `camera_config_templates` 表
- 可选：新增 `camera_discovery_tasks` 表用于记录扫描任务

**Dependencies**:
- 前端可能需要 Excel 解析库（如 `xlsx`）
- 网络扫描需要网络连通性测试工具
- 模板管理需要 JSON Schema 验证（用于 URL 模板变量）
