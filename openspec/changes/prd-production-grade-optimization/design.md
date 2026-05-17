## Context

AICK-MMP PRD v3.0 通过 autoplan 审查发现 15 个生产级差距，涵盖安全、数据模型、可观测性、性能、测试、设计规范、市场范围等方面。这些是影响产品面向真实客户的阻塞性问题，需要在 V1.0 发布前修复。

当前状态：
- PRD 3.2.1 硬编码 admin/admin123 默认密码
- 缺少 Alert 核心实体定义
- 色彩系统与已实现的 CLAUDE.md 工业暗色主题不一致
- 无 API 速率限制策略
- 无数据库索引策略
- 无 Prometheus/Grafana 可观测性架构
- 无录像存储容量计算
- 测试策略缺少混沌工程和 AI 精度回归

## Goals / Non-Goals

**Goals:**
- 修复所有 P0 级安全、数据模型、设计规范问题
- 补齐 P1 级可观测性、性能、API 完善要素
- 重新聚焦 MVP 市场范围
- 在现有架构内完成所有改进，不引入新的外部依赖

**Non-Goals:**
- 不重新设计现有数据模型（仅在 Recording 表加字段、新增 Alert 表）
- 不改变前端技术栈
- 不引入新的基础设施组件（Prometheus/Grafana、External Secrets Operator 已是 K8s 生态标准组件）
- 不修改已有 migration 脚本（新增 migration）
- 不重新设计身份认证流程（仅增强密码策略）
- 不实现多租户隔离

## Decisions

### D1: Alert 实体设计
- **方案**：独立表，与 AlertRule 通过 ruleId 关联
- **理由**：告警记录是独立生命周期实体，需要独立查询、统计、归档
- **表结构**：id, ruleId, cameraId, level(ENUM), message, status(ENUM: UNRESOLVED/RESOLVED/ACKNOWLEDGED), detail(json), createTime, handleTime, handlerId
- **索引**：(cameraId, createTime) 复合索引，level 索引

### D2: 密码策略变更
- **方案**：移除默认密码，首次登录强制设置密码 + 发送随机密码邮件
- **理由**：等保三级要求初始密码需修改，不在文档/代码中暴露
- **影响**：修改 PRD 3.2.1 节 + 修改注册流程

### D3: 色彩系统同步
- **方案**：PRD 采用与 CLAUDE.md 一致的工业暗色主题（#00d4ff 主色），保留"亮色主题"作为可选项
- **理由**：文档应与实现一致，避免开发混淆
- **影响**：PRD 6.2.1 节重写

### D4: API 速率限制策略
- **方案**：基于 Redis 的滑动窗口算法（已使用 Redis，零额外依赖）
- **阈值**：非认证请求 20/min，认证用户 100/min，API Key 1000/min
- **响应**：429 Too Many Requests + Retry-After 头

### D5: 错误码标准化
- **方案**：采用 RFC 7807 Problem Details 格式
- **理由**：行业标准，客户端库广泛支持（Spring Boot 内置支持 Problem+JSON）
- **兼容**：保留原有 code/message 字段，新增 type/title/status/detail 字段

### D6: 数据库索引策略
- **索引清单**：
  - camera(name) UNIQUE
  - camera(edge_node_id, status)
  - recording(camera_id, start_time, type) 复合索引
  - recording(start_time) 范围查询索引
  - alert(camera_id, create_time)
  - alert(rule_id, status)
  - edge_node(region_id, status)
  - stream_session(camera_id, status)
  - stream_session(user_id)

### D7: 市场范围聚焦
- **方案**：MVP 聚焦"连锁零售"和"仓储物流"2 个细分市场
- **理由**：这两个市场痛点最一致（多站点统一管理），购买决策链最短

### D8: 可观测性架构
- **方案**：Spring Boot Actuator + Micrometer → Prometheus → Grafana
- **指标**：JVM 指标、API 延迟分布(P50/P95/P99)、视频流延迟、节点心跳延迟
- **日志**：ELK 格式结构化日志（application-json.log），保留 30 天

### D9: 存储容量计算
- **公式**：`单路日存储量 = 码率(bps) × 86400 / 8`
- **基准**：1080p@30fps H.265 ≈ 2Mbps → 21.6GB/天；H.264 ≈ 4Mbps → 43.2GB/天

### D10: 测试增强
- **混沌工程**：使用 Chaos Monkey 模拟随机实例故障，验证 HA 自动切换
- **AI 精度回归**：模型更新时自动在标注数据集上验证（保存 1000 张测试图片作为基准）

### D11: 凭据管理方案

#### D11a: 全局凭据（Spring Profile + 环境变量）
- **方案**：Spring Boot 原生 `application-{profile}.yml` + K8s 环境变量覆盖，零额外依赖
- **文件结构**：
  - `application.yml`：通用配置，使用 `${PLACEHOLDER}` 占位符
  - `application-dev.yml`（.gitignore）：开发者本地凭据，复制自 `application-dev.example.yml`（入仓）
  - 生产环境：通过 K8s Deployment `env:` 字段注入，Spring Boot 自动解析环境变量覆盖
- **覆盖范围**：DB 密码、Redis 密码、Kafka SASL 凭证、JWT Signing Secret、摄像头凭据加密密钥
- **轮换策略**：更新环境变量 → 滚动重启 Pod，手动操作（MVP 阶段不需要自动化轮换）
- **调试方式**：Actuator `/actuator/env` 查看配置来源，`--debug` 追踪配置加载

#### D11b: 摄像头凭据（数据库加密存储）
摄像头密码不同于全局凭据——每个摄像头独立拥有，需要随摄像头 CRUD 生命周期管理。

- **存储**：`cameras.password` 列存储 AES-256-GCM 加密密文（Base64 编码），`cameras.username` 明文存储（非敏感）
- **加密方式**：JPA `AttributeConverter` 自动加解密，业务代码无感知，`camera.getPassword()` 始终返回明文
- **加密密钥**：独立配置项 `security.encryption.camera-credential-key`，与 SystemApp 密钥隔离，减少爆破半径
- **密钥来源**：走 D11a 全局凭据管理流程（环境变量注入）
- **API 层**：
  - 管理面 API（`GET /api/cameras`）：密码字段返回 `******` 脱敏
  - 边缘节点 API（`GET /api/cameras/edge-node/{nodeId}`）：返回解密后明文（边缘拉流必需）
  - 写接口（`POST/PUT`）：接受明文密码，Converter 自动加密存库
- **缓存**：解密结果缓存到 Redis，TTL 1 小时；摄像头更新时主动失效缓存
- **轮换**：管理员更新摄像头密码 → PUT API → Converter 加密新密码 → Redis 缓存失效 → 边缘节点下次拉取拿到新密码重连，秒级生效，无需重启
- **存量迁移**：Flyway Java-based Migration，一次性加密现有明文密码（检测 Base64+IV 长度判断是否已加密，幂等执行）

### D12: 优雅降级矩阵

| 故障组件 | 影响范围 | 降级行为 | 恢复条件 |
|----------|----------|----------|----------|
| AI 服务 | AI 分析、客流统计 | 视频监控完整可用，AI 面板显示"分析暂不可用" | 服务健康检查恢复 |
| Kafka | 告警、事件、心跳 | 告警切换到直接 API 调用（同步降级），心跳堆积后批量同步 | Kafka 重建完成 |
| Redis | 会话缓存、速率限制、流会话 | 回退本地缓存，首次访问无缓存；速率限制暂停；流会话检查降级 | Redis Sentinel 切换完成 |
| MySQL | 全部写操作 | 只读模式：可浏览摄像头、查看录像、查看告警；无法增删改设备 | MGR 主节点恢复 |

### D13: 功能开关系统
- **方案**：基于数据库的 Feature Flag 注册表 + 本地缓存（TTL 30 秒），零额外依赖
- **开关粒度**：AI 功能级（`ai.behavior-detection`, `ai.plate-recognition`, `ai.passenger-analysis`），可按区域/全局控制
- **Kill Switch 优先级**：系统级全局禁用 > 区域级 > 单摄像头级
- **API 版本弃用策略**：标注 `DEPRECATED` 响应头 → 保留 6 个月兼容 → 新版覆盖率 > 90%后移除

### D14: 应急响应 Runbook
- **边缘节点批量离线 Runbook**：确认网络（ping/路由）→ 切流到备用节点 → 逐台排查 → 恢复验证 → 复盘
- **存储满 Runbook**：确认使用率 → 紧急清理（最老的非关键录像）→ 扩容申请 → 根因分析
- **AI 误报风暴 Runbook**：执行功能开关 Kill Switch → 收集误报数据 → 回退模型 → 修复后灰度开放
- **合成监控**：每 5 分钟模拟 E2E 流程（登录 → 获取摄像头列表 → 启动视频流 → 查询录像 → 登出），失败即时告警

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 速率限制误伤正常用户 | 阈值可配置，初期设为宽松值（200/min），观察后收紧 |
| 索引增加写入延迟 | 所有新增索引在低峰期创建，监控写入性能 |
| 市场范围聚焦可能错过其他机会 | MVP 阶段聚焦，V1.0 按客户反馈扩展 |
| RFC 7807 过渡期客户端兼容 | 新旧格式共存一期，前端优先迁移 |
| 凭据管理增加部署复杂度 | 提供部署脚本自动化 Secrets 创建，文档化故障排查步骤 |
| 降级逻辑需要额外错误处理路径 | 每个服务调用的 fallback 在集成测试中覆盖 |
| 功能开关的缓存 TTL 导致 Kill Switch 生效延迟 | TTL 设为 30 秒，紧急时可手动刷新缓存或直查数据库 |
| Runbook 维护不及时会过时 | Runbook 与代码同仓管理，故障处理后强制更新 |
