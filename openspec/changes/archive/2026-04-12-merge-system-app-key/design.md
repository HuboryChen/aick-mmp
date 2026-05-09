## Context

### 当前状态
系统采用 `SystemApp`（系统应用）和 `ApiKey`（API密钥）分离管理：
- `SystemApp`: 存储应用基本信息（app_key, name, description, owner_type, status, permissions）
- `ApiKey`: 存储密钥信息（access_key, encrypted_secret），通过 `app_id` 关联 `SystemApp`

创建系统应用流程：
1. POST `/system-apps` 创建应用
2. POST `/api-keys/system` 为应用创建密钥

### 问题
- 两步创建流程繁琐
- 管理员需要管理两个实体
- 密钥与应用的关联不够紧密

### 约束
- 用户密钥（USER类型）保持独立管理
- 系统密钥（SYSTEM类型）与系统应用合并
- 需要数据迁移处理现有数据

## Goals / Non-Goals

**Goals:**
- 简化系统应用创建流程，一体化生成密钥
- 系统密钥与系统应用紧密绑定
- 用户密钥管理保持独立
- 数据完整性保证

**Non-Goals:**
- 不修改用户密钥管理流程
- 不改变现有API认证逻辑
- 不涉及边缘节点注册流程的修改

## Decisions

### Decision 1: 将密钥字段合并到 SystemApp 表

**方案A（采用）**: 在 `SystemApp` 表增加 `app_key` 和 `encrypted_secret` 字段
- 优点：减少表关联，查询效率高
- 缺点：需要数据迁移

**方案B（排除）**: 创建新的 `SystemAppCredential` 关联表
- 缺点：增加关联复杂度，与目标不符

### Decision 2: ApiKey 表保留 USER 类型记录

**方案A（采用）**: `ApiKey` 表仅保留 `type=USER` 的记录
- 优点：职责清晰，用户和系统密钥分离
- 迁移成本可控

**方案B（排除）**: 统一删除 `ApiKey` 表
- 缺点：用户密钥管理逻辑需全部重写

### Decision 3: 密钥生成策略

- `app_key`: 使用 `ak_` 前缀 + UUID 格式
- `app_secret`: 使用 `sk_` 前缀 + 32位随机字符串
- 加密存储：`app_secret` 使用 AES-256-GCM 加密（复用现有 `AESEncryptionUtil`）

### Decision 4: API 路由调整

| 原路由 | 新路由 | 说明 |
|--------|--------|------|
| POST `/api-keys/system` | 移除 | 系统密钥创建合并到系统应用 |
| GET `/api-keys/system` | 移除 | 列表查询合并到系统应用 |
| PUT `/api-keys/system/{id}/status` | 移除 | 状态管理合并到系统应用 |
| DELETE `/api-keys/system/{id}` | 移除 | 删除合并到系统应用 |

## Data Model Changes

### system_apps 表变更

```sql
ALTER TABLE system_apps ADD COLUMN app_key VARCHAR(64) UNIQUE;
ALTER TABLE system_apps ADD COLUMN encrypted_secret VARCHAR(256);
ALTER TABLE system_apps ADD COLUMN last_used_at DATETIME;
```

### api_keys 表变更

- 保留 `type=USER` 的记录
- 删除 `type=SYSTEM` 的记录（或标记迁移完成）

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 密钥迁移失败导致系统应用无法认证 | 迁移前备份数据，提供回滚脚本 |
| API兼容性问题 | 保持原有 API 响应格式，版本号递增 |
| 前端页面重构工作量大 | 分阶段实施，先保证后端逻辑正确 |
