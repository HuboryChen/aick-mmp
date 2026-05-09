## Context

当前摄像头管理系统要求用户手动配置协议类型、URL 格式等技术细节，对非技术用户造成较高的认知负荷。在大规模批量部署场景下，手动选择边缘节点并考虑负载状态的效率低下。摄像头品牌多样化且需要快速扩展支持，但缺乏统一的配置模板管理机制。

现有系统已实现基础的边缘节点自动分配功能（`autoAssignCamerasToEdgeNodes`），但未考虑地区维度，无法满足按地区分布的边缘节点部署模式。

系统采用 Spring Boot 3.2.5 + React 18 + Ant Design 5.x 技术栈，数据库使用 MySQL 8.0。后端采用 DDD 分层架构（领域层、应用层、基础设施层），前端采用三层架构（CSS 变量、组件类、原子工具类）。

## Goals / Non-Goals

**Goals:**

- 通过配置模板系统减少用户手动填写技术参数的工作量
- 提供网络发现和批量导入功能，支持大规模快速部署
- 增强边缘节点自动分配，支持基于地区的智能分配策略
- 保持系统可扩展性，支持快速添加新摄像头品牌模板

**Non-Goals:**

- 不实现完整的网络设备发现系统（仅针对常见摄像头）
- 不修改现有的摄像头 CRUD 核心逻辑
- 不改变现有的边缘节点心跳和故障转移机制
- 不实现跨环境的模板同步功能

## Decisions

### 配置模板数据模型

使用单独的 `camera_config_templates` 表存储配置模板，而不是硬编码在代码中。这样支持运行时动态添加模板，无需重新部署。

**理由：**
- 摄像头品牌和型号繁多，预置模板无法覆盖所有情况
- 用户可能需要自定义模板以适应特殊设备
- 便于模板的导入导出和跨环境迁移

**数据表结构：**
```sql
CREATE TABLE camera_config_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    default_port INT NOT NULL,
    url_path_template VARCHAR(500) NOT NULL,
    preset_parameters JSON,
    is_preset BOOLEAN DEFAULT FALSE,
    usage_count INT DEFAULT 0,
    last_used_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_brand_model (brand, model, is_deleted)
);
```

### 网络扫描实现方式

采用轻量级 IP 扫描 + 端口检测的方式，不进行完整的设备发现。

**理由：**
- 完整的网络扫描可能被安全策略阻止
- 简单的端口检测足以发现大多数摄像头设备
- 避免对网络造成过大负载

**扫描流程：**
1. 用户输入网段（如 192.168.1.0/24）
2. 系统生成 IP 列表并分批扫描
3. 对每个 IP 尝试连接常见端口（554、80、8080、8554）
4. 连接成功后尝试获取设备信息（HTTP 响应头、ONVIF）
5. 返回发现结果

### 批量导入文件格式

使用 Excel (.xlsx) 作为主要导入格式，CSV 作为备选。

**理由：**
- Excel 更适合非技术用户，支持下拉选择和单元格验证
- 现有的 Java 库（Apache POI）支持良好
- CSV 作为备选，便于其他系统集成

### 边缘节点智能分配算法

在现有 `NodeWeightCalculator` 的基础上，增加地区匹配度作为评分因子。

**算法公式：**
```
基础得分 = CPU负载权重 * (1 - cpuUsage/maxCpuLoad)
         + 容量权重 * (1 - currentCameraCount/maxCameraSupport)
         + 带宽权重 * (availableBandwidth/totalBandwidth)

最终得分 = 基础得分 * (1 + 地区加分 * isSameRegion)
```

**理由：**
- 复用现有的权重计算逻辑，避免重复开发
- 地区加分作为可选的乘数因子，不影响其他评分因素
- 可通过配置调整地区匹配的优先级

### 前端组件结构

为每个新功能创建独立的页面组件，共享通用的工业风格 UI 组件。

**页面组件：**
- `ConfigTemplateManagement.jsx` - 配置模板管理
- `CameraDiscovery.jsx` - 网络发现
- `CameraBatchImport.jsx` - 批量导入
- 修改 `CameraManagement.jsx` - 增加快速添加入口

**理由：**
- 保持组件职责单一，便于维护
- 共享现有工业风格组件，保持 UI 一致性
- 独立路由，便于权限控制

### 异步任务处理

网络扫描和批量导入采用异步任务模式，通过 WebSocket 推送进度。

**理由：**
- 大规模操作耗时较长，需要防止请求超时
- 实时进度反馈提升用户体验
- 支持任务取消和重试

**任务状态机：**
```
PENDING → VALIDATING → IMPORTING → COMPLETED
   ↓         ↓            ↓
 FAILED ← FAILED ← FAILED
```

## Risks / Trade-offs

### 网络扫描可能被防火墙阻止

**风险：** 某些网络环境可能阻止 IP 扫描，导致功能不可用。

**缓解：**
- 提供手动输入 IP 的备选方式
- 扫描前提示用户检查网络权限
- 扫描失败时提供清晰的错误提示

### 模板匹配失败导致配置错误

**风险：** 自动匹配的模板可能不适用于特定设备，导致连接失败。

**缓解：**
- 提供模板测试功能，在保存前验证配置
- 模板匹配失败时标记为警告，不阻止导入
- 支持导入后批量修改配置

### 大规模导入性能问题

**风险：** 一次性导入大量摄像头可能导致数据库压力过大。

**缓解：**
- 实现批次处理（每批 50 条）
- 批次之间增加延迟（默认 100ms）
- 支持分页查询和进度显示

### 地区分配策略可能导致负载不均

**风险：** 过度优先同地区可能导致某些区域节点过载。

**缓解：**
- 地区加分可配置，默认 30%
- 节点负载超过阈值时自动跨地区分配
- 提供手动覆盖选项

## Migration Plan

### 部署步骤

1. **数据库迁移**
   - 创建 `camera_config_templates` 表
   - 创建 `camera_discovery_tasks` 表（可选，用于任务历史）
   - 插入预置品牌模板（海康、大华、宇视等）

2. **后端部署**
   - 部署新的 Service 和 Controller
   - 更新 `CameraService` 的自动分配逻辑
   - 配置 WebSocket 支持

3. **前端部署**
   - 部署新的页面组件
   - 更新路由配置
   - 更新 `CameraManagement` 组件

4. **配置更新**
   - 添加新的配置项（地区加分、CPU 阈值等）
   - 配置扫描并发数和超时时间

### 回滚策略

- 保留原有的 `autoAssignCamerasToEdgeNodes` 方法签名，只增强内部逻辑
- 新增的功能独立部署，不影响现有功能
- 如果出现问题，可通过配置禁用新功能

### 数据兼容性

- 新功能不影响现有数据
- `camera_config_templates` 为新表，无数据迁移需求
- 摄像头实体无需修改，保持向后兼容

## Open Questions

1. **预置模板的维护方式**
   - 问题：预置模板如何更新？是否需要在线更新机制？
   - 建议：初期通过版本升级更新，后续考虑在线模板库

2. **网络扫描的权限要求**
   - 问题：是否需要特殊的网络权限？
   - 建议：使用普通 TCP 连接，不需要特殊权限

3. **批量导入的并发控制**
   - 问题：是否需要限制同时执行的导入任务数？
   - 建议：初期不做限制，后续根据实际使用情况调整

4. **WebSocket 连接管理**
   - 问题：如何管理大量并发 WebSocket 连接？
   - 建议：使用 Spring WebSocket + STOMP，配合消息队列

5. **地区信息的来源**
   - 问题：批量导入时地区信息如何获取？
   - 建议：支持地区名称匹配，同时支持地区 ID
