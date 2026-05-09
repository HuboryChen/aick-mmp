## Context

登录成功后，`useAuth` hook 中的 `setUserInfo()` 是异步操作，而 `navigate()` 是同步操作。由于 React 状态更新批处理机制，状态更新不会立即完成，导致跳转发生时 `userInfo` 仍为 `null`，`isAuthenticated` 为 `false`。

当前代码结构：
- `Login.js`: 登录成功后调用 `login()` → `navigate('/dashboard')`
- `useAuth.js`: `login()` 函数中 `setUserInfo(user)` 在 `navigate()` 之后才完成
- `App.js`: 根据 `isAuthenticated` 条件渲染 Sidebar 和 Header

## Goals / Non-Goals

**Goals:**
- 确保登录成功后，Sidebar 和 Header 正确渲染
- 用户体验：登录后立即看到完整的 Dashboard 布局

**Non-Goals:**
- 不修改现有的认证流程（token 存储、验证逻辑）
- 不修改路由结构

## Decisions

### 方案：setTimeout 延迟跳转

**选择原因：**
- 实现最简单，只需修改 `Login.js`
- 利用 `setTimeout(..., 0)` 让 React 先完成当前渲染周期（setUserInfo + re-render），再执行跳转
- 无需新增状态或依赖项

**实现：**
```javascript
// Login.js - onFinish
const result = await login(values);
if (result.success) {
  message.success('登录成功！');
  setTimeout(() => navigate('/dashboard'), 0);
}
```

**原理：**
```
┌─────────────────────────────────────────────────────────┐
│  setTimeout(..., 0) 的执行时序                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. login() 完成，调用 setUserInfo(user)                │
│  2. React 将 setTimeout 回调放入队列（异步）              │
│  3. 当前 render cycle 完成，DOM 更新                     │
│  4. Sidebar/Header 渲染                                 │
│  5. setTimeout 回调执行，navigate('/dashboard')        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**替代方案考虑：**

| 方案 | 优点 | 缺点 |
|------|------|------|
| `isLoggingIn` + useEffect | 显式状态管理 | 需改两个文件 |
| Promise 链式调用 | 逻辑清晰 | 需改 useAuth 返回值 |
| setTimeout 延迟跳转 | 改动最小 | 语义上不够"纯" |

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| `useEffect` 依赖项配置错误 | 使用 `userInfo` 作为依赖，确保精确触发 |
| 重复跳转 | 使用 `if (userInfo)` 条件判断防止重复 |

## Open Questions

- 无
