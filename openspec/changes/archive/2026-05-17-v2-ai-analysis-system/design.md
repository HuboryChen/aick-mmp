## Context

当前系统的 `MotionDetectionService` 仅提供简单的帧差分运动检测，注释明确写着"这是一个示例实现"。随着业务发展，需要引入真正的AI分析能力。

### 当前状态

```
摄像头 → RTSP流 → 边缘节点 → 帧差分 → 简单运动检测 → Kafka
```

### 目标状态

```
摄像头 → RTSP流 → 边缘节点 → FrameExtractor → gRPC流 → AI Service (集中GPU推理)
                                                          ↓
                                              Kafka 3 Topics → 中央服务 → 前端(5s轮询)
```

### 约束条件

- **Python AI + Java业务系统**：AI推理用Python实现，业务逻辑用Java实现
- **集中式AI架构**：所有AI推理集中在中央GPU服务器，边缘节点仅负责帧提取与转发
- **帧传输**：Edge → AI Service 使用 gRPC 双向流，替代 Kafka 帧传输
- **使用预训练模型**：不自行训练模型，使用开源预训练模型
- **GPU按需扩展**：根据分析路数动态调整GPU资源

### 利益相关者

- **运维团队**：需要简单的部署和监控方案
- **业务团队**：需要准确的客流统计和及时的告警
- **开发团队**：需要清晰的API和集成文档

## Goals / Non-Goals

### Goals

1. 构建独立的Python AI微服务，与Java系统松耦合
2. 实现客流统计（进出人数）+ 行为识别（滞留/闯入/聚集/跌倒）
3. 实现车牌识别（集中GPU推理，替代原始边缘端部署方案）
4. 通过Kafka消息队列集成AI分析结果与现有Java系统
5. 支持GPU加速和水平扩展

### Non-Goals

1. 不自行训练模型（使用预训练模型微调）
2. 不实现视频流传输（复用现有Janus Gateway）
3. 不实现人脸识别（隐私合规考虑）
4. 不实现多目标跟踪的精确ReID

## Decisions

### Decision 1: 微服务框架选择 FastAPI

**选项**：
- FastAPI + Uvicorn
- Flask
- gRPC + Protobuf

**决策**：FastAPI

**理由**：
- 异步高性能，与AI推理I/O特性匹配
- 自动OpenAPI文档，便于集成
- Pydantic数据验证，减少运行时错误
- 与Python生态深度集成（PyTorch、ONNX Runtime）

### Decision 2: AI框架选择 PyTorch + ONNX Runtime

**选项**：
- PyTorch原生推理
- ONNX Runtime
- TensorRT

**决策**：PyTorch用于训练/实验，ONNX Runtime用于生产部署

**理由**：
- ONNX Runtime支持跨平台部署（边缘+云端）
- 优化推理性能，支持GPU加速
- 模型与训练框架解耦

### Decision 3: 模型选型

| 任务 | 模型 | 理由 |
|------|------|------|
| 目标检测 | YOLOv8 | 高精度、易用、预训练模型丰富 |
| 姿态估计 | OpenPifPaf | 轻量级、开源、多人支持 |
| 车牌检测 | YOLOv8-plate | 预训练、支持多国车牌 |
| 车牌识别 | LPRNet | 端到端、高准确率、开源 |

**YOLOv8 输出格式自动适配**：
- YOLOv8 ONNX 输出格式取决于导出方式
- 支持自动检测：`PyTorch导出`、`ONNX standard`、`ONNX simplified`、`TensorRT`
- `warmup()` 时自动识别输出形状并配置后处理逻辑
- 无需手动配置，开箱即用

### Decision 4: 消息队列选型 Kafka

**决策**：复用现有Kafka基础设施

**理由**：
- 现有系统已集成Kafka
- 支持高吞吐消息传递
- 支持消息追溯和重放
- 消费者组机制支持水平扩展

### Decision 5: gRPC 通信（开发阶段）

**决策**：开发环境使用不安全gRPC连接，生产环境启用TLS

**理由**：
- 边缘节点通过gRPC流式传输视频帧到AI Service
- 开发环境内网通信，暂不启用TLS
- 生产环境跨网络部署时必须启用TLS加密

**配置**：
```yaml
ai:
  service:
    grpc:
      host: ai-service.default.svc.cluster.local
      port: 50051
      use_tls: false        # 开发环境 false，生产环境 true
      ca_cert: /certs/ca.crt
      client_cert: /certs/client.crt
      client_key: /certs/client.key
```

### Decision 6: 集中式AI架构

**决策**：所有AI分析集中在中央GPU服务器，边缘节点仅负责帧提取与转发

**理由**：
- 工程简化：取消边缘端AI推理模块，统一由AI Service处理所有分析类型
- GPU资源共享：车牌识别、客流分析、行为识别共享同一GPU资源
- 运维简化：单容器部署，无需管理边缘AI Agent
- 架构权衡：车牌识别延迟从边缘毫秒级增加到网络传输+集中推理（实测<500ms），但大幅降低系统复杂度

**架构示意**：
```
边缘节点          AI Service (单容器)
─────────        ─────────────────
FrameExtractor ──→ gRPC stream ──→ YOLO Detector ──→ PassengerAnalyzer
                 │                                    → BehaviorEngine
                 │                                    → PlateRecognizer
                 │                                    → PoseEstimator
                 │
                 └── Kafka Producer → ai-passenger-stats
                                      ai-behavior-events
                                      ai-vehicle-records
```

### Decision 7: 数据存储策略

**决策**：
- MySQL 8.0：AI分析结果（复用现有数据库基础设施）
- Redis 6.2：热点数据缓存（实时人数统计）
- MinIO：快照对象存储（行为告警截图、车牌识别截图）

**理由**：
- 复用现有MySQL 8.0基础设施（未使用PostgreSQL）
- Redis缓存减少数据库压力，Key格式 `ai:passenger:realtime:{cameraId}`
- MinIO提供S3兼容的对象存储接口，适用于存储AI分析截图与视频片段
- 通过 `FileController` 提供上传、下载、预签名URL接口

### Decision 8: gRPC 流式帧传输

**决策**：Edge → AI Service 通过 gRPC 双向流传输视频帧，替代 Kafka 帧传输

**理由**：
- 实时帧数据传输不适合消息队列（大体积、高吞吐）
- gRPC 流天然支持 streaming，延迟更低
- 单路 gRPC 连接即可承载一路摄像头的连续帧流
- 与原始设计（Kafka任务队列 + base64嵌入）相比，大幅降低序列化开销

### Decision 9: 前端轮询替代 WebSocket 推送

**决策**：前端通过 5 秒间隔轮询 REST API 获取 AI 分析结果

**理由**：
- 简化前端架构，无需维护 WebSocket 连接
- 客流分析（分钟级聚合）对实时性要求不高
- 行为告警有秒级需求，5秒轮询可接受
- 未来如需更实时推送，可切换为 WebSocket 或 SSE

## 容量规划与扩展性

### 设计目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 并发视频流 | 100路 | 初期目标 |
| 单路延迟 | < 500ms | P95 |
| 吞吐量 | 1000 FPS | 帧处理能力 |
| 可用性 | 99.9% | 年度停机 < 8.7小时 |

### 扩展策略

**水平扩展（节点扩展）**：
```
边缘节点 (FrameExtractor)
    │
    ▼
┌─────────────────────────────────────────┐
│           AI Service Cluster            │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ Pod #1  │ │ Pod #2  │ │ Pod #N  │   │
│  │ GPU: 1  │ │ GPU: 1  │ │ GPU: 1  │   │
│  └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────┘
    │
    ▼
  Kafka → 中央服务 → 前端
```

**扩展方式**：
1. **增加AI Pod数量**：通过HPA自动扩展，每个Pod独占1个GPU
2. **负载均衡**：边缘节点通过DNS发现多个AI服务实例
3. **Kafka分区隔离**：高优先级（告警）与普通消息分区处理

### 资源配置估算

| 规模 | GPU | CPU | 内存 | Pod数 |
|------|-----|-----|------|-------|
| 100路 | RTX 3090 x1 | 8核 | 16GB | 2 |
| 200路 | RTX 4090 x2 | 16核 | 32GB | 4 |
| 500路 | A100 x4 | 32核 | 64GB | 8 |

### HPA 配置

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-service
  minReplicas: 2      # 最小副本保证可用性
  maxReplicas: 10    # 最大副本限制成本
  metrics:
  - type: Resource
    resource:
      name: gpu-utilization
      target:
        type: Utilization
        averageUtilization: 70  # GPU利用率达70%时扩容
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60   # 扩容冷却
    scaleDown:
      stabilizationWindowSeconds: 300  # 缩容冷却
```

## Risks / Trade-offs

### Risk 1: GPU资源不足

**风险**：高并发分析时GPU成为瓶颈

**缓解**：
- HPA自动扩展AI微服务副本
- 多GPU集群部署
- 降级策略：降低分析帧率

### Risk 2: gRPC帧传输延迟

**风险**：gRPC流传输高分辨率帧可能导致网络瓶颈

**缓解**：
- 边缘节点降低分辨率后再传输（640x480）
- 控制帧率（客流1fps，车牌5fps）
- 监控gRPC连接延迟和吞吐量

### Risk 3: 集中式GPU资源瓶颈

**风险**：所有AI分析依赖中央GPU，单点故障风险

**缓解**：
- HPA自动扩展AI Service副本
- 支持降级策略（降低分析帧率、关闭非关键分析类型）
- 预留GPU资源冗余

### Risk 4: 模型准确率不足

**风险**：预训练模型在实际场景准确率不佳

**缓解**：
- 使用领域数据微调
- 设置置信度阈值过滤
- 人工复核机制

## Migration Plan

### Phase 1: AI Service 基础设施（已完成）

1. ✅ 创建AI微服务项目骨架 `aick-mmp-ai/`
2. ✅ 集成gRPC协议定义和服务器
3. ✅ 配置ONNX模型加载器
4. ✅ 创建Dockerfile（CUDA支持）
5. ✅ 集成Kafka结果发布器

### Phase 2: 客流分析（已完成）

1. ✅ 部署YOLOv8目标检测模型
2. ✅ 实现ByteTrack多目标跟踪
3. ✅ 实现进出人数统计（passenger_analyzer）
4. ✅ 集成中央服务Kafka消费者和REST API

### Phase 3: 行为识别（已完成）

1. ✅ 实现姿态估计（ONNX Runtime，替代原始OpenPifPaf方案）
2. ✅ 实现滞留/闯入/聚集/跌倒检测规则引擎
3. ✅ 实现Kafka行为告警推送
4. ✅ 前端行为告警页面

### Phase 4: 车牌识别（已完成，架构变更）

1. ✅ 实现YOLOv8-plate车牌检测
2. ✅ 实现LPRNet车牌字符识别
3. ✅ 白名单CRUD管理（Java后端 + 前端页面）
4. ✅ 车牌识别记录存储与查询
5. ❌ 边缘节点本地部署（改为集中式AI推理）
6. ❌ 本地SQLite缓存（取消边缘部署后不再需要）
7. ❌ 黑名单告警（待实现）

### Phase 5: 边缘节点帧传输（已完成）

1. ✅ 实现FrameExtractor帧提取器
2. ✅ 实现gRPC客户端流式传输
3. ✅ AI Service配置模块
4. ✅ 按摄像头配置分析类型和帧率

### Phase 6: Java 后端集成（已完成）

1. ✅ AI分析实体类 + Repository（4张表）
2. ✅ Kafka消费者（3个Topic + DLQ）
3. ✅ AI分析REST API（AIAnalysisController）
4. ✅ Redis实时人数缓存
5. ❌ WebSocket推送（改为前端轮询）

### Phase 7: 前端开发（已完成）

1. ✅ 客流实时仪表盘（AiPassengerDashboard）
2. ✅ 行为告警中心（AiBehaviorAlertCenter）
3. ✅ 车牌管理页面（AiLicensePlateManagement）
4. ⚠️ 前端5秒轮询（替代WebSocket）
5. ❌ AI配置管理页面（待实现）

### Phase 8: MinIO 对象存储集成（已完成）

1. ✅ Docker Compose 集成 MinIO 服务（端口 9000/9001，默认桶 ai-snapshots）
2. ✅ Maven 依赖 + MinioConfig + MinioService（4个操作：上传/下载/删除/预签名URL）
3. ✅ FileController REST API（POST upload / GET download / GET presigned / DELETE）
4. ✅ application.yml MinIO 配置节（支持环境变量覆盖）
5. ✅ 前端行为告警中心与车牌管理页快照展示

### Phase 9: AI 配置管理（已完成）

1. ✅ AiAnalysisConfig 实体/Repository/数据库迁移
2. ✅ REST API（GET/PUT/DELETE /api/v1/ai/configs/{cameraId}）
3. ✅ 前端 AiConfigManagement 页面（侧边栏菜单 AI分析 → 分析配置）
4. ✅ 支持每摄像头的分析类型开关、帧率、阈值配置

### Phase 10: 告警触发录像（已完成）

1. ✅ AiVehicleRecordConsumer 集成 RecordingNotificationService
2. ✅ 黑名单车辆检测时自动触发开始录像命令到 mmp-recording-commands topic
3. ✅ 录制时长 5 分钟，附带黑名单信息和触发原因

### Phase 11: 部署与运维（已完成）

1. ✅ Docker Compose配置
2. ✅ K8s Deployment + Service + HPA配置
3. ✅ Prometheus 监控配置（scrape 配置 + 4条告警规则）
4. ✅ Grafana 看板（8面板：GPU/延迟/流量/检测率/内存/Kafka积压）
5. ✅ 运维手册（架构/排障/调优/HPA/备份/部署/TLS）
6. ❌ 灰度发布与生产验证（待实际部署时完成）

### Phase 12: 测试（已完成）

1. ✅ AI 服务单元测试（12个测试文件）
2. ✅ 性能基准测试文档（docs/testing/ai-service-performance-test.md）
3. ✅ 全链路压测方案（4阶段：基线/负载/压测/恢复）

### Phase 14: 热力图（已完成）

1. ✅ AiPassengerStatsRepository 按小时聚合查询（findHourlyAggregatedStats）
2. ✅ REST API GET /api/v1/ai/heatmap（cameraId, startTime, endTime → 时段聚合数据）
3. ✅ 前端 AiHeatmap 页面（条形热力 + 统计卡片 + 时段数据表）

### Phase 15: 客流预测（已完成）

1. ✅ 后端预测引擎（基于历史移动平均 + 趋势模拟 + 置信度分级）
2. ✅ REST API GET /api/v1/ai/prediction（cameraId, historyHours, predictSteps）
3. ✅ 前端 AiPassengerPrediction 页面（柱状趋势图 + 置信度标签 + 历史范围滑块）

### Phase 16: gRPC TLS (设计已完成)

1. ✅ TLS 方案设计（证书生成、配置加载、部署步骤）
2. ✅ 开发环境不安全连接，生产环境启用 TLS
3. ❌ 实际证书部署（待生产环境部署时执行）

### 回滚策略

- 使用蓝绿部署，新版本验证后切换
- Kafka消费者支持版本兼容
- 数据库表结构向后兼容

## Open Questions

~~1. **GPU资源规划**：需要根据实际并发量确定GPU规格~~ → **已确认：支持100路并发，通过增加节点横向扩展**
~~2. **模型微调数据**：是否需要采集实际场景数据进行模型微调~~ → **待评估**
3. **告警通知方式**：是否需要集成短信/电话通知 → **待确认**
4. **数据保留策略**：分析结果保留多长时间 → **待确认**
~~5. **隐私合规**：是否需要人脸模糊处理~~ → **已排除（不实现人脸识别）**
~~6. **TLS加密**：生产环境是否需要TLS加密gRPC通信~~ → **已确认：开发环境不安全连接，生产环境需启用TLS**
~~7. **快照存储**：是否需要集成MinIO/OSS对象存储~~ → **已确认：集成MinIO，由MinioService + FileController提供完整的上传/下载/预签名URL功能**
