## Context

项目根目录聚集了 32 项内容，其中 AI 工具配置目录占 5 个（.claude/.cursor/.codebuddy/.qoder/.trae），规格文档分 spec/ 和 openspec/specs/ 两套，SQL 脚本同时存在于 docs/sql/ 和 backend resources/db/，以及根层 Dockerfile 已无引用但未被清理。此外 docker-compose.yml 引用的 Janus 配置路径断裂导致容器启动失败。

## Goals / Non-Goals

**Goals:**
- 根目录配置目录从 5 个压缩到 1 个（仅保留 .claude/）
- 废弃 spec/，内容迁移到 openspec/specs/
- SQL 迁移脚本统一到 backend resources/db/migration/
- 修复 Janus 配置路径 bug
- 删除无引用的根层 Dockerfile
- 在 README 添加文档地图

**Non-Goals:**
- 不改动业务代码
- 不改动前端目录结构
- 不改动 openspec/specs/ 中已有规格的内容
- 不引入新的构建系统或 CI/CD

## Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | 仅保留 .claude/，其余 AI 工具配置目录加入 .gitignore 并删除 | .qoder/.trae/.codebuddy 内容为空壳或重复，实际开发仅使用 Claude Code |
| 2 | spec/ 整体删除，内容合并到 openspec/specs/ | 双轨制造成认知负担，openspec/specs/ 是项目当前的活跃规格系统 |
| 3 | SQL 迁移脚本移入 backend/.../resources/db/migration/ 而非新增位置 | 与 Spring Boot + Flyway 的标准目录约定一致 |
| 4 | 根层 Dockerfile 直接删除而非注释保留 | git 历史可追溯，无引用的死代码应直接清理 |
| 5 | 文档地图放在 README.md 的"项目结构"章节前 | 开发者阅读 README 时最先看到的段落之一，建立文档导航直觉 |

### 关于 spec/ 迁移的详细方案

spec/ 下现有内容：
- `spec/Me2AI/需求描述.md` — 原始用户需求
- `spec/Me2AI/技术约束.md` — 技术约束（已由 CLAUDE.md 覆盖）
- `spec/Me2AI/任务规划.md` — 人类任务规划（标记为 AI 不修改）
- `spec/AI2AI/后端架构信息.md` — 后端架构说明
- `spec/AI2AI/前端架构信息.md` — 前端架构说明
- `spec/AI2AI/协议和数据.md` — API 和数据库设计

迁移目标：
- `需求描述.md` → `openspec/specs/requirements/`（如该目录不存在则创建）
- `技术约束.md` → 内容已由 `CLAUDE.md` 覆盖，不迁移，直接废弃
- `任务规划.md` → 直接废弃（历史任务清单）
- 其他 AI2AI 文档 → `openspec/specs/architecture/`

### 关于 SQL 迁移脚本的处理

现有 `docs/sql/` 下的迁移脚本：
- V20260517__create_ai_analysis_config_table.sql — 创建 ai_analysis_config 表

检查 `backend/aick-mmp-central/src/main/resources/db/` 目录是否存在，如不存在则创建 `migration/` 子目录，将脚本移入。`docs/sql/` 目录保留但改为仅存放数据库设计说明（ER 图等），不放具体 DDL。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 清理 AI 工具配置目录后，其他开发者若使用 .cursor/.codebuddy 会丢失配置 | 先在 .gitignore 中排除，在 commit 消息中注明移除内容和原因 |
| spec/ 被删除后，旧链接/引用失效 | 保留一个 spec/README.md 临时代替，指向 openspec/specs/ 对应位置 |
| SQL 脚本迁移后若 Flyway checksum 不匹配可能报错 | 迁移脚本内容不变（仅文件位置变化），checksum 不依赖路径；或使用 `mvn flyway:repair` |
| 根层 Dockerfile 可能被外部 CI 引用 | 删除前需 grep CI 配置文件确认无引用 |
