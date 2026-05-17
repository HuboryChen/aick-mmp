## Context

README.md 自项目早期创建后未系统性更新，而实际代码经过多轮迭代（v2.0 AI 分析系统、MinIO 存储、配置管理、热力图与客流预测等），导致文档与实现之间存在 10+ 处差异。同时 `docker-compose.yml` 缺少 MinIO 服务配置，与后端的 MinioService/MinioConfig/FileController 不一致。

## Goals / Non-Goals

**Goals:**
- 修复 README.md 所有已知偏差，使其准确反映当前实际实现
- 将 MinIO 服务补全到 docker-compose.yml
- 删除前端未使用的 socket.io-client 依赖（JS 层面不 import 导致的技术债）

**Non-Goals:**
- 不修改任何业务逻辑代码
- 不新增功能或重构
- 不更新 spec/ 文档（本次不涉及需求变更）

## Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Mermaid 架构图加入 AI 层 | 当前图缺少整个 AI 分析子系统（FastAPI + ONNX + gRPC + 3 Kafka Topics + MinIO），在新 member onboarding 时会造成误导 |
| 2 | API 文档从实际 Controller 代码反推 | 与其手动维护，不如直接扫描 `@XxxMapping` 注解生成，确保与实现 100% 一致 |
| 3 | 端口表直接从 docker-compose.yml 提取 | services.ports 是唯一真实来源，与 README 的端口表保持同步 |
| 4 | 删除未使用的 socket.io-client | package.json 声明了依赖但源码无 import，这是无意义的技术债。WebSocket 推送已被设计决策 Decision 9 确认为 5 秒轮询替代 |

## Risks / Trade-offs

- README.md 仍可能在未来迭代中再次过时 — 建议将 README 更新纳入后续变更的检查清单
- 删除 socket.io-client 不影响任何现有功能（实际未被使用）

## Open Questions

- 无
