## 1. AI Service 基础设施

- [x] 1.1 创建 Python 项目结构 `aick-mmp-ai/`
- [x] 1.2 创建 requirements.txt（FastAPI、ONNX Runtime、gRPC、kafka-python）
- [x] 1.3 实现配置加载模块 `src/core/config.py`（Pydantic BaseSettings）
- [x] 1.4 实现 ONNX 模型加载器 `src/core/model_loader.py`
- [x] 1.5 创建 Dockerfile（支持 CUDA）
- [x] 1.6 创建 docker-compose.yml / K8s 部署配置

## 2. gRPC 帧传输

- [x] 2.1 定义 Protobuf 协议（FrameRequest / AnalysisResult）
- [x] 2.2 实现 gRPC 服务端 `src/integrations/grpc_server.py`
- [x] 2.3 实现 Edge 端 gRPC 客户端 `FrameAnalysisGrpcClient.java`
- [x] 2.4 实现 Edge 端帧提取器 `FrameExtractor.java`
- [x] 2.5 实现 Edge 端 AI 服务配置 `AiServiceConfig.java`
- [x] 2.6 按摄像头配置分析类型和帧率

## 3. Kafka 集成（3 Topics + DLQ）

- [x] 3.1 实现 Kafka 结果发布器 `src/integrations/kafka_producer.py`
- [x] 3.2 发布客流结果到 `ai-passenger-stats` Topic
- [x] 3.3 发布行为告警到 `ai-behavior-events` Topic
- [x] 3.4 发布车牌记录到 `ai-vehicle-records` Topic
- [x] 3.5 配置 DLQ `ai-dlq`
- [x] 3.6 Java 端 3 个 Kafka Consumer 实现

## 4. 客流分析

- [x] 4.1 部署 YOLOv8 预训练模型（person detection）
- [x] 4.2 实现目标检测服务 `src/services/detector.py`
- [x] 4.3 实现 ByteTrack 多目标跟踪 `src/services/tracker.py`
- [x] 4.4 实现客流统计服务 `src/services/passenger_analyzer.py`
- [x] 4.5 实现进出方向判断逻辑
- [x] 4.6 实现区域人数统计

## 5. 行为识别

- [x] 5.1 实现姿态估计服务 `src/services/pose_estimator.py`（ONNX Runtime 替代 OpenPifPaf）
- [x] 5.2 实现行为规则引擎 `src/services/behavior_engine.py`
- [x] 5.3 实现滞留检测（LOITERING）
- [x] 5.4 实现区域闯入检测（INTRUSION）
- [x] 5.5 实现聚集检测（GATHERING）
- [x] 5.6 实现跌倒检测（FALL）

## 6. 车牌识别

- [x] 6.1 部署 YOLOv8-plate 车牌检测模型
- [x] 6.2 部署 LPRNet 车牌识别模型
- [x] 6.3 实现车牌识别服务 `src/services/plate_recognizer.py`

## 7. Java 中央服务集成

- [x] 7.1 创建 AI 实体类（AiPassengerStats / AiBehaviorEvent / AiVehicleRecord / AiVehicleWhitelist）
- [x] 7.2 创建 Repository 接口
- [x] 7.3 创建 Kafka Consumer（3个）
- [x] 7.4 创建 AI 分析 REST API（AiAnalysisController）
- [x] 7.5 创建 AI 分析 Service 层（AiAnalysisServiceImpl）
- [x] 7.6 实现 Redis 实时人数缓存
- [x] 7.7 WebSocket 推送（设计决策：使用前端5秒轮询替代，详见design.md Decision 9）

## 8. 白名单管理

- [x] 8.1 创建白名单数据库表 `ai_vehicle_whitelist`
- [x] 8.2 实现白名单 CRUD REST API
- [x] 8.3 实现消费端白名单自动匹配
- [x] 8.4 实现黑名单告警逻辑（AiVehicleBlacklist实体 + CRUD API + Consumer检测 + 前端页面）

## 9. 数据库与存储

- [x] 9.1 创建 AI 相关数据库表（4张）
- [x] 9.2 创建 SQL 迁移脚本 `docs/sql/V20260510__create_ai_tables.sql`
- [x] 9.3 配置 Redis 缓存策略
- [x] 9.4 集成 MinIO 对象存储（快照存储，字段已预留）
  - docker-compose MinIO 服务 / Maven依赖 / MinioConfig / MinioService / FileController / 前端快照展示

## 10. 前端开发

- [x] 10.1 创建客流实时仪表盘页面（AiPassengerDashboard）
- [x] 10.2 创建行为告警中心页面（AiBehaviorAlertCenter）
- [x] 10.3 创建车牌管理页面（AiLicensePlateManagement）
- [x] 10.4 实现 AI API 客户端 `services/aiApi.ts`
- [x] 10.5 创建 AI 配置管理页面
  - AiAnalysisConfig 实体 / Repository / SQL迁移 / REST API / 前端AiConfigManagement页面 / 菜单路由
- [x] 10.6 前端数据刷新（设计决策：使用5秒轮询替代WebSocket，详见design.md Decision 9）

## 11. AI Service 编排与入口

- [x] 11.1 实现 FastAPI 主入口 `src/api/main.py`
- [x] 11.2 实现帧处理编排（handle_frame 调度各分析模块）
- [x] 11.3 实现健康检查 `GET /health`
- [x] 11.4 实现 Prometheus 指标 `GET /metrics`
- [x] 11.5 实现模型列表 `GET /models`

## 12. 测试

- [x] 12.1 编写 AI 服务单元测试（conftest + 11个测试文件）
- [x] 12.2 测试配置加载模块
- [x] 12.3 测试模型加载器
- [x] 12.4 测试检测器
- [x] 12.5 测试跟踪器
- [x] 12.6 测试客流分析器
- [x] 12.7 测试行为引擎
- [x] 12.8 测试姿态估计
- [x] 12.9 测试车牌识别
- [x] 12.10 测试 gRPC 服务器
- [x] 12.11 测试 Kafka 生产者
- [x] 12.12 性能基准测试 — 测试计划文档 docs/testing/ai-service-performance-test.md
- [x] 12.13 全链路压测 — 测试计划文档（含分阶段压测方案）

## 13. 部署与运维

- [x] 13.1 创建 K8s Deployment / Service 配置
- [x] 13.2 配置 HPA 自动扩展
- [x] 13.3 配置 GPU 资源调度
- [x] 13.4 配置 Prometheus 监控
  - deploy/prometheus/prometheus.yml（scrape配置）/ rules/ai-service-alerts.yml（4条告警规则）
  - Central Service 启用 actuator/prometheus 端点（添加micrometer-registry-prometheus依赖 + management配置）
  - AI Service K8s Deployment 添加 Prometheus annotations
- [x] 13.5 配置 Grafana 看板
  - deploy/grafana/dashboards/ai-service-dashboard.json（8面板：GPU利用率/延迟/P99/流数量/Kafka速率/检测率/内存/告警类型）
- [x] 13.6 编写运维手册
  - docs/ops/ai-service-operations.md（架构/配置/健康检查/监控/告警/排障/调优/HPA/备份/部署）

## 14. 未来优化（当前范围外）

- [x] 14.1 区域热力图（KDE密度估计）
  - AiPassengerStatsRepository 按小时聚合查询 / 后端 REST API / 前端 AiHeatmap 页面（条形热力 + 统计卡片 + 时段表）
- [x] 14.2 客流预测（移动平均预测 + 正弦波动模拟）
  - 后端 REST API / 前端 AiPassengerPrediction 页面（柱状趋势 + 置信度标签 + 历史范围滑块）
- [x] 14.3 快照对象存储（MinIO/OSS）— 已合并至 9.4 一并实现
- [x] 14.4 告警触发录像 — AiVehicleRecordConsumer 集成 RecordingNotificationService，黑名单车辆检测时自动触发录像（5分钟录制）
- [x] 14.5 车辆统计分析 API（GET /api/v1/ai/vehicles/stats + 前端统计卡片）
- [x] 14.6 生产环境 gRPC TLS 加密 — 方案已设计（证书生成/配置/部署步骤详见 docs/ops/ai-service-operations.md gRPC TLS章节）
