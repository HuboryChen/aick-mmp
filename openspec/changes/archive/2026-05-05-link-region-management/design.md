## Context

当前系统架构中：
- **Region（区域）**：树形层级结构，支持多级嵌套，具有 `id`, `code`, `name`, `parent_id`, `level`, `path` 等字段
- **Camera（摄像头）**：已通过 `region_id` 与区域关联
- **EdgeNode（边缘节点）**：使用自由文本 `location` 字段，未与区域管理关联
- **CdnNode（CDN节点）**：使用自由文本 `region` 和 `region_code` 字段，未与区域管理关联

本次改造的核心是将边缘节点和CDN节点纳入区域管理体系，实现：
1. 数据完整性：节点与区域的关系由外键约束保证
2. 查询灵活性：支持按区域树筛选、聚合节点数据
3. 前端一致性：统一使用区域选择器替代文本输入

## Goals / Non-Goals

**Goals:**
- EdgeNode 新增 `region_id` 字段，与 `location` 共存（`location` 作为详细地址）
- CdnNode 新增 `region_id` 字段替代 `region` 和 `region_code`
- 前端 EdgeNodeManagement、CdnNodeManagement 使用区域树选择器
- 区域统计接口支持递归统计子区域节点数

**Non-Goals:**
- 不修改区域的树形结构和层级逻辑
- 不修改 Camera 的 region_id 关联（仅校验现有实现）
- 不迁移遗留的 `location`/`region` 数据到新字段
- 不实现区域的权限控制

## Decisions

### Decision 1: EdgeNode 数据模型

**选择方案**：在 EdgeNode 中添加 `region_id` 外键，保留 `location` 字段作为详细地址描述（如"XX街道XX号"）

**理由**：
- `location` 保留用于存储具体地址信息，是业务必需字段
- `region_id` 用于区域层级管理和统计
- 两者共存，互不干扰

### Decision 2: CdnNode 字段清理

**选择方案**：新增 `region_id` 外键，移除 `region` 和 `region_code` 自由文本字段

**理由**：
- `region` 和 `region_code` 与 Region 表功能重复
- 统一使用 `region_id` 简化数据模型

### Decision 3: 遗留数据处理

**选择方案**：不迁移遗留数据，新增字段允许 NULL

**理由**：
- 遗留的 location 文本难以自动匹配到对应区域
- 由用户在编辑时手动选择区域
- 降低迁移复杂性和数据风险

### Decision 4: 区域统计递归策略

**选择方案**：区域统计接口默认返回递归统计（包含所有子区域节点数）

**理由**：
- 递归统计更符合业务需求（如"华东区"应包含所有子区域的节点）
- 前端可直接使用，无需额外计算

## Risks / Trade-offs

**[风险]** 遗留数据无区域关联
- **缓解**：新增字段允许 NULL，不影响现有数据；用户可手动补充区域关联

**[风险]** API 兼容性
- **缓解**：新增可选字段 `regionId`，旧版 API 仍可正常工作

**[风险]** 前端区域选择器缺失
- **缓解**：如无现成组件，需在 specs 中明确要求新增

## Migration Plan

1. **Phase 1: 后端改造**
   - 新增 `region_id` 字段（允许 NULL）
   - 移除 CdnNode 的 `region` 和 `region_code` 字段
   - 部署后端代码

2. **Phase 2: 前端改造**
   - EdgeNodeManagement: 表单中新增区域选择器，`location` 保留
   - CdnNodeManagement: 表单中区域输入改为区域选择器
   - 更新 DTO 绑定

3. **Phase 3: 验证**
   - 验证 CRUD 功能正常
   - 验证区域统计（递归）正确
