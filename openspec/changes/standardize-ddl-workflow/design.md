## Context

当前项目使用 `spring.jpa.hibernate.ddl-auto: update`，schema 由 JPA 注解驱动。`resources/db/migration/` 下有 21 个 SQL 脚本，其中 2 个使用 `V2__`、`V5__` 命名（与 `VYYYYMMDD__` 主流格式不一致）。缺少完整 DDL 快照，开发者需要通过翻阅多个 SQL 文件或阅读实体类来理解完整 schema。

## Goals / Non-Goals

**Goals:**
- 生成一份完整的 `schema-full.sql`，包含全部 34 张表的 `CREATE TABLE` 语句
- 重命名 2 个不合规的迁移脚本（V2__ → V20260401__，V5__ → V20260401__）
- 新增 `db/README.md` 描述 DDL 工作流

**Non-Goals:**
- 不切换 migration 框架（仍使用 Hibernate ddl-auto，不引入 Flyway）
- 不修改任何 JPA 实体或业务代码
- 不修改数据库连接配置

## Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | `schema-full.sql` 从已有 SQL 脚本合并生成而非实体逆向导出 | (a) 迁移脚本记录了明确的 DDL 类型和约束，(b) 现有脚本经过了验证，(c) 实体类上的 JPA 注解无法完整表达索引名、外键名等细节 |
| 2 | 不合规脚本统一重命名为 `V20260401__` | V2/V5 无对应日期信息，选取相近的 2026-04-01 作为合理近似值 |
| 3 | `schema-full.sql` 放在 `db/` 而非 `db/migration/` | 区分全量快照和增量迁移，降低混淆 |
| 4 | 不加 `DROP TABLE IF EXISTS` 前缀 | 保证文档可读性，不使用逆向工程 |

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 合并生成的 full.sql 遗漏约束或索引 | 逐表对照迁移脚本和实体确认 |
| 重命名 V2/V5 后若有外部引用会断裂 | 整个仓库 grep 确认无其他引用（只有 migration/ 内有这些文件） |
| future 版本中开发者忘记更新 full.sql | README 中明确工作流要求，CI 可后续加入校验 |
