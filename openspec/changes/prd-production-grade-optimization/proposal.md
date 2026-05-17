## Why

当前 PRD (v3.0) 覆盖了完整的产品功能范围，但在生产级产品质量方面存在 15 个具体差距，涵盖安全（默认密码硬编码）、数据模型（Alert 实体缺失）、可观测性（无 Prometheus 集成）、性能保障（无索引策略、无速率限制）、测试完整性（无混沌工程、无 AI 精度回归）等方面。这些差距必须在产品面向真实客户前修复。

## What Changes

- **安全加固**：移除 PRD 中的默认密码硬编码，改为"首次登录强制修改随机密码"
- **数据模型补充**：新增 Alert 实体定义，Recording 表增加 edgeNodeId 字段
- **设计规范同步**：PRD 色彩系统与 CLAUDE.md 工业暗色主题保持一致
- **API 完善**：添加速率限制策略、分页响应增加排序字段
- **可观测性**：明确 Prometheus + Grafana 技术栈
- **索引策略**：为核心查询路径添加数据库索引
- **存储计算**：添加录像存储开销估算（单路 1080p@30fps H.265 ≈ 20GB/天）
- **测试增强**：补充混沌工程测试、AI 精度回归测试
- **范围聚焦**：MVP 从 5 个目标细分市场聚焦到 2 个
- **错误码标准化**：改为 RFC 7807 Problem Details 格式
- **凭据管理**：K8s Secrets + 定期轮换，源头消除硬编码凭据
- **优雅降级**：定义 AI/Kafka/Redis/MySQL 各组件故障时的系统行为
- **功能开关**：AI 功能级 Kill Switch + API 版本弃用策略
- **应急响应**：Runbook 体系 + 合成监控

## Capabilities

### New Capabilities
- `api-rate-limiting`: API 调用频率限制策略（100 req/min/user, 1000 req/min/API Key）
- `observability-stack`: Prometheus + Grafana 监控体系
- `db-index-strategy`: 数据库索引策略定义
- `storage-capacity-planning`: 录像存储容量计算和规划
- `chaos-engineering`: 生产环境故障注入测试
- `rfc-7807-error-format`: API 错误响应标准化为 RFC 7807

### Modified Capabilities
- `requirements`: PRD 安全策略（移除默认密码）、市场范围（聚焦到 2 个细分市场）、定价章节顺序修复
- `architecture`: 新增 Alert 实体到数据模型
- `video-recording-storage`: Recording 表添加 edgeNodeId 字段
- `industrial-theme`: 色彩系统规范与 CLAUDE.md 同步
- `api-key-management`: API 速率限制策略

### New Capabilities (Batch 2 — from brainstorming)
- `secrets-management`: 凭据管理方案（K8s Secrets + 定期轮换）
- `graceful-degradation`: 组件故障时的优雅降级策略
- `feature-flags`: 功能开关系统（Kill Switch + API 版本弃用）
- `incident-response-runbook`: 应急响应 Runbook + 合成监控

## Impact

- **PRD 文档**：3.2.1 节登录默认密码策略修改，1.5 节目标市场范围收缩
- **数据模型**：Alert 实体新增对应 migration 脚本，Recording 表新增字段
- **API 规范**：8.4 节错误码改为 RFC 7807，所有 API 响应增加 Rate-Limit 头
- **前端主题**：6.2.1 节色彩规范同步为工业暗色主题
- **测试计划**：第 9 章补充混沌工程和 AI 精度回归测试
- **非功能需求**：4.6 节补充 Prometheus/Grafana，4.1 节补充存储估算
- **凭据安全**：配置文件和代码中移除所有明文凭据，改为 K8s Secrets 注入
- **降级架构**：AI/Kafka/Redis/MySQL 各组件增加降级逻辑和 fallback 路径
- **功能控制**：新增 feature_flag 表、API 弃用响应头、前端开关对接
- **运维能力**：新增 runbook 文档和合成监控脚本
