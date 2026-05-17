## 1. Security & Password Policy (P0)

- [ ] 1.1 修改 PRD 3.2.1 节：移除 admin/admin123 默认密码提示，改为"系统生成随机密码，首次登录强制修改"
- [ ] 1.2 实现首次登录强制修改密码的后端逻辑（token 添加 forcePasswordChange 标记）
- [ ] 1.3 实现首次登录密码修改页面 UI 和交互逻辑
- [ ] 1.4 添加用户注册时系统生成随机密码并安全分发的机制

## 2. Alert 数据模型 (P0)

- [ ] 2.1 创建 Alert JPA 实体（id, ruleId, cameraId, level, message, detail, status, createTime, handleTime, handlerId）
- [ ] 2.2 创建 Alert Repository 接口（支持按 cameraId+createTime 查询、按 level/status 统计）
- [ ] 2.3 创建 Alert 数据库 migration 脚本（V20260517__create_alert_table.sql）
- [ ] 2.4 更新 schema-full.sql 添加 Alert 表定义
- [ ] 2.5 在 PRD 5.1 节补充 Alert 数据模型定义

## 3. Recording 数据模型补充 (P0)

- [ ] 3.1 在 Recording JPA 实体中添加 edgeNodeId 字段
- [ ] 3.2 创建数据库 migration 脚本添加 Recording.edge_node_id 列
- [ ] 3.3 在录像查询 API 响应中包含 edgeNodeId
- [ ] 3.4 更新 PRD 5.1.6 Recording 表添加 edgeNodeId 字段

## 4. PRD 设计系统同步 (P0)

- [ ] 4.1 重写 PRD 6.2.1 节色彩规范为工业暗色主题（主色 #00d4ff，背景 #0a0e17）
- [ ] 4.2 在 PRD 中明确说明亮色主题是可选的替代主题
- [ ] 4.3 确保 PRD 色彩值与 CLAUDE.md / theme.css 中的 CSS 变量一致

## 5. API 速率限制 (P1)

- [ ] 5.1 实现基于 Redis 滑动窗口的速率限制过滤器（RateLimitingFilter）
- [ ] 5.2 配置匿名请求、认证用户、API Key 三种速率限制阈值
- [ ] 5.3 实现 429 响应 + Retry-After 头 + X-RateLimit-* 头
- [ ] 5.4 在 PRD 4.3 节补充速率限制策略
- [ ] 5.5 在 API Key 管理界面添加速率限制配置

## 6. RFC 7807 错误码标准化 (P1)

- [ ] 6.1 实现全局异常处理器返回 RFC 7807 Problem+JSON 格式
- [ ] 6.2 保留现有 code/message 字段保持向后兼容
- [ ] 6.3 为验证错误添加 errors 数组（包含 field, rejectedValue, message）
- [ ] 6.4 创建 /api/problems 问题类型注册表文档
- [ ] 6.5 更新 PRD 8.4 节错误码定义

## 7. 数据库索引策略 (P1)

- [ ] 7.1 创建数据库 migration 脚本添加核心索引（camera, recording, alert, edge_node, stream_session）
- [ ] 7.2 更新 schema-full.sql 添加索引定义
- [ ] 7.3 在 PRD 5.1 节补充索引说明

## 8. 可观测性架构 (P1)

- [ ] 8.1 在 backend 项目中配置 Micrometer + Prometheus 指标暴露
- [ ] 8.2 添加自定义指标：API 延迟分布、视频流延迟、节点心跳延迟
- [ ] 8.3 配置结构化 JSON 日志输出（ELK 格式）
- [ ] 8.4 创建 Grafana dashboard 配置模板（service health, API latency, stream latency, edge heartbeats）
- [ ] 8.5 更新 PRD 4.6 节补充可观测性技术栈

## 9. 存储容量规划 (P1)

- [ ] 9.1 编写存储容量计算文档（含公式、各分辨率/编码参考值）
- [ ] 9.2 更新 PRD 添加存储容量规划指南
- [ ] 9.3 更新 PRD 4.1 节补充存储基准数据

## 10. 混沌工程测试 (P1)

- [ ] 10.1 编写 Nginx 故障切换测试用例
- [ ] 10.2 编写 MySQL 主从切换测试用例
- [ ] 10.3 编写 Kafka Broker 故障测试用例
- [ ] 10.4 将混沌工程测试纳入季度测试计划
- [ ] 10.5 更新 PRD 9.3 节补充混沌工程测试

## 11. PRD 范围与结构修复 (P1)

- [ ] 11.1 调整 PRD 1.5 节目标市场：MVP 聚焦连锁零售和仓储物流
- [ ] 11.2 修复 PRD 章节编号：将第 12 节（商业模式）移到第 11 节（风险评估）之后
- [ ] 11.3 修复 PRD 内部引用不一致（如 3.9.x 实际在系统设置下）

## 12. AI 精度回归测试 (P2)

- [ ] 12.1 建立 AI 模型精度基准测试数据集（至少 1000 张标注图片）
- [ ] 12.2 实现模型更新时自动运行精度验证的 CI 流程
- [ ] 12.3 设置精度阈值：客流 >95%，行为识别 >90%，车牌 >98%
- [ ] 12.4 更新 PRD 3.10.4 节补充 AI 精度验证要求

## 13. 凭据管理 (P1)

### 13a. 全局凭据（Spring Profile + 环境变量）

- [ ] 13.1 审计当前代码中所有明文凭据配置，替换为 `${PLACEHOLDER}` 引用
- [ ] 13.2 创建 `application-dev.example.yml` 模板文件（入仓），记录所有必需的环境变量占位符
- [ ] 13.3 将 `application-dev.yml` 加入 `.gitignore`，从版本控制中移除现有文件（如有）
- [ ] 13.4 编写凭据管理部署文档（含 K8s Env 配置、轮换步骤、Actuator 调试方法）
- [ ] 13.5 更新 PRD 4.3.3 节补充凭据管理方案

### 13b. 摄像头凭据加密

- [ ] 13.6 在 `EncryptionProperties` 中新增 `cameraCredentialKey` 配置项
- [ ] 13.7 在 `AESEncryptionUtil` 中新增 `encryptCameraPassword()` / `decryptCameraPassword()` 方法（使用独立密钥）
- [ ] 13.8 创建 `CameraPasswordEncryptor` 实现 JPA `AttributeConverter`（加密写库、解密读库，业务代码无感知）
- [ ] 13.9 在 `Camera` 实体的 `password` 字段添加 `@Convert(CameraPasswordEncryptor.class)` 注解
- [ ] 13.10 修改 `CameraServiceImpl` 管理面 API 返回时 password 字段脱敏为 `******`
- [ ] 13.11 实现解密结果 Redis 缓存（key: `camera:pwd:decrypted:{cameraId}`, TTL: 1 小时），摄像头更新时主动失效
- [ ] 13.12 创建 Flyway Java-based Migration 加密存量明文密码（检测是否已加密，幂等执行）
- [ ] 13.13 在 `application.yml` 和 `application-dev.example.yml` 中添加 `security.encryption.camera-credential-key` 占位符

## 14. 优雅降级 (P1)

- [ ] 14.1 实现 AI 服务不可用时视频监控不受影响的 fallback 逻辑
- [ ] 14.2 实现 Kafka 不可用时告警降级为同步 API 调用
- [ ] 14.3 实现 Redis 不可用时回退本地缓存的降级策略
- [ ] 14.4 实现 MySQL 只读模式下仍可浏览摄像头和录像
- [ ] 14.5 在前端各区适配降级状态展示（AI 面板显示"暂不可用"等）
- [ ] 14.6 编写降级场景的集成测试
- [ ] 14.7 更新 PRD 添加优雅降级矩阵

## 15. 功能开关系统 (P1)

- [ ] 15.1 实现基于数据库的 Feature Flag 注册表 + 本地缓存
- [ ] 15.2 实现 AI 功能级 Kill Switch（ai.behavior-detection 等）
- [ ] 15.3 实现按区域/全局的开关控制
- [ ] 15.4 实现 API 版本弃用响应头 + 弃用周期管理
- [ ] 15.5 在前端根据功能开关控制功能可见性
- [ ] 15.6 更新 PRD 添加功能开关策略

## 16. 应急响应 Runbook (P2)

- [ ] 16.1 编写边缘节点批量离线处理 Runbook
- [ ] 16.2 编写存储满紧急处理 Runbook
- [ ] 16.3 编写 AI 误报风暴处理 Runbook
- [ ] 16.4 实现合成监控（每 5 分钟模拟 E2E 用户行为检查）
- [ ] 16.5 更新 PRD 第 11 节补充应急响应和合成监控
