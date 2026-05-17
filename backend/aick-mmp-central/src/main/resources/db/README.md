# 数据库 DDL 管理流程

## 工作流

每次数据库 schema 变更（新增表、修改列、添加索引等），需要按以下顺序执行：

### 第一步：更新完整快照

修改 `schema-full.sql`，确保它反映变更后的完整数据库结构。

### 第二步：新增增量迁移

在 `migration/` 目录下创建新的迁移脚本，只包含本次的增量变更。

## 文件说明

| 文件 | 用途 | 更新时机 |
|------|------|----------|
| `schema-full.sql` | 完整 DDL 快照，所有 `CREATE TABLE` 的单一数据源 | schema 每次变更时先更新 |
| `migration/VYYYYMMDD__*.sql` | 增量迁移脚本，只包含本次变更 | 更新完 full.sql 后新增 |

## 命名规范

增量迁移脚本统一使用 `VYYYYMMDD__<kebab-description>.sql` 格式：

- `VYYYYMMDD__` — 创建脚本时的日期（如 `V20260517__`）
- 描述部分使用 kebab-case（如 `create_alert_tables`）

## 原则

- **先更新 full.sql，再新增迁移脚本** — 确保完整快照始终是最新的
- 迁移脚本只做增量变更（`ALTER TABLE`、`CREATE TABLE` 等）
- schema 的快照文件不包含 `DROP TABLE IF EXISTS`，保持可读性
- 不需要修改已有的迁移脚本
