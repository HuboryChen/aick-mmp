## 1. docker-compose.yml 修复

- [x] 1.1 在 ai-service 之前添加 MinIO 服务定义（端口 9000/9001，环境变量，卷 minio-data，网络 aick-network）
- [x] 1.2 在 volumes 节添加 `minio-data` 卷

## 2. README.md — 架构图更新

- [x] 2.1 重写 Mermaid 图：加入 AI 服务层（FastAPI + ONNX）、gRPC 帧传输流、Kafka 3 Topics、MinIO、RTSP Server
- [x] 2.2 保持图中现有关系不变，仅补充缺失组件

## 3. README.md — 端口映射表修复

- [x] 3.1 Central LB 端口从 `80 → 8080` 改为 `8090 → 80`
- [x] 3.2 补充 MinIO 端口（9000 API / 9001 Console）
- [x] 3.3 补充 RTSP Server 端口（8554 RTSP / 1935 RTMP / 8888 HTTP）

## 4. README.md — 技术栈版本修正

- [x] 4.1 Spring Security `5.x` → `6.x`
- [x] 4.2 Python `3.14` → `3.11`

## 5. README.md — 环境变量表修正

- [x] 5.1 `ZONE` → 替换为 `REGION`
- [x] 5.2 补充 MinIO 变量（MINIO_ENDPOINT、MINIO_ACCESS_KEY、MINIO_SECRET_KEY、MINIO_BUCKET_NAME）

## 6. README.md — AI API 文档补全

- [x] 6.1 补充黑名单 CRUD（4 个端点）
- [x] 6.2 补充车辆统计端点
- [x] 6.3 补充 AI 分析配置 CRUD（3 个端点）
- [x] 6.4 补充热力图端点
- [x] 6.5 补充客流预测端点
- [x] 6.6 补充文件上传/下载/预签名/删除（4 个端点）

## 7. README.md — v2.0 功能列表补全

- [x] 7.1 黑名单告警管理
- [x] 7.2 AI 分析配置管理页面
- [x] 7.3 区域热力图
- [x] 7.4 客流预测
- [x] 7.5 MinIO 对象存储（快照）
- [x] 7.6 告警触发录像
- [x] 7.7 Prometheus 监控配置
- [x] 7.8 Grafana 看板
- [x] 7.9 运维手册

## 8. 清理未使用依赖

- [x] 8.1 从 frontend/package.json 删除 `socket.io-client` 依赖
- [x] 8.2 从前端技术栈表删除 Socket.IO

## 9. 验证

- [x] 9.1 确认 docker-compose config 语法有效
- [x] 9.2 确认 README.md 中所有端口引用与 docker-compose.yml 一致
- [x] 9.3 确认 README.md 中所有 API 路径与实际 Controller 一致
