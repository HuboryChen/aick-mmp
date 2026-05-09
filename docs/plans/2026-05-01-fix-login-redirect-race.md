# Fix Login Redirect Race Condition

> **For Claude:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task.

**Goal:** 修复登录成功后 Sidebar 和 Header 不显示的问题，通过延迟跳转让 React 完成状态更新和渲染。

**Architecture:** 在 `Login.js` 的 `onFinish` 函数中，使用 `setTimeout(..., 0)` 延迟执行跳转，利用 JavaScript 事件循环机制确保 React 先完成当前渲染周期。

**Tech Stack:** React, React Router, Ant Design

---

## Task 1: 修改 Login.js 登录成功跳转逻辑

**Files:**
- Modify: `frontend/src/pages/Login.js:21-23`

**Step 1: 确认当前代码位置**

当前代码（第 21-23 行）：
```javascript
if (result.success) {
  message.success('登录成功！');
  navigate('/dashboard');
}
```

**Step 2: 修改为延迟跳转**

使用 `setTimeout(..., 0)` 包裹 `navigate` 调用：

```javascript
if (result.success) {
  message.success('登录成功！');
  setTimeout(() => navigate('/dashboard'), 0);
}
```

**Step 3: 手动测试验证**

1. 清除浏览器登录状态（退出登录或清除 cookie）
2. 访问登录页面
3. 输入正确凭据登录
4. **验证**：Dashboard 页面加载时，左侧 Sidebar 和顶部 Header 应立即可见

**Step 4: 验证错误处理不受影响**

1. 输入错误密码
2. **验证**：显示错误提示，不发生跳转

---

## Task 2: 更新 AI2AI 文档（如需要）

**Files:**
- Modify: `spec/AI2AI/前端架构信息.md`（如果其中包含 Login 组件的描述）

**Step 1: 检查是否需要更新**

根据规则，完成任务后需要检查并更新 AI2AI 中的文档。

---

## 完成标准

- [ ] 登录成功后 Sidebar 和 Header 正确显示
- [ ] 错误登录不触发跳转
- [ ] AI2AI 文档已更新（如需要）
