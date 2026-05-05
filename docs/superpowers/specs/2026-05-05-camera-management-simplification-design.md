# 摄像头管理简化功能设计文档

**创建日期**: 2026-05-05
**状态**: 已批准

## 概述

当前摄像头管理操作复杂度较高，用户需要理解 5 种协议类型（RTSP、ONVIF、GB28181、HTTP、RTMP）及其对应的 URL 格式才能完成基本配置。在大规模批量部署场景下，手动选择边缘节点并考虑负载状态的效率低下。摄像头品牌多样化且需要快速扩展支持，但现有系统缺乏统一的配置模板管理机制。

本设计通过配置模板系统、网络发现功能、批量导入功能和区域智能分配来简化摄像头管理操作。

## 目标

- 通过配置模板系统减少用户手动填写技术参数的工作量
- 提供网络发现和批量导入功能，支持大规模快速部署
- 增强边缘节点自动分配，支持基于地区的智能分配策略
- 保持系统可扩展性，支持快速添加新摄像头品牌模板

## 非目标

- 不实现完整的网络设备发现系统（仅针对常见摄像头）
- 不修改现有的摄像头 CRUD 核心逻辑
- 不改变现有的边缘节点心跳和故障转移机制
- 不实现跨环境的模板同步功能

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层                               │
├──────────────┬──────────────┬──────────────┬────────────────┤
│   Config     │    Camera    │   Camera     │   Enhanced     │
│   Template   │  Discovery   │  Batch       │   Camera       │
│  Management  │              │   Import     │  Management    │
└──────┬───────┴──────┬───────┴──────┬───────┴───────┬────────┘
       │              │              │               │
       │              │              │               │
┌──────▼──────────────▼──────────────▼───────────────▼────────┐
│                    API Gateway (Central)                    │
├─────────────────────────────────────────────────────────────┤
│  CameraConfigTemplateController │  CameraDiscoveryController │
│  CameraBatchImportController    │  CameraController (增强)    │
└──────────────┬──────────────────┬───────────────────────────┘
               │                  │
┌──────────────▼──────────────────▼───────────────────────────┐
│                       应用服务层                             │
├─────────────────────────────────────────────────────────────┤
│  CameraConfigTemplateService │ CameraDiscoveryService      │
│  CameraBatchImportService    │ CameraService (增强)         │
└──────────────┬──────────────────┬───────────────────────────┘
               │                  │
┌──────────────▼──────────────────▼───────────────────────────┐
│                       领域服务层                             │
├─────────────────────────────────────────────────────────────┤
│              NodeWeightCalculator (启用区域加成)             │
└───────────────────┬─────────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                    基础设施层                                │
├─────────────────────────────────────────────────────────────┤
│  CameraConfigTemplateRepository │ CameraRepository         │
│  EdgeNodeRepository              │ RegionRepository         │
└─────────────────────────────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                    数据层                                    │
├─────────────────────────────────────────────────────────────┤
│  cameras │ camera_config_templates │ camera_discovery_tasks │
│  camera_batch_import_tasks │ edge_nodes │ regions          │
└─────────────────────────────────────────────────────────────┘
```

## 数据模型

### camera_config_templates 表

存储摄像头品牌/型号的配置模板。

```sql
CREATE TABLE camera_config_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand VARCHAR(100) NOT NULL COMMENT '品牌名称',
    model VARCHAR(100) NOT NULL COMMENT '型号',
    protocol VARCHAR(20) NOT NULL COMMENT '协议类型',
    default_port INT NOT NULL COMMENT '默认端口',
    url_path_template VARCHAR(500) NOT NULL COMMENT 'URL路径模板，支持变量 {ip},{port},{username},{password}',
    preset_parameters JSON COMMENT '预设参数',
    is_preset BOOLEAN DEFAULT FALSE COMMENT '是否预置模板',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '软删除标记',
    deleted_at TIMESTAMP COMMENT '删除时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_brand_model (brand, model, is_deleted),
    INDEX idx_protocol (protocol),
    INDEX idx_brand (brand)
);
```

### camera_discovery_tasks 表

记录网络扫描任务。

```sql
CREATE TABLE camera_discovery_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    network_segment VARCHAR(50) NOT NULL COMMENT '扫描网段，如 192.168.1.0/24',
    status VARCHAR(20) NOT NULL COMMENT '任务状态: PENDING,RUNNING,COMPLETED,FAILED,CANCELLED',
    progress INT DEFAULT 0 COMMENT '扫描进度 0-100',
    total_ips INT DEFAULT 0 COMMENT '总IP数量',
    found_devices JSON COMMENT '发现的设备列表',
    started_at TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
```

### camera_batch_import_tasks 表

记录批量导入任务。

```sql
CREATE TABLE camera_batch_import_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '导入文件名',
    status VARCHAR(20) NOT NULL COMMENT '任务状态: PENDING,VALIDATING,IMPORTING,COMPLETED,FAILED,CANCELLED',
    total_records INT DEFAULT 0 COMMENT '总记录数',
    success_count INT DEFAULT 0 COMMENT '成功数量',
    fail_count INT DEFAULT 0 COMMENT '失败数量',
    error_details JSON COMMENT '错误详情',
    started_at TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
```

## 后端服务设计

### CameraConfigTemplateService

**职责**: 管理摄像头配置模板的增删改查和URL生成。

**主要方法**:
```java
// 创建配置模板
CameraConfigTemplateDTO createTemplate(CreateTemplateRequestDTO request);

// 查询模板列表
Page<CameraConfigTemplateDTO> getTemplates(Pageable pageable, String brand, String protocol);

// 更新模板
CameraConfigTemplateDTO updateTemplate(Long id, UpdateTemplateRequestDTO request);

// 删除模板
void deleteTemplate(Long id);

// 根据模板和参数生成连接URL
String generateUrl(Long templateId, Map<String, String> parameters);

// 根据品牌/型号自动匹配模板
CameraConfigTemplateDTO matchTemplate(String brand, String model);

// 记录模板使用
void incrementUsage(Long templateId);

// 导入模板
List<CameraConfigTemplateDTO> importTemplates(List<CameraConfigTemplateDTO> templates);

// 导出模板
List<CameraConfigTemplateDTO> exportTemplates(List<Long> templateIds);
```

### CameraDiscoveryService

**职责**: 执行网络扫描和设备发现。

**主要方法**:
```java
// 启动网段扫描（异步）
Long startScan(String networkSegment, Long userId);

// 获取扫描进度
ScanProgressDTO getScanProgress(Long taskId);

// 取消扫描任务
void cancelScan(Long taskId);

// 单个IP连通性测试
ConnectivityResultDTO testConnectivity(String ip, Integer port);

// 设备品牌识别
DeviceIdentifyDTO identifyDevice(String ip, Integer port);

// 获取扫描任务历史
Page<DiscoveryTaskDTO> getScanHistory(Pageable pageable, Long userId);
```

**扫描流程**:
1. 解析网段（如 192.168.1.0/24）生成IP列表
2. 分批扫描IP，尝试连接常见端口（554、80、8080、8554）
3. 连接成功后尝试获取设备信息
4. 根据HTTP响应头中的 `Server` 字段识别品牌（如 "Hikvision-Webs" → 海康威视，"Dahua" → 大华），或基于端口默认品牌映射
5. 通过WebSocket实时推送进度

**品牌识别规则**:
- HTTP/ONVIF: 通过HTTP响应头 `Server` 字段匹配
- RTSP: 通过端口默认品牌映射（554端口常见品牌列表）
- 无法识别: 标记为"未知品牌"，允许用户手动选择

### CameraBatchImportService

**职责**: 处理批量导入和进度跟踪。

**主要方法**:
```java
// 生成Excel导入模板
byte[] getImportTemplate();

// 启动批量导入（异步）
Long startImport(MultipartFile file, Long userId);

// 获取导入进度
ImportProgressDTO getImportProgress(Long taskId);

// 取消导入任务
void cancelImport(Long taskId);

// 下载错误报告
byte[] downloadErrorReport(Long taskId);

// 获取导入任务历史
Page<ImportTaskDTO> getImportHistory(Pageable pageable, Long userId);

// 验证导入数据
List<ValidationErrorDTO> validateImportData(List<CameraImportDTO> records);
```

**导入流程**:
1. 解析Excel/CSV文件
2. 验证必填字段和数据格式
3. 匹配配置模板（基于品牌/型号）
4. 自动分配边缘节点（基于地区和负载）
5. 批量创建摄像头记录
6. 通过WebSocket实时推送进度

### 边缘节点分配增强

**修改点**: `CameraService.selectOptimalEdgeNode()` 方法

**原有逻辑**: 使用 `NodeWeightCalculator.calculateWeight()` 计算节点权重

**新逻辑**: 使用 `NodeWeightCalculator.calculateWeightWithRegionBonus()` 增加地区加成

**配置项**:
```yaml
edge-node:
  region-bonus-rate: 0.3  # 同地区节点获得30%的权重加成
  cpu-threshold: 80.0
  memory-threshold: 85.0
```

**加成公式**:
```
基础得分 = calculateWeight(node, cpuUsage, memoryUsage)
是否同地区 = (camera.regionId != null && edgeNode.regionId != null && camera.regionId == edgeNode.regionId) ? 1 : 0
最终得分 = 基础得分 × (1 + 地区加成 × 是否同地区)
```

**地区匹配逻辑**: 当摄像头的 `regionId` 与边缘节点的 `regionId` 相同时，视为同地区。

## 前端组件设计

### ConfigTemplateManagement.jsx

配置模板管理页面。

**功能**:
- 模板列表展示（品牌、型号、协议、使用次数、是否预置）
- 添加/编辑模板表单
- 实时URL预览（输入参数后生成示例URL）
- 模板导入/导出功能
- URL模板变量提示

**URL模板变量**:
- `{ip}` - 摄像头IP地址
- `{port}` - 端口号
- `{username}` - 用户名
- `{password}` - 密码
- `{channel}` - 通道号（可选）

### CameraDiscovery.jsx

网络发现页面。

**功能**:
- 扫描任务表单（网段输入）
- 扫描进度展示（进度条、已扫描IP数、已发现设备数）
- 发现设备列表（IP、端口、识别品牌、推荐模板）
- 设备选择和一键添加
- 扫描任务历史记录
- WebSocket 实时推送扫描进度

**WebSocket 订阅**:
```
/topic/discovery/{taskId}
```

**推送消息格式**:
```json
{
  "taskId": 123,
  "progress": 45,
  "totalIps": 256,
  "scannedIps": 115,
  "foundDevices": [
    {
      "ip": "192.168.1.101",
      "port": 554,
      "brand": "海康威视",
      "model": "DS-2CD2T45D-I5"
    }
  ]
}
```

### CameraBatchImport.jsx

批量导入页面。

**功能**:
- 导入模板下载按钮
- 文件上传组件（支持拖拽，仅限 .xlsx 和 .csv）
- 导入进度展示（进度条、成功数、失败数）
- 导入结果报告
- 错误报告下载
- 导入任务历史
- WebSocket 实时推送导入进度

**WebSocket 订阅**:
```
/topic/import/{taskId}
```

**推送消息格式**:
```json
{
  "taskId": 123,
  "status": "IMPORTING",
  "progress": 60,
  "totalRecords": 100,
  "successCount": 58,
  "failCount": 2,
  "currentRecord": "Camera-061"
}
```

### CameraManagement.jsx 增强点

**新增功能**:
- 添加摄像头表单中增加"配置模板"下拉选择器
- 选择模板后自动填充协议、端口、URL格式
- 显示"推荐边缘节点"标签（基于地区智能分配）
- 顶部工具栏增加"网络发现"和"批量导入"快捷按钮

## API 设计

### 配置模板 API

```
GET    /api/camera-config-templates              - 获取模板列表
POST   /api/camera-config-templates              - 创建模板
GET    /api/camera-config-templates/{id}         - 获取模板详情
PUT    /api/camera-config-templates/{id}         - 更新模板
DELETE /api/camera-config-templates/{id}         - 删除模板
POST   /api/camera-config-templates/{id}/generate - 生成URL
POST   /api/camera-config-templates/match        - 匹配模板
POST   /api/camera-config-templates/import       - 导入模板
GET    /api/camera-config-templates/export       - 导出模板
```

### 网络发现 API

```
POST   /api/camera-discovery/scan                - 启动扫描
GET    /api/camera-discovery/scan/{taskId}/progress - 获取进度
DELETE /api/camera-discovery/scan/{taskId}       - 取消扫描
POST   /api/camera-discovery/test-connectivity   - 测试连通性
GET    /api/camera-discovery/history             - 扫描历史
```

### 批量导入 API

```
GET    /api/camera-batch-import/template         - 下载导入模板
POST   /api/camera-batch-import/import           - 上传并导入
GET    /api/camera-batch-import/{taskId}/progress - 获取进度
DELETE /api/camera-batch-import/{taskId}         - 取消导入
GET    /api/camera-batch-import/{taskId}/errors  - 下载错误报告
GET    /api/camera-batch-import/history          - 导入历史
```

## 部署方案

### 数据库迁移

1. 创建 `camera_config_templates` 表
2. 创建 `camera_discovery_tasks` 表
3. 创建 `camera_batch_import_tasks` 表
4. 插入预置品牌模板数据

### 后端部署

1. 部署新的 Service 和 Controller
2. 更新 `CameraService` 的自动分配逻辑
3. 配置 WebSocket 支持
4. 添加配置项到 `application.yml`

### 前端部署

1. 部署新的页面组件
2. 更新路由配置
3. 更新 `CameraManagement` 组件
4. 配置 WebSocket 连接

### 回滚策略

- 保留原有的 `autoAssignCamerasToEdgeNodes` 方法签名，只增强内部逻辑
- 新增的功能独立部署，不影响现有功能
- 如果出现问题，可通过配置禁用新功能

## 风险与缓解

### 网络扫描可能被防火墙阻止

**风险**: 某些网络环境可能阻止 IP 扫描，导致功能不可用。

**缓解**:
- 提供手动输入 IP 的备选方式
- 扫描前提示用户检查网络权限
- 扫描失败时提供清晰的错误提示

### 模板匹配失败导致配置错误

**风险**: 自动匹配的模板可能不适用于特定设备，导致连接失败。

**缓解**:
- 提供模板测试功能，在保存前验证配置
- 模板匹配失败时标记为警告，不阻止导入
- 支持导入后批量修改配置

### 大规模导入性能问题

**风险**: 一次性导入大量摄像头可能导致数据库压力过大。

**缓解**:
- 实现批次处理（每批 50 条）
- 批次之间增加延迟（默认 100ms）
- 支持分页查询和进度显示

### 地区分配策略可能导致负载不均

**风险**: 过度优先同地区可能导致某些区域节点过载。

**缓解**:
- 地区加分可配置，默认 30%
- 节点负载超过阈值时自动跨地区分配
- 提供手动覆盖选项

## 测试计划

### 单元测试

- `CameraConfigTemplateService` 的所有方法
- `CameraDiscoveryService` 的扫描逻辑
- `CameraBatchImportService` 的解析和验证逻辑
- `NodeWeightCalculator` 的地区加成计算

### 集成测试

- 端到端配置模板创建和使用流程
- 网络扫描到设备发现的完整流程
- 批量导入和错误处理的完整流程
- 区域智能分配的正确性验证

### 性能测试

- 大规模导入性能（100+ 摄像头）
- 网络扫描在不同网络环境下的表现
- WebSocket 并发连接的稳定性

## 配置项

```yaml
# 摄像头配置模板
camera-config-templates:
  preset-templates-path: "classpath:templates/preset-cameras.json"

# 网络扫描
camera-discovery:
  scan-batch-size: 50              # 每批扫描的IP数量
  scan-timeout-ms: 2000            # 单个IP扫描超时时间
  common-ports: [554, 80, 8080, 8554] # 常见端口列表

# 批量导入
camera-batch-import:
  batch-size: 50                   # 批次大小
  batch-delay-ms: 100              # 批次间延迟
  max-file-size-mb: 10             # 最大文件大小

# 边缘节点分配
edge-node:
  region-bonus-rate: 0.3           # 地区加成比例
  cpu-threshold: 80.0              # CPU阈值
  memory-threshold: 85.0           # 内存阈值

# WebSocket
websocket:
  discovery-topic: "/topic/discovery"
  import-topic: "/topic/import"
```

## 依赖项

### 后端新增依赖

```xml
<!-- Apache POI for Excel processing -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- WebSocket support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 前端新增依赖

```json
{
  "xlsx": "^0.18.5",
  "stompjs": "^2.3.3",
  "sockjs-client": "^1.6.1"
}
```
