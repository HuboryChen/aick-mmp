## Why

登录成功后，由于 `setUserInfo()` 状态更新是异步的，而 `navigate('/dashboard')` 是同步执行的，导致跳转发生时 `userInfo` 状态尚未更新，Sidebar 和 Header 组件不会被渲染。用户看到的是没有侧边栏的 Dashboard 页面。

刷新浏览器后，由于重新执行了 `checkAuth()` 从服务端验证 token 并设置 `userInfo`，界面恢复正常。

## What Changes

- 修改 `useAuth` hook，添加 `isLoggingIn` 状态追踪登录过程
- 修改 `Login.js`，在状态更新完成后再执行页面跳转
- 确保登录成功后整个布局（包括 Sidebar、Header）正确渲染

## Capabilities

### New Capabilities

- `login-redirect-sync`: 登录跳转同步机制，确保状态更新完成后再导航

### Modified Capabilities

- 无

## Impact

- **前端**: `frontend/src/hooks/useAuth.js`, `frontend/src/pages/Login.js`
