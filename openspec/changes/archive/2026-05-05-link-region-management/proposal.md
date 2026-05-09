## Why

当前系统中，摄像头管理（Camera）已通过 `region_id` 与区域管理系统关联，但边缘节点（EdgeNode）和CDN节点（CdnNode）的"所属地区"功能使用的是自由文本字段（`location`、`region`、`region_code`），未与系统区域管理进行关联。这导致：

1. **数据不一致**：边缘节点和CDN节点无法参与基于区域的层级统计与筛选
2. **管理不便**：无法通过区域树快速筛选和管理节点
3. **扩展性受限**：区域层级变更时无法级联影响相关节点

本次变更将建立边缘节点、CDN节点与区域管理的完整关联，实现基于区域的数据聚合与可视化。

## What Changes

1. **EdgeNode 实体改造**
   - 新增 `region_id` 字段替代 `location` 文本字段
   - 更新 DTO、Service、Controller 层以支持区域关联
   - 前端改为区域选择器（树形下拉）

2. **CdnNode 实体改造**
   - 新增 `region_id` 字段替代 `region` 和 `region_code` 自由文本字段
   - 移除冗余字段 `region_code`（可直接使用 Region.code）
   - 更新 DTO、Service、Controller 层以支持区域关联
   - 前端改为区域选择器

3. **Camera 实体校验**
   - 确认 `region_id` 关联正确性
   - 补充前端区域选择器（如尚未实现）

4. **区域查询功能增强**
   - 边缘节点按区域查询/筛选
   - CDN节点按区域查询/筛选
   - 区域统计接口支持边缘节点和CDN节点数量统计

## Capabilities

### New Capabilities
- `region-node-association`: 边缘节点和CDN节点的区域关联能力，支持区域选择、查询筛选、统计汇总

### Modified Capabilities
- `region-management`: 区域管理能力需要扩展统计接口，增加边缘节点数和CDN节点数的统计
- `camera-management`: 确认前端区域选择器已正确实现（如缺失则补充）

## Impact

**后端影响范围：**
- `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/EdgeNode.java`
- `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/CdnNode.java`
- `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/dto/EdgeNodeDTO.java`
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/` - 边缘节点和CDN节点服务
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/` - 边缘节点和CDN节点控制器
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/` - 数据访问层

**前端影响范围：**
- `frontend/src/pages/EdgeNodeManagement.js` - 边缘节点管理页面
- `frontend/src/pages/CdnNodeManagement.js` - CDN节点管理页面
- `frontend/src/pages/CameraManagement.js` - 摄像头管理页面（校验）
- 共享组件：区域选择树形组件

**数据库：**
- `regions` 表：已有
- `cameras` 表：已有 `region_id`
- `edge_nodes` 表：新增 `region_id` 列
- `cdn_nodes` 表：新增 `region_id` 列，移除 `region`、`region_code` 列

**API 影响：**
- `GET /api/v1/edge-nodes` - 新增 `regionId` 筛选参数
- `GET /api/v1/cdn-nodes` - 新增 `regionId` 筛选参数
- `POST/PUT /api/v1/edge-nodes` - 请求体中 `regionId` 替换 `location`
- `POST/PUT /api/v1/cdn-nodes` - 请求体中 `regionId` 替换 `region`/`regionCode`
- `GET /api/v1/regions/{id}/stats` - 新增 `edgeNodeCount`、`cdnNodeCount` 字段
