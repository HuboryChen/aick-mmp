## ADDED Requirements

### Requirement: AI微服务基础架构

AI微服务 SHALL 提供独立的Python服务，负责所有AI推理任务，通过 gRPC 接收帧数据、通过Kafka输出分析结果。

#### Scenario: 服务启动
- **WHEN** AI微服务启动时
- **THEN** 系统 SHALL 加载所有预训练模型到内存
- **AND** 建立与Kafka broker的连接
- **AND** 启动gRPC服务器和FastAPI HTTP服务

#### Scenario: 健康检查
- **WHEN** 调用 `GET /health` 接口时
- **THEN** 系统 SHALL 返回服务健康状态
- **AND** 包含模型加载状态
- **AND** 包含GPU可用状态
- **AND** 包含Kafka连接状态

#### Scenario: GPU资源检测
- **WHEN** 系统启动时
- **THEN** 系统 SHALL 检测GPU可用性
- **AND** 如果GPU可用则使用CUDA加速推理
- **AND** 如果GPU不可用则回退到CPU推理
- **AND** 在日志中记录设备信息

---

### Requirement: gRPC流式帧分析接口

AI微服务 SHALL 通过 gRPC 双向流提供帧分析接口，边缘节点按摄像头建立独立流连接。

#### Scenario: 流式帧分析
- **WHEN** 边缘节点建立 gRPC 流连接并发送帧数据时
- **THEN** 系统 SHALL 接收流式FrameRequest（包含cameraId、frameData、timestamp、analysisTypes）
- **AND** 按 analysisTypes 执行对应分析（passenger/behavior/plate）
- **AND** 实时返回AnalysisResult流

#### Scenario: 帧处理编排
- **WHEN** 收到帧时
- **THEN** 系统 SHALL 根据 analysisTypes 执行对应分析管道：
  - passenger：detector → tracker → passenger_analyzer → kafka
  - behavior：detector → tracker → behavior_engine → kafka
  - plate：plate_recognizer → kafka
- **AND** 按FPS配置控制各类型分析频率

---

### Requirement: 模型管理

AI微服务 SHALL 提供基础的模型管理能力。

#### Scenario: 模型列表查询
- **WHEN** 调用 `GET /api/v1/models` 时
- **THEN** 系统 SHALL 返回 `models/` 目录下的可用ONNX模型列表

---

### Requirement: 推理引擎管理

AI微服务 SHALL 管理多个AI模型的推理引擎。

#### Scenario: 模型加载
- **WHEN** 系统启动时
- **THEN** 系统 SHALL 从指定路径加载ONNX模型
- **AND** 创建ONNX Runtime推理会话
- **AND** 配置执行providers（GPU优先）
- **AND** 记录加载状态和耗时

#### Scenario: 推理执行
- **WHEN** 收到分析请求时
- **THEN** 系统 SHALL 预处理输入数据（缩放、归一化）
- **AND** 调用对应模型的推理会话
- **AND** 后处理输出（解码、NMS）
- **AND** 返回结构化结果

#### Scenario: 模型卸载
- **WHEN** 收到卸载请求或内存压力时
- **THEN** 系统 SHALL 释放模型占用的内存
- **AND** 关闭推理会话
- **AND** 更新服务状态

---

### Requirement: 配置管理

AI微服务 SHALL 支持通过环境变量和配置文件管理服务配置。

#### Scenario: 配置项
- **WHEN** 系统启动时
- **THEN** 系统 SHALL 加载 `Settings` 配置（Pydantic BaseSettings）
- **AND** 支持通过 `AI_` 前缀环境变量覆盖
- **AND** 验证配置完整性

#### Scenario: 主要配置项
- **WHEN** 配置加载时
- **THEN** 系统 SHALL 支持以下配置项：
  - `kafka_bootstrap_servers`：Kafka服务器地址
  - `grpc_port`：gRPC服务端口（默认50051）
  - `http_port`：HTTP服务端口（默认8000）
  - `model_dir`：模型文件路径
  - `detection_confidence`：检测置信度阈值（默认0.5）

---

### Requirement: 日志与监控

AI微服务 SHALL 提供日志记录和Prometheus监控指标。

#### Scenario: Prometheus指标
- **WHEN** 调用 `GET /metrics` 时
- **THEN** 系统 SHALL 返回Prometheus格式指标
- **AND** 包含推理请求计数（按分析类型）
- **AND** 包含推理耗时分布（按分析类型）

#### Scenario: 结构化日志
- **WHEN** 服务运行时产生日志时
- **THEN** 系统 SHALL 包含时间戳、日志级别、服务名称、消息内容
