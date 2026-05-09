## Why

摄像头管理功能中，执行删除操作时前端显示"删除成功"（204响应），但摄像头并未真正从列表中消失。根本原因是软删除实现不完整：`Camera` 实体有两个软删除字段（`is_deleted` 和 `deleted_at`），但删除时只设置了 `deletedAt`，导致查询过滤逻辑与实际数据不一致。同时发现批量删除存在 API 路径不匹配问题。

## What Changes

- 完善 `CameraServiceImpl.deleteCamera()` 方法，同时设置 `isDeleted=true` 和 `deletedAt`
- 修复前端批量删除 API 路径：`/cameras/batch-delete` → `/cameras/batch-operation`
- 确保所有查询方法使用统一的软删除过滤逻辑
- 添加事务边界验证，确保删除操作的原子性

## Capabilities

### New Capabilities
- `camera-soft-delete`: 完整的软删除功能，确保删除的摄像头从列表中正确消失

### Modified Capabilities
- 无（修复现有实现，非新需求变更）

## Impact

- **后端**: `CameraServiceImpl.java` - 删除方法修改
- **前端**: `api.js` - 批量删除 API 路径修正
- **数据库**: 无需变更（`is_deleted` 和 `deleted_at` 列已存在）
- **测试**: 需验证删除后摄像头不再出现在列表中
