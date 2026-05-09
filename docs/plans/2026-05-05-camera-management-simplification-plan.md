# 摄像头管理简化功能 - 详细实施计划

**创建日期**: 2026-05-05
**基准文档**: `docs/superpowers/specs/2026-05-05-camera-management-simplification-design.md`
**状态**: 待实施

---

## 实施概述

基于批准的设计文档，分 4 个功能模块 + 2 个增强点实施。预估总工时 12-14 天。

| 模块 | 工时 | 优先级 |
|------|------|--------|
| Phase 1: 数据模型与基础 | 1 天 | P0 |
| Phase 2: 配置模板系统 | 3 天 | P0 |
| Phase 3: 网络发现功能 | 3 天 | P0 |
| Phase 4: 批量导入功能 | 3 天 | P1 |
| Phase 5: 边缘节点分配增强 | 1 天 | P0 |
| Phase 6: 前端增强与联调 | 2 天 | P0 |

---

## 前置条件确认

设计文档中所述的基础设施大多**已存在**：

| 前提条件 | 状态 |
|----------|------|
| `NodeWeightCalculator.calculateWeightWithRegionBonus()` | ✅ 已实现 |
| WebSocket STOMP 配置 | ✅ 已存在 (broker: `/topic`, `/queue`) |
| `poi-ooxml` 依赖 | ✅ 已存在 (5.2.5) |
| `spring-boot-starter-websocket` | ✅ 已存在 |
| Camera 实体存在 `regionId` 字段 | ✅ 已存在 |
| `CameraService.selectOptimalEdgeNode()` | ✅ 已存在 |
| `CameraService.autoAssignCamerasToEdgeNodes()` | ✅ 已存在 |
| Ant Design 前端框架 | ✅ 已存在 |

---

## Phase 1: 数据模型与基础

### 1.1 新增 Entity 类

**后端 (aick-mmp-shared)** - 新增 3 个实体类：

#### 1.1.1 CameraConfigTemplate 实体

**文件**: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/CameraConfigTemplate.java`

```java
@Entity
@Table(name = "camera_config_templates")
public class CameraConfigTemplate {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 20)
    @Enumerated(STRING)
    private ProtocolType protocol;

    @Column(name = "default_port", nullable = false)
    private Integer defaultPort;

    @Column(name = "url_path_template", nullable = false, length = 500)
    private String urlPathTemplate;

    @Column(columnDefinition = "JSON")
    private String presetParameters;

    @Column(name = "is_preset")
    private Boolean isPreset = false;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### 1.1.2 CameraDiscoveryTask 实体

**文件**: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/CameraDiscoveryTask.java`

字段：`id`, `userId`, `networkSegment`, `status` (PENDING/RUNNING/COMPLETED/FAILED/CANCELLED), `progress`, `totalIps`, `foundDevices` (JSON), `startedAt`, `completedAt`, `createdAt`

#### 1.1.3 CameraBatchImportTask 实体

**文件**: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/CameraBatchImportTask.java`

字段：`id`, `userId`, `fileName`, `status` (PENDING/VALIDATING/IMPORTING/COMPLETED/FAILED/CANCELLED), `totalRecords`, `successCount`, `failCount`, `errorDetails` (JSON), `startedAt`, `completedAt`, `createdAt`

### 1.2 新增 DTO 类

**后端 (aick-mmp-central)**:

| DTO | 文件路径 | 用途 |
|-----|----------|------|
| `CameraConfigTemplateDTO` | `central/dto/CameraConfigTemplateDTO.java` | 模板数据传输 |
| `CreateTemplateRequestDTO` | `central/dto/CreateTemplateRequestDTO.java` | 创建模板请求 |
| `UpdateTemplateRequestDTO` | `central/dto/UpdateTemplateRequestDTO.java` | 更新模板请求 |
| `DiscoveryTaskDTO` | `central/dto/DiscoveryTaskDTO.java` | 扫描任务 DTO |
| `ScanProgressDTO` | `central/dto/ScanProgressDTO.java` | 扫描进度 DTO |
| `ConnectivityResultDTO` | `central/dto/ConnectivityResultDTO.java` | 连通性测试结果 |
| `DeviceIdentifyDTO` | `central/dto/DeviceIdentifyDTO.java` | 设备识别结果 |
| `ImportTaskDTO` | `central/dto/ImportTaskDTO.java` | 导入任务 DTO |
| `ImportProgressDTO` | `central/dto/ImportProgressDTO.java` | 导入进度 DTO |
| `CameraImportDTO` | `central/dto/CameraImportDTO.java` | 导入记录 DTO |
| `ValidationErrorDTO` | `central/dto/ValidationErrorDTO.java` | 验证错误 DTO |

### 1.3 新增 Repository 接口

**后端 (aick-mmp-central)**:

| Repository | 文件路径 | 关键方法 |
|------------|----------|----------|
| `CameraConfigTemplateRepository` | `central/repository/CameraConfigTemplateRepository.java` | `findByBrand()`, `findByProtocol()`, `findByBrandAndModel()`, `findByIsPresetAndIsDeletedFalse()` |
| `CameraDiscoveryTaskRepository` | `central/repository/CameraDiscoveryTaskRepository.java` | `findByUserIdOrderByCreatedAtDesc()`, `findByStatus()` |
| `CameraBatchImportTaskRepository` | `central/repository/CameraBatchImportTaskRepository.java` | `findByUserIdOrderByCreatedAtDesc()`, `findByStatus()` |

### 1.4 预置模板数据

**文件**: `backend/aick-mmp-central/src/main/resources/templates/preset-cameras.json`

包含主流品牌模板（约 15-20 条）：

```json
[
  {
    "brand": "海康威视",
    "model": "DS-2CD2T45D-I5",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/Streaming/Channels/{channel}01",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "海康威视",
    "model": "DS-2CD2146G2-I",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/Streaming/Channels/{channel}01",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "大华",
    "model": "DH-IPC-HFW2431T-ZS",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/cam/realmonitor?channel={channel}&subtype=0",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "大华",
    "model": "DH-IPC-HDBW2431R-ZS",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/cam/realmonitor?channel={channel}&subtype=0",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "宇视",
    "model": "IPC-B2241-I",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/Streaming/channels/{channel}01",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "华为",
    "model": "C2120-I-P",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/live/{channel}",
    "presetParameters": "{\"channel\":\"1\"}",
    "isPreset": true
  },
  {
    "brand": "TP-LINK",
    "model": "TL-IPC546HP",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}/stream1",
    "presetParameters": "{}",
    "isPreset": true
  },
  {
    "brand": "General",
    "model": "RTSP Camera",
    "protocol": "RTSP",
    "defaultPort": 554,
    "urlPathTemplate": "rtsp://{username}:{password}@{ip}:{port}",
    "presetParameters": "{}",
    "isPreset": true
  },
  {
    "brand": "General",
    "model": "ONVIF Camera",
    "protocol": "ONVIF",
    "defaultPort": 80,
    "urlPathTemplate": "http://{ip}:{port}/onvif/device_service",
    "presetParameters": "{}",
    "isPreset": true
  },
  {
    "brand": "General",
    "model": "HTTP Camera",
    "protocol": "HTTP",
    "defaultPort": 80,
    "urlPathTemplate": "http://{username}:{password}@{ip}:{port}/video",
    "presetParameters": "{}",
    "isPreset": true
  }
]
```

#### DataInitializerConfig 更新

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/DataInitializerConfig.java`

添加 `CommandLineRunner` bean，启动时检查 `camera_config_templates` 表是否为空，为空则加载 `preset-cameras.json` 中的预置模板。

---

## Phase 2: 配置模板系统

### 2.1 Service 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraConfigTemplateService.java` (接口)
**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraConfigTemplateServiceImpl.java` (实现)

**核心方法**:
- `createTemplate(CreateTemplateRequestDTO)` — 创建模板
- `getTemplates(Pageable, brand, protocol)` — 分页查询模板列表
- `getTemplateById(Long)` — 获取模板详情
- `updateTemplate(Long, UpdateTemplateRequestDTO)` — 更新模板
- `deleteTemplate(Long)` — 软删除模板
- `generateUrl(Long, Map<String,String>)` — 根据模板和参数生成连接 URL
- `matchTemplate(String brand, String model)` — 根据品牌/型号匹配模板（先精确匹配，再模糊匹配）
- `incrementUsage(Long)` — 记录模板使用次数
- `importTemplates(List<TemplateDTO>)` — 批量导入模板
- `exportTemplates(List<Long>)` — 导出模板为列表
- `getPresetTemplates()` — 获取预置模板
- `getBrands()` — 获取品牌列表（用于前端下拉）

**generateUrl 实现逻辑**:
```java
public String generateUrl(Long templateId, Map<String, String> params) {
    CameraConfigTemplate template = repository.findById(templateId)
        .orElseThrow(() -> new NotFoundException("Template not found"));
    
    String url = template.getUrlPathTemplate();
    
    // 替换所有变量 {key} → 实际值
    for (Map.Entry<String, String> entry : params.entrySet()) {
        url = url.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
    }
    
    // 使用默认端口（如果参数中未提供）
    if (!params.containsKey("port")) {
        url = url.replace("{port}", String.valueOf(template.getDefaultPort()));
    }
    
    return url;
}
```

### 2.2 Controller 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraConfigTemplateController.java`

| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `GET /api/camera-config-templates` | `getTemplates()` | ADMIN/OPERATOR | 分页查询模板 |
| `GET /api/camera-config-templates/{id}` | `getTemplateById()` | ADMIN/OPERATOR | 获取模板详情 |
| `POST /api/camera-config-templates` | `createTemplate()` | ADMIN | 创建模板 |
| `PUT /api/camera-config-templates/{id}` | `updateTemplate()` | ADMIN | 更新模板 |
| `DELETE /api/camera-config-templates/{id}` | `deleteTemplate()` | ADMIN | 删除模板 |
| `POST /api/camera-config-templates/{id}/generate-url` | `generateUrl()` | ADMIN/OPERATOR | 生成连接 URL |
| `POST /api/camera-config-templates/match` | `matchTemplate()` | ADMIN/OPERATOR | 匹配模板 |
| `POST /api/camera-config-templates/import` | `importTemplates()` | ADMIN | 导入模板 |
| `GET /api/camera-config-templates/export` | `exportTemplates()` | ADMIN | 导出模板 |
| `GET /api/camera-config-templates/brands` | `getBrands()` | ADMIN/OPERATOR | 获取品牌列表 |

### 2.3 前端页面

**文件**: `frontend/src/pages/ConfigTemplateManagement.js`

- Ant Design `ProTable` 展示模板列表
- 搜索条件：品牌、协议
- 操作列：编辑、删除
- 添加/编辑 Modal 表单（品牌、型号、协议、默认端口、URL 模板、预设参数）
- URL 模板变量提示信息（`{ip}`, `{port}`, `{username}`, `{password}`, `{channel}`）
- 实时 URL 预览：输入参数后即时生成示例 URL
- 模板导入/导出按钮

### 2.4 路由配置

**文件**: `frontend/src/App.js`

新增路由：
```jsx
<Route path="/cameras/templates" element={<ConfigTemplateManagement />} />
```

### 2.5 API 层

**文件**: `frontend/src/utils/api.js`

新增 `cameraConfigTemplateApi` 模块：
```javascript
export const cameraConfigTemplateApi = {
    getTemplates: (params) => http.get('/api/camera-config-templates', { params }),
    getTemplateById: (id) => http.get(`/api/camera-config-templates/${id}`),
    createTemplate: (data) => http.post('/api/camera-config-templates', data),
    updateTemplate: (id, data) => http.put(`/api/camera-config-templates/${id}`, data),
    deleteTemplate: (id) => http.delete(`/api/camera-config-templates/${id}`),
    generateUrl: (id, params) => http.post(`/api/camera-config-templates/${id}/generate-url`, params),
    matchTemplate: (brand, model) => http.post('/api/camera-config-templates/match', { brand, model }),
    importTemplates: (data) => http.post('/api/camera-config-templates/import', data),
    exportTemplates: (ids) => http.get('/api/camera-config-templates/export', { params: { ids } }),
    getBrands: () => http.get('/api/camera-config-templates/brands'),
};
```

---

## Phase 3: 网络发现功能

### 3.1 Service 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraDiscoveryService.java` (接口)
**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraDiscoveryServiceImpl.java` (实现)

**核心方法**:
- `startScan(String networkSegment, Long userId)` → 异步执行扫描，返回 taskId
- `getScanProgress(Long taskId)` → 获取扫描进度
- `cancelScan(Long taskId)` → 取消扫描
- `testConnectivity(String ip, Integer port)` → 测试单个 IP 连通性
- `identifyDevice(String ip, Integer port)` → 识别设备品牌/型号
- `getScanHistory(Pageable, Long userId)` → 获取扫描历史

**扫描实现逻辑**:
```java
@Async
public void executeScan(Long taskId) {
    CameraDiscoveryTask task = repository.findById(taskId).get();
    task.setStatus(ScanStatus.RUNNING);
    task.setStartedAt(LocalDateTime.now());
    repository.save(task);
    
    List<String> ips = generateIpList(task.getNetworkSegment());
    task.setTotalIps(ips.size());
    repository.save(task);
    
    List<DiscoveredDevice> devices = new ArrayList<>();
    int scanned = 0;
    
    for (List<String> batch : Lists.partition(ips, scanBatchSize)) {
        if (task.getStatus() == ScanStatus.CANCELLED) break;
        
        for (String ip : batch) {
            for (int port : commonPorts) {
                ConnectivityResult result = tryConnect(ip, port);
                if (result.isConnected()) {
                    DeviceInfo info = identifyDevice(ip, port);
                    devices.add(new DiscoveredDevice(ip, port, info.getBrand(), info.getModel()));
                }
            }
            scanned++;
        }
        
        // 更新进度
        task.setProgress(scanned * 100 / ips.size());
        task.setFoundDevices(toJson(devices));
        repository.save(task);
        
        // WebSocket 推送进度
        websocketChannel.sendToTopic(
            String.format("/topic/discovery/%d", taskId),
            buildProgressMessage(task)
        );
        
        // 批次间延迟
        Thread.sleep(100);
    }
    
    task.setStatus(ScanStatus.COMPLETED);
    task.setCompletedAt(LocalDateTime.now());
    repository.save(task);
}
```

**品牌识别逻辑**:
- RTSP 端口 (554): 尝试 RTSP DESCRIBE 请求，解析响应中的 Server 字段
- HTTP 端口 (80, 8080): 发送 HTTP GET 请求，解析响应头 `Server` 字段
  - `Hikvision-Webs` → 海康威视
  - `Dahua` → 大华
  - `Uniview` → 宇视
  - 其他 → 未知
- ONVIF 端口: 尝试 ONVIF 探测
- 无法识别 → 标记为"未知品牌"

### 3.2 Controller 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraDiscoveryController.java`

| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `POST /api/camera-discovery/scan` | `startScan()` | ADMIN/OPERATOR | 启动网段扫描 |
| `GET /api/camera-discovery/scan/{taskId}/progress` | `getScanProgress()` | ADMIN/OPERATOR | 获取扫描进度 |
| `DELETE /api/camera-discovery/scan/{taskId}` | `cancelScan()` | ADMIN/OPERATOR | 取消扫描 |
| `POST /api/camera-discovery/test-connectivity` | `testConnectivity()` | ADMIN/OPERATOR | 测试连通性 |
| `POST /api/camera-discovery/identify` | `identifyDevice()` | ADMIN/OPERATOR | 识别设备 |
| `GET /api/camera-discovery/history` | `getScanHistory()` | ADMIN/OPERATOR | 扫描历史 |

### 3.3 前端页面

**文件**: `frontend/src/pages/CameraDiscovery.js`

- 扫描任务表单：网段输入 (CIDR 格式，如 `192.168.1.0/24`)
- 扫描控制按钮：开始扫描、取消扫描
- 扫描进度展示：进度条、已扫描 IP 数 / 总 IP 数、已发现设备数
- 发现的设备列表（表格）：IP、端口、识别品牌、推荐模板、操作（一键添加）
- 一键添加 Modal：选择已发现的设备，配置摄像头名称、选择区域、选择边缘节点（自动推荐），点击添加
- 扫描历史记录：表格展示过往扫描任务
- WebSocket 实时订阅 `/topic/discovery/{taskId}` 更新进度

### 3.4 WebSocket 集成

**后端**: 在 `CameraDiscoveryServiceImpl` 中注入 `SimpMessagingTemplate`，扫描过程中推送进度消息到 `/topic/discovery/{taskId}`。

**前端**: 使用 `@microsoft/signalr` 或原生 STOMP 客户端连接到 WebSocket，订阅扫描进度主题。也可以使用已有的 `socket.io-client`。

**注意**: 当前 `WebSocketConfig` 配置了 `/ws/alerts` 和 `/ws/stream` 端点。可以为发现功能在 `WebSocketConfig` 中注册新端点 `/ws/discovery`，或复用现有 STOMP broker 配置通过 `/topic/discovery/{taskId}` 推送。

实际上，由于 STOMP broker 已经配置了 `/topic` 前缀，可以直接用 `SimpMessagingTemplate.convertAndSend("/topic/discovery/" + taskId, payload)` 推送，前端连接现有端点即可。

### 3.5 路由配置

**文件**: `frontend/src/App.js`

新增路由：
```jsx
<Route path="/cameras/discovery" element={<CameraDiscovery />} />
```

### 3.6 API 层

**文件**: `frontend/src/utils/api.js`

新增 `cameraDiscoveryApi` 模块

---

## Phase 4: 批量导入功能

### 4.1 Service 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraBatchImportService.java` (接口)
**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraBatchImportServiceImpl.java` (实现)

**核心方法**:
- `getImportTemplate()` → 生成 Excel 导入模板（含列头和示例数据）
- `startImport(MultipartFile file, Long userId)` → 异步执行批量导入
- `getImportProgress(Long taskId)` → 获取导入进度
- `cancelImport(Long taskId)` → 取消导入
- `downloadErrorReport(Long taskId)` → 下载错误报告 Excel
- `getImportHistory(Pageable, Long userId)` → 获取导入历史
- `validateImportData(List<CameraImportDTO>)` → 验证导入数据（批量）

**导入流程**:
```
1. 解析文件 (.xlsx / .csv)
2. 逐行验证必填字段 (摄像头名称、IP、品牌/型号)
3. 匹配配置模板 (品牌 + 型号 → CameraConfigTemplate)
4. 自动分配边缘节点 (CameraService.selectOptimalEdgeNode() + region bonus)
5. 分批创建摄像头记录 (每批 50 条)
6. WebSocket 实时推送进度
7. 完成后生成错误报告 (含失败记录和错误原因)
```

**Excel 导入模板列**:
| 列名 | 必填 | 说明 |
|------|------|------|
| 摄像头名称 | ✅ | 唯一，2-50 字符 |
| 品牌 | ✅ | 用于匹配模板 |
| 型号 | ✅ | 用于匹配模板 |
| IP 地址 | ✅ | IPv4 格式 |
| 端口 | 可选 | 默认使用模板端口 |
| 所属区域 | ✅ | 区域名称，需与系统中区域名称匹配 |
| 用户名 | 可选 | 访问摄像头的用户名 |
| 密码 | 可选 | 访问摄像头的密码 |
| 分辨率 | 可选 | 默认 1920x1080 |
| 描述 | 可选 | |

### 4.2 Controller 层

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraBatchImportController.java`

| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `GET /api/camera-batch-import/template` | `downloadTemplate()` | ADMIN/OPERATOR | 下载导入模板 |
| `POST /api/camera-batch-import/import` | `startImport()` | ADMIN | 上传并导入 |
| `GET /api/camera-batch-import/{taskId}/progress` | `getImportProgress()` | ADMIN/OPERATOR | 获取导入进度 |
| `DELETE /api/camera-batch-import/{taskId}` | `cancelImport()` | ADMIN | 取消导入 |
| `GET /api/camera-batch-import/{taskId}/errors` | `downloadErrorReport()` | ADMIN/OPERATOR | 下载错误报告 |
| `GET /api/camera-batch-import/history` | `getImportHistory()` | ADMIN/OPERATOR | 导入历史 |

### 4.3 前端页面

**文件**: `frontend/src/pages/CameraBatchImport.js`

- 导入模板下载按钮
- 文件上传组件 (Ant Design `Upload`)，支持拖拽
  - 限制：仅 `.xlsx` 和 `.csv`，最大 10MB
  - 文件类型验证
- 导入进度展示：
  - 进度条 (总进度)
  - 成功数 / 失败数
  - 当前处理记录名
  - 取消导入按钮
- 导入结果报告：
  - 成功后显示摘要（总记录数、成功数、失败数）
  - 失败记录可下载错误报告
- 导入任务历史列表（表格）

### 4.4 路由配置

**文件**: `frontend/src/App.js`

新增路由：
```jsx
<Route path="/cameras/batch-import" element={<CameraBatchImport />} />
```

### 4.5 API 层

**文件**: `frontend/src/utils/api.js`

新增 `cameraBatchImportApi` 模块

---

## Phase 5: 边缘节点分配增强

### 5.1 CameraService 更新

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java`

修改 `selectOptimalEdgeNode(CameraDTO cameraDTO)` 方法：

**原有逻辑** (简化)：
```java
public Long selectOptimalEdgeNode(CameraDTO cameraDTO) {
    List<EdgeNode> nodes = edgeNodeRepository.findByStatus(EdgeNodeStatus.ONLINE);
    // 使用 NodeWeightCalculator.calculateWeight()
    EdgeNode best = nodes.stream()
        .max(Comparator.comparingDouble(n -> weightCalculator.calculateWeight(n, n.getCpuUsage(), n.getMemoryUsage())))
        .orElse(null);
    return best != null ? best.getId() : null;
}
```

**新逻辑**：
```java
public Long selectOptimalEdgeNode(CameraDTO cameraDTO) {
    List<EdgeNode> nodes = edgeNodeRepository.findByStatus(EdgeNodeStatus.ONLINE);
    
    // 获取摄像头所属区域 ID
    Long cameraRegionId = cameraDTO.getRegionId();
    
    // 配置的地区加成比例（从配置中读取）
    double regionBonusRate = edgeNodeProperties.getRegionBonusRate(); // 默认 0.3
    
    EdgeNode best = nodes.stream()
        .max(Comparator.comparingDouble(n -> 
            weightCalculator.calculateWeightWithRegionBonus(
                n, n.getCpuUsage(), n.getMemoryUsage(), 
                cameraRegionId, regionBonusRate
            )
        ))
        .orElse(null);
    
    if (best != null) {
        incrementUsage(best);
        return best.getId();
    }
    
    // 没有在线节点，抛出异常或返回 null
    throw new ServiceException("没有可用的边缘节点");
}
```

### 5.2 配置项

**文件**: `backend/aick-mmp-central/src/main/resources/application.yml`

添加配置：
```yaml
edge-node:
  region-bonus-rate: 0.3
```

### 5.3 Configuration Properties 类

**文件**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/properties/EdgeNodeProperties.java` (新建)

```java
@ConfigurationProperties(prefix = "edge-node")
public class EdgeNodeProperties {
    private double regionBonusRate = 0.3;
    private double cpuThreshold = 80.0;
    private double memoryThreshold = 85.0;
    // getters & setters
}
```

在主类或配置类上添加 `@EnableConfigurationProperties(EdgeNodeProperties.class)`。

---

## Phase 6: 前端增强与联调

### 6.1 侧边栏菜单更新

**文件**: `frontend/src/components/Layout.js` (或对应的布局组件)

在"摄像头管理"菜单项下添加子菜单：
```
摄像头管理
├── 摄像头列表      → /cameras
├── 配置模板        → /cameras/templates
├── 网络发现        → /cameras/discovery
└── 批量导入        → /cameras/batch-import
```

### 6.2 CameraManagement 组件增强

**文件**: `frontend/src/pages/CameraManagement.js`

增强点：
1. **顶部工具栏** 增加快捷按钮：
   - "网络发现" 按钮 → 跳转到 `/cameras/discovery`
   - "批量导入" 按钮 → 跳转到 `/cameras/batch-import`
   - "配置模板" 按钮 → 跳转到 `/cameras/templates`

2. **添加/编辑摄像头 Modal** 增加"配置模板"下拉选择器：
   - 选择品牌后自动过滤型号
   - 选择型号后自动填充协议、默认端口、URL 模板
   - URL 生成预览（根据 IP、端口等输入实时生成完整 URL）
   - 匹配成功后显示"推荐边缘节点"标签

3. **添加摄像头表单**在提交时调用 `selectOptimalEdgeNode()`：
   - 如果用户未手动选择边缘节点，自动推荐最优节点
   - 推荐节点高亮显示并有"推荐"标签

### 6.3 API 层更新

**文件**: `frontend/src/utils/api.js`

汇总所有新增 API 模块：
- `cameraConfigTemplateApi`
- `cameraDiscoveryApi`
- `cameraBatchImportApi`

---

## 实施顺序与依赖

```
Phase 1: 数据模型
  ├── 1.1 实体类 (shared)
  ├── 1.2 DTO 类 (central)
  ├── 1.3 Repository (central)
  ├── 1.4 数据库迁移（启动时自动 DDL）
  └── 1.5 预置模板数据
       │
       ▼
Phase 2: 配置模板 ──────────────────────────┐
  ├── 2.1 Service 层                        │
  ├── 2.2 Controller 层                     │
  ├── 2.3 前端页面                           │
  └── 2.4 路由 + API 层                     │
       │                                    │
       ▼                                    ▼
Phase 3: 网络发现                   Phase 5: 节点分配增强
  ├── 3.1 Service 层                  ├── 5.1 CameraService 更新
  ├── 3.2 Controller 层               ├── 5.2 配置项
  ├── 3.3 前端页面                     └── 5.3 Properties 类
  ├── 3.4 WebSocket 集成                   │
  └── 3.5 路由 + API 层                    │
       │                                    │
       ▼                                    ▼
Phase 4: 批量导入                   Phase 6: 前端增强
  ├── 4.1 Service 层                  ├── 6.1 菜单更新
  ├── 4.2 Controller 层               ├── 6.2 CameraManagement 增强
  ├── 4.3 前端页面                     └── 6.3 API 层更新
  ├── 4.4 WebSocket 集成
  └── 4.5 路由 + API 层
```

**推荐实施顺序**: Phase 1 → Phase 2 + Phase 5 (并行) → Phase 3 → Phase 4 → Phase 6

---

## 完整文件变更清单

### 新增文件 (后端 20 个)

| # | 文件路径 | 所属 Phase |
|---|----------|-----------|
| 1 | `aick-mmp-shared/.../model/CameraConfigTemplate.java` | 1.1 |
| 2 | `aick-mmp-shared/.../model/CameraDiscoveryTask.java` | 1.1 |
| 3 | `aick-mmp-shared/.../model/CameraBatchImportTask.java` | 1.1 |
| 4 | `aick-mmp-central/.../dto/CameraConfigTemplateDTO.java` | 1.2 |
| 5 | `aick-mmp-central/.../dto/CreateTemplateRequestDTO.java` | 1.2 |
| 6 | `aick-mmp-central/.../dto/UpdateTemplateRequestDTO.java` | 1.2 |
| 7 | `aick-mmp-central/.../dto/DiscoveryTaskDTO.java` | 1.2 |
| 8 | `aick-mmp-central/.../dto/ScanProgressDTO.java` | 1.2 |
| 9 | `aick-mmp-central/.../dto/ConnectivityResultDTO.java` | 1.2 |
| 10 | `aick-mmp-central/.../dto/DeviceIdentifyDTO.java` | 1.2 |
| 11 | `aick-mmp-central/.../dto/ImportProgressDTO.java` | 1.2 |
| 12 | `aick-mmp-central/.../dto/ImportTaskDTO.java` | 1.2 |
| 13 | `aick-mmp-central/.../dto/CameraImportDTO.java` | 1.2 |
| 14 | `aick-mmp-central/.../dto/ValidationErrorDTO.java` | 1.2 |
| 15 | `aick-mmp-central/.../repository/CameraConfigTemplateRepository.java` | 1.3 |
| 16 | `aick-mmp-central/.../repository/CameraDiscoveryTaskRepository.java` | 1.3 |
| 17 | `aick-mmp-central/.../repository/CameraBatchImportTaskRepository.java` | 1.3 |
| 18 | `aick-mmp-central/.../service/CameraConfigTemplateService.java` | 2.1 |
| 19 | `aick-mmp-central/.../service/impl/CameraConfigTemplateServiceImpl.java` | 2.1 |
| 20 | `aick-mmp-central/.../controller/CameraConfigTemplateController.java` | 2.2 |
| 21 | `aick-mmp-central/.../service/CameraDiscoveryService.java` | 3.1 |
| 22 | `aick-mmp-central/.../service/impl/CameraDiscoveryServiceImpl.java` | 3.1 |
| 23 | `aick-mmp-central/.../controller/CameraDiscoveryController.java` | 3.2 |
| 24 | `aick-mmp-central/.../service/CameraBatchImportService.java` | 4.1 |
| 25 | `aick-mmp-central/.../service/impl/CameraBatchImportServiceImpl.java` | 4.1 |
| 26 | `aick-mmp-central/.../controller/CameraBatchImportController.java` | 4.2 |
| 27 | `aick-mmp-central/.../config/properties/EdgeNodeProperties.java` | 5.3 |
| 28 | `aick-mmp-central/src/main/resources/templates/preset-cameras.json` | 1.4 |

### 新增文件 (前端 5 个)

| # | 文件路径 | 所属 Phase |
|---|----------|-----------|
| 1 | `frontend/src/pages/ConfigTemplateManagement.js` | 2.3 |
| 2 | `frontend/src/pages/CameraDiscovery.js` | 3.3 |
| 3 | `frontend/src/pages/CameraBatchImport.js` | 4.3 |
| 4 | `frontend/src/components/CameraTemplateSelector.js` | 6.2 |
| 5 | `frontend/src/components/DiscoveryDeviceList.js` | 3.3 |

### 修改文件 (后端 5 个)

| # | 文件路径 | 变更说明 | 所属 Phase |
|---|----------|----------|-----------|
| 1 | `CameraServiceImpl.java` | `selectOptimalEdgeNode()` 改为使用 `calculateWeightWithRegionBonus()` | 5.1 |
| 2 | `DataInitializerConfig.java` | 添加 CommandLineRunner 加载预置模板 | 1.5 |
| 3 | `application.yml` | 添加 `edge-node.region-bonus-rate` 等配置 | 5.2 |
| 4 | `CentralApplication.java` | 添加 `@EnableConfigurationProperties(EdgeNodeProperties.class)` | 5.3 |
| 5 | `pom.xml` (aick-mmp-parent) | 无需修改（依赖已存在） | — |

### 修改文件 (前端 3 个)

| # | 文件路径 | 变更说明 | 所属 Phase |
|---|----------|----------|-----------|
| 1 | `App.js` | 添加 3 个新路由 + 侧边栏菜单更新 | 2.4 / 3.5 / 4.5 / 6.1 |
| 2 | `CameraManagement.js` | 添加快捷按钮 + 模板选择器集成 | 6.2 |
| 3 | `utils/api.js` | 添加 3 个新 API 模块 | 2.5 / 3.6 / 4.5 / 6.3 |

---

## 测试策略

### 单元测试

| 测试类 | 测试内容 | 文件路径 |
|--------|----------|----------|
| `CameraConfigTemplateServiceTest` | CRUD、URL 生成、品牌匹配 | `central/src/test/.../service/` |
| `CameraDiscoveryServiceTest` | IP 生成、设备识别、扫描状态流转 | 同上 |
| `CameraBatchImportServiceTest` | 文件解析、验证逻辑、批量创建 | 同上 |
| `NodeWeightCalculatorTest` (增强) | 地区加成计算 | 已存在，补充测试用例 |

### 集成测试

| 测试类 | 测试内容 |
|--------|----------|
| `CameraConfigTemplateIntegrationTest` | 模板创建 → URL 生成 → 使用 |
| `CameraDiscoveryIntegrationTest` | 扫描任务创建 → 进度查询 → 取消 |
| `CameraBatchImportIntegrationTest` | 模板下载 → 文件上传 → 导入完成 |

### 前端测试（可选）

| 测试文件 | 测试内容 |
|----------|----------|
| `ConfigTemplateManagement.test.js` | 页面渲染、表单交互、URL 预览 |
| `CameraDiscovery.test.js` | 扫描流程、设备列表渲染 |
| `CameraBatchImport.test.js` | 文件上传、进度展示 |

---

## 配置项汇总

```yaml
# 统一添加到 application.yml
camera-config-templates:
  preset-templates-path: "classpath:templates/preset-cameras.json"

camera-discovery:
  scan-batch-size: 50
  scan-timeout-ms: 2000
  common-ports: [554, 80, 8080, 8554]

camera-batch-import:
  batch-size: 50
  batch-delay-ms: 100
  max-file-size-mb: 10

edge-node:
  region-bonus-rate: 0.3
  cpu-threshold: 80.0
  memory-threshold: 85.0
```

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 网络扫描触发安全策略 | 扫描功能不可用 | 提供手动输入 IP 备选；扫描前权限检查提示 |
| 模板匹配失败导致 URL 错误 | 摄像头无法连接 | 提供 URL 预览和测试功能；匹配失败时警告但不阻塞 |
| 大规模导入 DB 压力大 | 导入慢或超时 | 分批处理（50 条/批）+ 批次间延迟 + 异步 |
| 地区调度导致负载不均 | 部分节点过载 | 地区加成可配置；负载超阈值时自动跨地区 |
