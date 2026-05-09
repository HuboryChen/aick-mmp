## Context

摄像头管理系统采用软删除策略，通过 `deleted_at` 时间戳标记已删除记录。但 `Camera` 实体定义了两个软删除字段：

```java
@Builder.Default
@Column(name = "is_deleted")
private Boolean isDeleted = false;    // 未被使用

@Column(name = "deleted_at")
private LocalDateTime deletedAt;       // 被设置
```

`CameraServiceImpl.deleteCamera()` 只设置了 `deletedAt`，而部分 Repository 查询方法使用 `isDeleted=false` 条件过滤，导致查询结果不一致。

此外，前端批量删除调用 `/cameras/batch-delete`，但后端实际端点是 `/cameras/batch-operation`。

## Goals / Non-Goals

**Goals:**
- 确保删除操作后摄像头不再出现在任何列表查询中
- 统一软删除字段的使用规范
- 修复批量删除 API 路径不匹配

**Non-Goals:**
- 不改变软删除字段结构（保留两个字段）
- 不实现硬删除（永久删除）功能
- 不修改其他关联实体的删除逻辑

## Decisions

### Decision 1: 统一设置两个软删除字段

**选择**: 修改 `CameraServiceImpl.deleteCamera()` 同时设置 `isDeleted=true` 和 `deletedAt`

**理由**: 保持向后兼容，现有代码可能使用任一字段查询。虽然 `deletedAt IS NOT NULL` 理论上足够，但设置 `isDeleted=true` 可以明确表达意图并防止边界情况。

**替代方案**:
- 移除 `is_deleted` 字段（风险高：可能影响其他模块）
- 只使用 `is_deleted` 字段（需要迁移数据，改动大）

### Decision 2: 修复前端 API 路径

**选择**: 修改前端 `api.js` 批量删除端点为 `/batch-operation`

**理由**: 后端控制器已实现 `/batch-operation` 端点，前端应适配。

**替代方案**:
- 修改后端添加 `/batch-delete` 端点（增加冗余，不推荐）
- 使用批量删除的批量循环单个删除（性能差）

### Decision 3: 添加删除结果验证

**选择**: 在 Service 层添加删除后验证，确认 `deleted_at` 已被持久化

**理由**: 204 响应只能证明方法执行成功，不能保证数据已写入数据库。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 事务回滚但前端显示成功 | 高 | 添加异常处理和日志 |
| 缓存导致列表未刷新 | 中 | 前端添加时间戳参数或强制刷新 |
| 其他模块依赖 `isDeleted` 字段 | 低 | 代码审查确认所有引用点 |

## Open Questions

1. 是否需要添加删除后的确认查询？
2. 批量删除是否需要事务支持（部分成功/部分失败）？
