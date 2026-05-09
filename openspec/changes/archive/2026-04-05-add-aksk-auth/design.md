## Context

当前系统认证体系仅支持 JWT 认证方式：
- 用户通过 `/auth/login` 获取 JWT Token
- 后续请求携带 `Authorization: Bearer <token>` Header
- Edge 节点使用数据库中的 `authUsername/authPassword` 字段（代码中未实际使用）

系统需要扩展支持 AK/SK 认证，以满足：
1. 普通用户通过 API 访问系统资源
2. Edge 节点等系统组件的服务级认证

## Goals / Non-Goals

**Goals:**
- 实现用户级 AK/SK：用户可创建个人密钥，继承用户角色权限
- 实现系统级 AK/SK：管理员创建系统应用，系统应用持有密钥用于认证
- 统一认证过滤器：同一 API 同时支持 JWT 和 AK/SK
- Edge 节点自注册：使用 AK/SK 完成自注册流程
- 密钥安全：SK 加密存储，支持启用/禁用、删除操作

**Non-Goals:**
- 不实现 SK 轮换/重置功能（SK 创建后不可更改）
- 不实现 API 级别的细粒度权限控制（权限由系统应用配置决定）
- 不实现 Edge 节点自注册审批流程（注册后直接激活）

## Decisions

### Decision 1: AK/SK 签名协议 - 简化签名

**选择**：简化签名协议（仅对关键操作签名验证）

**原因**：
- 完整签名需对所有请求计算 HMAC，增加实现复杂度
- Edge 节点主要进行心跳上报等高频操作，简化签名降低性能开销
- 系统内部网络环境相对可信，可适当简化

**签名算法**：
```
stringToSign = HTTP_METHOD + "\n" +
               REQUEST_PATH + "\n" +
               TIMESTAMP

Signature = Base64(HMAC-SHA256(stringToSign, SK))
```

**Headers**：
```
X-Access-Key: ak_xxxxxxxxxxxxxxxxxxxxxxxx
X-Signature: dGVzdF90ZXN0X3NpZ25hdHVyZQ==
X-Timestamp: 2026-04-05T17:00:00Z
```

**需要签名验证的操作**：
- `POST /edge/register` - Edge 注册
- `PUT /edge-nodes/{id}/config` - Edge 配置更新

**仅验证 AK 有效的操作**：
- `POST /edge-nodes/{id}/heartbeat` - 心跳上报
- `GET /api/cameras` - 读取操作

---

### Decision 2: SK 加密存储 - AES-256-GCM

**选择**：AES-256-GCM 对称加密

**原因**：
- GCM 模式提供认证加密，同时保证机密性和完整性
- 与 RSA 相比，签名计算效率更高
- 与 AES-CBC 相比，GCM 更安全（防止填充oracle攻击）

**实现**：
- 使用应用配置的 `app-secret-key` 作为主密钥
- SK 加密后存储到 `encrypted_secret` 字段
- 解密后的 SK 缓存在 Redis，TTL 5 分钟

---

### Decision 3: 认证过滤器链设计 - 策略工厂模式

**选择**：UnifiedAuthFilter + AuthenticationStrategyFactory 策略工厂模式

**架构组件**：
```
UnifiedAuthFilter
    └── AuthenticationStrategyFactory
            ├── JwtAuthenticationStrategy (priority=1)
            ├── AkskAuthenticationStrategy (priority=2)
            └── AnonymousAuthenticationStrategy (priority=3)
```

**原因**：
- 策略模式将每种认证方式封装为独立策略，便于扩展和维护
- 工厂模式负责策略的加载、排序和调度
- 认证优先级通过策略的 `priority` 属性控制 (值越小优先级越高)
- 统一过滤器作为入口，协调策略工厂完成认证
- 认证结果统一转换为 `UnifiedPrincipal` 存入 SecurityContext

**认证流程**：
```
1. UnifiedAuthFilter 接收请求
2. 检查公开端点 → 直接放行
3. 检查已有认证 → 跳过认证
4. 调用 AuthenticationStrategyFactory.authenticate(request)
5. 策略工厂按优先级遍历策略:
   a. JwtAuthenticationStrategy.supports() → 检查 Authorization: Bearer
   b. AkskAuthenticationStrategy.supports() → 检查 X-Access-Key
   c. AnonymousAuthenticationStrategy → 作为兜底
6. 策略匹配后执行 authenticate() → 返回 UnifiedAuthenticationToken
7. 结果存入 SecurityContext
```

**实际文件结构**：
```
security/
├── UnifiedAuthFilter.java          # 统一认证过滤器
├── UnifiedPrincipal.java           # 统一认证主体
├── UnifiedAuthenticationToken.java # 统一认证令牌
└── strategy/
    ├── AuthenticationStrategy.java        # 策略接口
    ├── AuthenticationStrategyFactory.java  # 策略工厂
    ├── JwtAuthenticationStrategy.java      # JWT 策略
    ├── AkskAuthenticationStrategy.java    # AK/SK 策略
    └── AnonymousAuthenticationStrategy.java # 匿名策略
```

---

### Decision 4: 权限模型 - 基于系统应用的权限集

**选择**：系统应用持有权限列表，Edge 节点通过关联的系统应用获取权限

**原因**：
- 与用户角色权限解耦
- 便于管理机器对机器的权限
- 支持细粒度的系统级权限控制

**预置权限**：
```
EDGE_REGISTER      - Edge 节点注册
EDGE_HEARTBEAT    - 心跳上报
EDGE_CONFIG_UPDATE - 接收配置更新
```

---

### Decision 5: Edge 节点自注册流程

**选择**：Edge 节点启动时自动注册，中央服务直接激活

**流程**：
```
1. Edge 节点使用预配置的 AK/SK
2. Edge 节点启动时 POST /edge/register
3. Central 验证签名，检查 AK/SK 有效性
4. 创建 EdgeNode 记录，关联 SystemApp
5. 返回注册成功，开始心跳
```

**原因**：
- 简化部署流程，无需人工审批
- 适合内部网络环境
- 管理员可通过禁用 AK/SK 阻止异常节点

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| SK 泄露 | 高 | SK 仅创建时显示一次；支持立即禁用；日志记录密钥使用 |
| 时钟偏差 | 中 | 签名验证容忍 ±5 分钟时间戳偏差 |
| Redis 缓存穿透 | 中 | 签名验证前检查缓存；连续解密失败拒绝请求 |
| AK/SK 暴力破解 | 低 | 使用强随机数生成 AK/SK；无返回错误信息区分 |
| Edge 节点克隆攻击 | 低 | 禁用 AK/SK 后清除 Redis 缓存；心跳超时自动离线 |

## Migration Plan

### Phase 1: 数据库迁移
1. 创建 `system_apps` 表
2. 创建 `api_keys` 表
3. 修改 `edge_nodes` 表（添加 app_id，移除 auth_username/auth_password）

### Phase 2: 核心功能实现
1. 实现 AES-256-GCM 加密工具类
2. 实现 ApiKeyService（创建、验证、缓存）
3. 实现 SystemAppService
4. 实现 CombinedAuthFilter
5. 实现 AK/SK 签名验证

### Phase 3: API 端点实现
1. 实现 SystemAppController
2. 实现 ApiKeyController
3. 实现 EdgeRegisterController

### Phase 4: Edge 节点适配
1. Edge 节点配置 AK/SK
2. 实现 Edge 侧签名逻辑
3. 修改心跳/注册请求

### Phase 5: 测试验证
1. 单元测试
2. 集成测试
3. 安全测试

## Open Questions

1. **签名容差时间**：±5 分钟是否合适？是否需要可配置？
2. **SK 加密主密钥**：使用应用配置中的哪个密钥？是否需要独立管理？
3. **Edge 节点注册后状态**：默认 ONLINE 还是待激活？
4. **API 限流**：是否需要为 AK/SK 添加独立的限流策略？
