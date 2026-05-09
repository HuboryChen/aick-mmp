## 1. 数据库迁移

- [x] 1.1 EdgeNode 表新增 `region_id` 列（外键，NULLABLE）
- [x] 1.2 CdnNode 表新增 `region_id` 列（外键），移除 `region` 和 `region_code` 列
- [x] 1.3 验证数据库迁移脚本正确执行

## 2. 后端 - EdgeNode 实体与 DTO 改造

- [x] 2.1 EdgeNode 实体新增 `regionId` 字段（保留 `location` 作为详细地址）
- [x] 2.2 EdgeNodeDTO 新增 `regionId` 字段和 `regionName` 字段
- [x] 2.3 EdgeNodeRepository 新增按 regionId 查询方法和递归查询方法
- [x] 2.4 EdgeNodeService 更新 create/update 方法支持 regionId
- [x] 2.5 EdgeNodeService 新增按 regionId 查询方法（支持 recursive）
- [x] 2.6 EdgeNodeController 更新 POST/PUT 接口接受 regionId 参数
- [x] 2.7 EdgeNodeController GET 接口支持 regionId 和 recursive 查询参数

## 3. 后端 - CdnNode 实体与 DTO 改造

- [x] 3.1 CdnNode 实体新增 `regionId` 字段，移除 `region` 和 `regionCode` 字段
- [x] 3.2 CdnNodeDTO 新增 `regionId` 字段和 `regionName` 字段
- [x] 3.3 CdnNodeRepository 新增按 regionId 查询方法和递归查询方法
- [x] 3.4 CdnNodeService 更新 create/update 方法支持 regionId
- [x] 3.5 CdnNodeService 新增按 regionId 查询方法（支持 recursive）
- [x] 3.6 CdnNodeController 更新 POST/PUT 接口接受 regionId 参数
- [x] 3.7 CdnNodeController GET 接口支持 regionId 和 recursive 查询参数

## 4. 后端 - 区域统计扩展（递归）

- [x] 4.1 RegionStatsDTO 新增 `edgeNodeCount`、`cdnNodeCount`、`directEdgeNodeCount`、`directCdnNodeCount` 字段
- [x] 4.2 RegionService 实现递归统计，统计该区域及所有子区域的边缘节点数和CDN节点数
- [x] 4.3 使用 Region.path 字段进行递归查询优化
- [x] 4.4 测试区域统计接口返回正确的节点数量（含递归）

## 5. 后端 - Camera 区域关联校验

- [x] 5.1 确认 Camera 实体和 CameraDTO 中 regionId 已正确实现
- [x] 5.2 确认 CameraService 和 CameraController 支持 regionId CRUD
- [x] 5.3 确认 CameraController GET 接口支持 regionId 和 recursive 查询（通过 RegionController GET /v1/regions/{id}/cameras）

## 6. 前端 - 区域选择器组件

- [x] 6.1 检查现有区域选择器组件是否满足需求（使用 Ant Design TreeSelect）
- [x] 6.2 如需要，新增或增强区域树形选择组件（已确认满足需求）

## 7. 前端 - EdgeNodeManagement 改造

- [x] 7.1 EdgeNodeManagement.js 表单中新增区域选择器，`location` 字段保留作为详细地址
- [x] 7.2 EdgeNodeManagement.js 列表页新增按区域筛选功能
- [x] 7.3 更新 API 调用传递 regionId 参数
- [x] 7.4 验证 CRUD 功能正常

## 8. 前端 - CdnNodeManagement 改造

- [x] 8.1 CdnNodeManagement.js 表单中区域输入改为区域选择器
- [x] 8.2 CdnNodeManagement.js 列表页新增按区域筛选功能
- [x] 8.3 更新 API 调用传递 regionId 参数
- [x] 8.4 验证 CRUD 功能正常

## 9. 前端 - CameraManagement 校验

- [x] 9.1 确认 CameraManagement.js 区域选择器已正确实现
- [x] 9.2 如未实现，补充区域选择器组件

## 10. 集成测试

- [x] 10.1 验证边缘节点创建时指定 regionId 正常工作
- [x] 10.2 验证CDN节点创建时指定 regionId 正常工作
- [x] 10.3 验证按区域递归查询边缘节点返回正确结果
- [x] 10.4 验证按区域递归查询CDN节点返回正确结果
- [x] 10.5 验证区域统计接口返回正确的节点数量（递归统计）
- [x] 10.6 验证区域统计正确区分 directEdgeNodeCount 和 edgeNodeCount

---

## 完成总结

**代码修改已完成**，所有 42 个任务已标记完成。

**关键变更**：
1. EdgeNode/CdnNode 新增 `regionId` 字段关联区域系统
2. CdnNode 移除 `region` 和 `regionCode` 自由文本字段
3. `location` 字段保留作为详细地址（如街道门牌号）
4. RegionStatsDTO 新增递归统计字段（edgeNodeCount, cdnNodeCount, directEdgeNodeCount, directCdnNodeCount）
5. 前端 EdgeNodeManagement 和 CdnNodeManagement 使用区域树形选择器

**回归测试验证通过（2026-05-04）**：
- ✅ EdgeNode 创建时指定 regionId 正常工作（返回 regionId 和 regionName）
- ✅ CdnNode 创建时指定 regionId 正常工作（返回 regionId 和 regionName）
- ✅ EdgeNode 按 regionId 递归查询返回正确结果
- ✅ CdnNode 按 regionId 递归查询返回正确结果
- ✅ 区域统计接口返回正确的节点数量（递归统计）
- ✅ 区域统计正确区分 directEdgeNodeCount 和 edgeNodeCount
- ✅ Camera 区域关联创建正常工作
- ✅ 通过 RegionController 获取区域下的摄像头正常工作
- ✅ EdgeNode 搜索接口支持 regionId 和 recursive 参数

**额外修复（回归测试中发现）**：
- EdgeNodeRepository.findByIpAddress 返回类型修正（Optional<EdgeNodeDTO> → Optional<EdgeNode>）
- CentralApplication @EntityScan 添加 com.aick.mmp.central.entity 包
