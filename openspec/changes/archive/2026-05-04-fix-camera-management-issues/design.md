## Context

摄像头管理模块是边缘计算视频监控系统的核心组件。当前代码存在多个严重缺陷：

1. **逻辑运算符优先级错误**：`CameraServiceImpl.calculateNodeWeight()` 中的表达式 `cpuUsage == null || cpuUsage < 80 && memoryUsage == null || memoryUsage < 85` 由于 `&&` 优先级高于 `||`，导致节点选择逻辑错误

2. **认证注解格式错误**：`@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR)")` 缺少右引号，权限验证失效

3. **软删除未实现**：虽然实体有 `isDeleted` 字段，但实际使用硬删除

4. **代码重复**：`CameraServiceImpl` 和 `EdgeNodeFailoverServiceImpl` 中存在重复的权重计算逻辑

## Goals / Non-Goals

**Goals:**
- 修复所有严重安全问题（逻辑错误、认证漏洞）
- 实现软删除功能，保护数据完整性
- 消除重复代码，提高可维护性
- 优化查询性能，解决 N+1 问题

**Non-Goals:**
- 不重构整个系统架构
- 不修改现有的 API 接口契约（除了状态码）
- 不实现完整的审计日志功能

## Decisions

### Decision 1: 软删除实现方式

**选择方案 B: 使用 `deletedAt` 时间戳**

| 方案 | 优点 | 缺点 |
|------|------|------|
| A: `isDeleted` 布尔字段 | 简单 | 无法知道删除时间 |
| B: `deletedAt` 时间戳 | 能追踪删除时间 | 查询需多一个条件 |
| C: 双字段 | 最完整 | 复杂，存储冗余 |

**理由**: 方案 B 平衡了功能性和简单性，同时便于后续扩展恢复功能。

### Decision 2: 权重计算服务抽取

**创建 `NodeWeightCalculator` 共享服务**

```java
@Service
public class NodeWeightCalculator {
    public double calculateWeight(EdgeNode node, Double cpuUsage, Double memoryUsage) {
        // 修复后的逻辑
    }
}
```

**理由**: 消除重复代码，统一计算逻辑，便于后续维护和扩展。

### Decision 3: 查询优化策略

**使用 `@EntityGraph` 解决 N+1 问题**

```java
@EntityGraph(attributePaths = {"edgeNode", "region"})
List<Camera> findAllByIsDeletedFalse();
```

**理由**: Spring Data JPA 原生支持，无需修改查询方法签名，侵入性小。

## Risks / Trade-offs

- **[风险]** 软删除影响现有查询 → **缓解**: 确保 Repository 层正确处理软删除条件
- **[风险]** 数据库迁移需要时间 → **缓解**: 使用向后兼容的 ALTER TABLE 添加 nullable 字段
- **[权衡]** 软删除增加存储成本 → 当前数据量下可接受

## Migration Plan

1. **Phase 1**: 添加数据库字段（向后兼容）
   ```sql
   ALTER TABLE cameras ADD COLUMN deleted_at TIMESTAMP NULL;
   ```

2. **Phase 2**: 部署修复代码（向后兼容）
   - 修复逻辑运算符优先级
   - 修复认证注解
   - 添加软删除查询条件

3. **Phase 3**: 切换到软删除（可选）
   - 修改删除方法从 hard delete 改为 soft delete
   - 添加恢复功能

**回滚策略**: Phase 1/2 可直接回滚代码；Phase 3 需数据迁移脚本。

## Open Questions

1. 是否需要保留硬删除的能力（管理员强制删除）？
2. 软删除数据的保留期限是多久？
3. 是否需要实现定时清理已删除数据的任务？
