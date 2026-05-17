## 1. 根目录 AI 工具配置清理

- [x] 1.1 将 .qoder/、.trae/、.codebuddy/ 加入 .gitignore
- [x] 1.2 删除 .qoder/、.trae/、.codebuddy/ 目录及其内容
- [x] 1.3 在 README 中记录配置目录变更说明

## 2. 废弃旧的 spec/ 目录

- [x] 2.1 创建 openspec/specs/requirements/ 并将 spec/Me2AI/需求描述.md 移入
- [x] 2.2 创建 openspec/specs/architecture/ 并将 spec/AI2AI/ 下文档移入
- [x] 2.3 在 openspec/specs/ 对应位置添加 README.md 说明原文来源
- [x] 2.4 删除 spec/ 目录

## 3. 统一 SQL 迁移脚本位置

- [x] 3.1 确认 backend/aick-mmp-central/src/main/resources/db/migration/ 目录是否存在，不存在则创建
- [x] 3.2 将 docs/sql/ 下的 3 个 SQL 脚本移入 migration/ 目录
- [x] 3.3 更新 docs/sql/README.md 说明该目录仅保留数据库设计文档，迁移脚本已移至 backend

## 4. 修复 Janus 配置路径断裂

- [x] 4.1 将 docker-compose.yml 中 `./janus/janus.cfg` 改为 `./media-servers/janus/janus.cfg`（已验证路径已正确）

## 5. 清理根层 Dockerfile

- [x] 5.1 grep CI/CD 配置文件确认无外部引用，删除根层 Dockerfile
- [x] 5.2 将 Dockerfile 中仍有价值的代码段（如构建缓存策略）归档到 docs/ 或注释

## 6. README 文档地图

- [x] 6.1 在 README.md 的"项目结构"章节前增加文档地图表格

## 7. 验证

- [x] 7.1 确认 docker-compose config 语法有效
- [x] 7.2 确认 spec/ 已废弃且在仓库中无残留引用
- [x] 7.3 在 README 文档地图中注明配置目录变更，告知开发者
