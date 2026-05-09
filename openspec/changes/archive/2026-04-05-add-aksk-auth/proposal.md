## Why

当前系统仅支持基于 JWT 的用户认证，无法满足以下场景：
1. **普通用户调用 API**：用户希望通过 API 访问系统资源，而非仅通过前端
2. **系统间认证**：Edge 节点等系统组件需要以服务身份认证，而非模拟用户

现有 Edge 节点数据库中存在 `authUsername/authPassword` 字段，但代码中未实际使用，存在设计不一致问题。

## What Changes

### 新增功能
- **用户级 AK/SK**：普通用户可创建个人 AK/SK，用于 API 认证，继承用户角色权限
- **系统级 AK/SK**：管理员创建系统应用，为系统应用分配 AK/SK，用于 Edge 节点等系统间认证
- **统一认证过滤器**：同一 API 端点同时支持 JWT 和 AK/SK 认证
- **系统应用管理**：管理员可创建、配置系统应用及其权限
- **API 密钥管理**：支持 AK/SK 的创建、启用/禁用、删除操作
- **Edge 节点自注册**：Edge 节点可使用 AK/SK 自注册到中央服务

### 数据库变更
- **新增** `system_apps` 表：存储系统应用配置
- **新增** `api_keys` 表：存储 API 密钥
- **修改** `edge_nodes` 表：移除 `authUsername/authPassword` 字段，新增 `app_id` 关联

### 密钥安全
- SK 使用 **AES-256-GCM** 加密存储
- 解密后 SK 缓存在 Redis，TTL 5 分钟
- 创建 AK/SK 时 SK 一次性显示，之后不可查看

## Capabilities

### New Capabilities
- `api-key-auth`: AK/SK 认证能力，支持签名验证、密钥管理、缓存策略
- `system-app-management`: 系统应用管理能力，支持应用的创建、配置、权限管理
- `edge-self-registration`: Edge 节点自注册能力，支持使用 AK/SK 进行自注册

### Modified Capabilities
- `edge-auth`: Edge 节点认证方式从无认证变更为 AK/SK 认证

## Impact

| 影响范围 | 说明 |
|---------|------|
| **后端模块** | `aick-mmp-central` 新增认证过滤器、API 控制器、服务层 |
| **数据模型** | 新增 `SystemApp`、`ApiKey` 实体，修改 `EdgeNode` 实体 |
| **Edge 节点** | 配置变更，使用 AK/SK 替代原有未使用的认证字段 |
| **数据库** | 新增 2 张表，修改 1 张表 |
| **API** | 新增 `/system-apps`、`/api-keys` 端点，修改 Edge 节点注册端点 |
