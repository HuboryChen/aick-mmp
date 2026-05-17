## Why

当前数据库 schema 管理依赖 Hibernate `ddl-auto: update`，21 个 SQL 迁移脚本是手动维护的历史参考，与实体类无强制一致性。新开发者无法通过一份完整 DDL 快速了解全部表结构，且命名规范不统一（V2__、V5__ 混用），存在路径引用断裂风险。

## What Changes

- 创建 `schema-full.sql` 作为完整 DDL 单文件，一张表对应一段 `CREATE TABLE`
- 统一切换脚本命名规范：废弃 `V2__` `V5__` 格式，全部改为 `VYYYYMMDD__` 格式
- 新增 `db/README.md` 说明"先更新 full.sql，再新增增量迁移"的工作流
- 非破坏性变更——不修改任何实体或业务逻辑

## Capabilities

### New Capabilities
- `db-schema-management`: 数据库 schema 版本管理与完整 DDL 快照机制

### Modified Capabilities

（无）

## Impact

- `backend/.../resources/db/migration/`: 新增 `schema-full.sql`，2 个旧命名文件重命名
- `backend/.../resources/db/`: 新增 `README.md`
- 不涉及代码修改、API 变更或配置修改
