## ADDED Requirements

### Requirement: 集中式车牌识别

AI Service SHALL 提供基于深度学习的车牌识别能力，在中央GPU服务器统一处理。

#### Scenario: 车牌检测与识别
- **WHEN** AI Service 收到视频帧且配置了车牌识别时
- **THEN** 系统 SHALL 使用YOLOv8-plate检测车牌区域
- **AND** 使用LPRNet识别车牌号码
- **AND** 返回车牌号、置信度、车牌颜色

#### Scenario: 识别结果发布
- **WHEN** 车牌识别完成时
- **THEN** 系统 SHALL 通过 gRPC 返回识别结果
- **AND** 异步发布结果到 Kafka Topic `ai-vehicle-records`
- **AND** 包含 plateNumber、cameraId、timestamp、confidence

---

### Requirement: 白名单管理

系统 SHALL 支持车牌白名单管理，允许授权车辆通行。

#### Scenario: 白名单车辆识别
- **WHEN** 识别到的车牌在白名单中时
- **THEN** 系统 SHALL 标记识别结果为白名单车辆
- **AND** 包含白名单类型（长期/临时）
- **AND** 包含过期时间（如果有）

#### Scenario: 黑名单车辆告警
- **WHEN** 识别到的车牌在黑名单中时
- **THEN** 系统 SHALL 触发告警
- **AND** 告警类型为BLACKLIST_VEHICLE
- **AND** 严重级别为CRITICAL

#### Scenario: 白名单CRUD
- **WHEN** 管理员管理白名单时
- **THEN** 系统 SHALL 支持以下操作：
  - `POST /api/v1/ai/vehicles/whitelist`：添加白名单
  - `PUT /api/v1/ai/vehicles/whitelist/{id}`：更新白名单
  - `DELETE /api/v1/ai/vehicles/whitelist/{id}`：删除白名单
  - `GET /api/v1/ai/vehicles/whitelist`：查询白名单列表

---

### Requirement: 车牌记录存储与查询

系统 SHALL 存储所有车牌识别记录，支持查询和统计。

#### Scenario: 创建识别记录
- **WHEN** 车牌识别完成时
- **THEN** 系统 SHALL 创建识别记录
- **AND** 包含plateNumber、cameraId、timestamp、plateColor、confidence
- **AND** 包含 isWhitelisted 标记

#### Scenario: 查询识别记录
- **WHEN** 调用 `GET /api/v1/ai/vehicles/records` 时
- **THEN** 系统 SHALL 支持以下查询参数：
  - `cameraId`：按摄像头筛选
  - `plateNumber`：按车牌号查询
  - `page/size`：分页查询

#### Scenario: 车辆统计分析
- **WHEN** 调用 `GET /api/v1/ai/vehicles/stats` 时
- **THEN** 系统 SHALL 返回统计信息
- **AND** 包含日/周/月车流量
- **AND** 包含车辆类型分布

---

### Requirement: 帧传输与处理

边缘节点 SHALL 负责帧提取与转发，不进行本地AI推理。

#### Scenario: 帧提取与推流
- **WHEN** 边缘节点从RTSP流获取到视频帧时
- **THEN** 系统 SHALL 通过 gRPC 流式传输帧数据到 AI Service
- **AND** 包含 cameraId、timestamp、frameData(bytes)
- **AND** 按配置的帧率发送（默认客流1fps、车牌5fps）

#### Scenario: 配置驱动分析
- **WHEN** 边缘节点启动时
- **THEN** 系统 SHALL 根据 `AiServiceConfig` 加载摄像头分析配置
- **AND** 为每路摄像头创建独立的 gRPC 流
- **AND** 按 analysisTypes 配置发送分析类型标记

#### Scenario: 资源监控
- **WHEN** 边缘节点运行时
- **THEN** 系统 SHALL 监控CPU、内存使用率
- **AND** 当资源紧张时降低帧率
