## Why

项目根目录存在大量 AI 工具配置目录（5 个活跃工具目录占用 40% 根目录项）、两套规格文档系统（spec/ + openspec/specs/）未同步，以及多处路径引用不一致（Janus 配置路径断裂、Dockerfile 无引用），导致新成员 onboarding 时认知负担高，且存在真实 bug（Janus 容器挂载失败）。

## What Changes

- **清理根目录噪声**：移除/忽略不再活跃的 AI 工具配置目录（.qoder, .trae, .codebuddy），仅保留 .claude/
- **统一规格文档体系**：废弃旧的 spec/ 目录，将其内容迁移到 openspec/specs/，删除 spec/
- **统一 SQL 迁移脚本位置**：将 docs/sql/ 下脚本移至 backend/aick-mmp-central/src/main/resources/db/migration/
- **修复 Janus 配置路径断裂**（bug fix）：docker-compose.yml 中的 `./janus/janus.cfg` 改为 `./media-servers/janus/janus.cfg`
- **移除无引用的根层 Dockerfile**：确认无 CI/CD 引用后删除
- **在 README 中添加文档地图**：帮助开发者快速定位各类文档

## Capabilities

### New Capabilities

（无 — 本次变更为重构和清理，不引入新的产品能力）

### Modified Capabilities

（无 — 未改变任何产品行为的 spec-level 需求）

## Impact

- `docker-compose.yml`：Janus 配置文件路径修正
- `spec/`：整体废弃并迁移到 openspec/specs/
- `Dockerfile`（根层）：删除
- `.gitignore`：新增忽略规则
- `docs/sql/`：迁移到 `backend/.../resources/db/migration/`
- `README.md`：增加文档地图
