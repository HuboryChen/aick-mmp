## Why

README.md 严重滞后于实际代码实现，包含多处架构描述过时、端口映射错误、API 文档不完整、技术栈版本错误以及缺少 MinIO docker-compose 配置等问题。新成员 onboarding 和技术评审依赖 README 作为入口，当前的偏差会导致认知混乱。

## What Changes

- 更新系统架构图（Mermaid），加入 AI 服务、gRPC 帧传输、Kafka 3 Topics、MinIO、RTSP Server
- 修复端口映射表（Central LB 8090，补充 MinIO/RTSP 端口）
- 补全 AI API 文档（黑名单、车辆统计、分析配置、热力图、客流预测、文件上传下载）
- 修复技术栈版本（Spring Security 5.x → 6.x，Python 3.14 → 3.11）
- 更新环境变量表（用 REGION 替换不存在的 ZONE，补充 MinIO 变量）
- 补全 v2.0 功能列表（黑名单、热力图、预测、配置管理、MinIO、告警录像、Prometheus/Grafana）
- 将 MinIO 服务添加到 docker-compose.yml（含 minio-data 卷）
- 删除或确认 Socket.IO 的引用使用情况

## Capabilities

### New Capabilities
（无 — 本次变更仅涉及项目文档和 docker-compose 配置，不涉及新能力）

### Modified Capabilities
（无 — 无 spec 级别的需求变更）

## Impact

- `README.md`：大幅重写架构图、API 文档、功能列表、端口表、环境变量表、技术栈表
- `docker-compose.yml`：添加 MinIO 服务（端口 9000/9001，卷 minio-data）
