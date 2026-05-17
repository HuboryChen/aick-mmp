## 1. 修正不合规的迁移脚本命名

- [x] 1.1 将 `V2__add_recording_storage_fields.sql` 重命名为 `V20260401__add_recording_storage_fields.sql`
- [x] 1.2 将 `V5__Create_regions_table.sql` 重命名为 `V20260401__create_regions_table.sql`

## 2. 生成完整 DDL 快照

- [x] 2.1 读取所有迁移脚本，提取 CREATE TABLE 语句
- [x] 2.2 按模块（shared / central）组织表结构
- [x] 2.3 写入 `backend/.../resources/db/schema-full.sql`

## 3. 新增工作流文档

- [x] 3.1 创建 `backend/.../resources/db/README.md`，说明"先更新 full.sql，再新增增量迁移"的工作流

## 4. 验证

- [x] 4.1 确认重命名后无残留引用（grep V2__/V5__）
- [x] 4.2 确认 schema-full.sql 包含全部 41 张表且语法正确
