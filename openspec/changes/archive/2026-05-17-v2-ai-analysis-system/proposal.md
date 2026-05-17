## Why

当前系统的视频分析能力（`MotionDetectionService`）仅为示例实现，缺乏真正的AI分析能力。随着业务发展，需要引入专业的AI能力来处理：
- **客流统计**：实时进出人数统计，为商业决策提供数据支撑
- **行为识别**：异常行为检测，提升安全管理水平
- **车牌识别**：车辆管理自动化，支持白名单、黑名单管理

通过构建集中式AI架构，所有推理在中央GPU服务器完成，边缘节点仅负责帧提取与转发，降低系统复杂度同时共享GPU资源。

## What Changes

### 新增功能

1. **独立AI微服务**
   - Python FastAPI服务，负责所有AI推理任务
   - 支持GPU加速（NVIDIA CUDA）
   - 与现有Java系统通过Kafka消息队列解耦集成

2. **云端AI分析**
   - 客流统计：YOLOv8 + ByteTrack，实现进出人数实时统计
   - 行为识别：OpenPifPaf姿态估计 + 规则引擎，支持滞留、闯入、聚集等告警

3. **gRPC实时帧流**
   - Edge → AI Service 通过 gRPC 双向流传输视频帧
   - 替代原始设计的Kafka帧传输方案，降低延迟和序列化开销

4. **Kafka结果总线**
   - 3个专用Topic（ai-passenger-stats, ai-behavior-events, ai-vehicle-records）+ 1个死信队列
   - JSON格式消息，无额外序列化依赖

5. **数据存储扩展**
   - AI分析结果表（客流统计、行为事件、车牌记录）
   - Redis缓存热点数据
   - 对象存储分析快照

6. **前端页面**
   - 实时客流大屏
   - 行为告警中心
   - 车牌查询管理

## Capabilities

### New Capabilities

- `ai-microservice`: AI微服务核心架构，定义服务框架、部署方案、gRPC+HTTP API规范
- `cloud-ai-analysis`: 集中式AI分析能力，包括客流统计和行为识别（替代原始混合云边方案）
- `edge-ai-vehicle`: 车牌识别能力（中央GPU推理，边缘仅转发帧）
- `ai-kafka-integration`: Kafka消息集成，定义3个结果Topic + DLQ
- `ai-data-storage`: AI数据存储设计，MySQL表结构和Redis缓存策略

### Modified Capabilities

- `motion-detection-recording`: 将现有的MotionDetectionService从示例实现升级为真正的AI分析

## Impact

### 新增代码

- `aick-mmp-ai/`: Python AI微服务项目
  - `src/api/`: FastAPI入口和gRPC服务器
  - `src/services/`: AI分析服务（detector, tracker, passenger, behavior, plate, pose）
  - `src/core/`: 模型加载器和配置管理
  - `src/integrations/`: gRPC协议和Kafka发布器
  - `models/`: ONNX模型文件

### 修改代码

- `aick-mmp-edge/`: 集成边缘AI车牌识别
- `aick-mmp-central/`: 新增AI管理API

### 新增依赖

- Python 3.11+
- FastAPI, ONNX Runtime, PyTorch
- kafka-python, aiokafka
- Redis, MinIO (对象存储)

### 新增基础设施

- AI微服务Docker镜像
- GPU计算资源（按需扩展）
- Redis缓存集群
- Kafka Topic配置

### API变更

- 新增 `/api/v1/ai/*` 管理API（REST，前端5秒轮询）
- 新增 gRPC 流式帧分析接口（Edge → AI Service）
