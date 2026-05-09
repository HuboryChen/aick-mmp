# merge-system-app-key

## Why

当前系统应用(SystemApp)和系统API密钥(ApiKey)采用分离管理模式，创建系统应用后需要额外步骤为其创建密钥，增加了管理复杂度。用户希望系统应用和密钥一体化管理，同时保持用户密钥和系统密钥的独立管理。

## What Changes

1. **系统应用创建时直接生成密钥**
   - 创建系统应用时自动生成 `app_key` 和 `app_secret`
   - 首次创建时返回密钥信息（仅显示一次）
   - 简化管理员操作流程

2. **重构系统应用数据模型**
   - 将密钥字段(`app_key`, `app_secret`)合并到 `SystemApp` 表
   - 移除独立的 `ApiKey` 表中 `SYSTEM` 类型的记录
   - `ApiKey` 表仅保留 `USER` 类型的密钥

3. **调整API和Service层**
   - 简化 `SystemAppService`，内嵌密钥管理逻辑
   - 调整 `ApiKeyService`，仅处理用户密钥
   - 更新相关Controller的权限和路径

4. **数据迁移**
   - 迁移现有系统应用的密钥数据到 `SystemApp` 表
   - 清理 `ApiKey` 表中 `SYSTEM` 类型的遗留数据

## Capabilities

### New Capabilities
- `system-app-with-key`: 系统应用与密钥一体化创建和管理能力

### Modified Capabilities
- `api-key-management`: API密钥管理能力调整，仅管理用户密钥

## Impact

- **数据库**: 修改 `system_apps` 表结构，迁移 `api_keys` 表数据
- **后端Service**: 重构 `SystemAppService`、`ApiKeyService`
- **后端Controller**: 调整 `SystemAppController`、`ApiKeyController` 路由和权限
- **API协议**: 调整系统应用相关API响应格式
- **前端**: 调整系统应用管理页面，移除独立的系统密钥管理页面
