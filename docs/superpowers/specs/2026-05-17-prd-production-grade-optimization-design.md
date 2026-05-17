# PRD 生产级优化 — 设计文档

创建: 2026-05-17 | 源自: autoplan + brainstorming

## 为什么需要这个优化

AICK-MMP PRD v3.0 覆盖了产品功能全范围，但 autoplan 审查发现 19 个具体差距（原 15 个 + brainstorming 补充 4 个领域）。这些差距直接影响产品能否面向真实企业客户交付。

## 涵盖的能力

### 新增能力 (10个)
| 能力 | 优先级 | 说明 |
|------|--------|------|
| api-rate-limiting | P1 | Redis 滑动窗口速率限制，三级阈值 |
| observability-stack | P1 | Prometheus + Grafana + 结构化日志 |
| db-index-strategy | P1 | 9 个核心查询索引 |
| storage-capacity-planning | P1 | 存储计算公式 + 监控阈值 |
| chaos-engineering | P1 | 季度故障注入测试 |
| rfc-7807-error-format | P1 | API 错误标准化 |
| secrets-management | P1 | K8s Secrets + 凭据轮换 |
| graceful-degradation | P1 | 四组件降级矩阵 |
| feature-flags | P1 | AI Kill Switch + API 版本弃用 |
| incident-response-runbook | P2 | Runbook + 合成监控 |

### 修改的能力 (5个)
| 能力 | 变更内容 |
|------|----------|
| requirements | 密码策略、市场聚焦、章节顺序 |
| architecture | Alert 实体新增 |
| video-recording-storage | edgeNodeId 字段 |
| industrial-theme | PRD 色彩系统同步 |
| api-key-management | API Key 速率限制 |

## 核心设计决策

决策清单 (D1-D14):
- D1: Alert 独立表 + 索引
- D2: 首次登录强制改密
- D3: 工业暗色主题为 PRD 默认
- D4: Redis 滑动窗口速率限制
- D5: RFC 7807 Problem+JSON（向后兼容）
- D6: 9 个核心查询索引
- D7: MVP 聚焦零售+仓储物流
- D8: Micrometer → Prometheus → Grafana
- D9: 单路 1080p H.265 ≈ 21.6GB/天
- D10: 混沌工程 + AI 精度回归
- D11: K8s External Secrets + 90 天轮换
- D12: AI/Kafka/Redis/MySQL 降级矩阵
- D13: Feature Flag 注册表 + Kill Switch
- D14: Runbook + 5 分钟合成监控

详见: `openspec/changes/prd-production-grade-optimization/design.md`

## 任务覆盖

16 个任务组，共 55 个任务:
- P0: 4 组 (安全策略、Alert 模型、Recording 补充、主题同步)
- P1: 10 组 (速率限制、RFC 7807、索引、可观测性、存储计算、混沌工程、PRD 修复、凭据管理、降级、功能开关)
- P2: 2 组 (AI 精度回归、应急响应 Runbook)

详见: `openspec/changes/prd-production-grade-optimization/tasks.md`
