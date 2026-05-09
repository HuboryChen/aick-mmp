---
name: 系统应用和API Key管理前端实现
overview: 为系统应用管理和API Key管理功能添加前端页面、路由和菜单项，补全缺失的功能模块
design:
  architecture:
    framework: react
  styleKeywords:
    - Industrial
    - Dark Theme
    - Cyan Accent
  fontSystem:
    fontFamily: system-ui
    heading:
      size: 20px
      weight: 600
    subheading:
      size: 14px
      weight: 500
    body:
      size: 14px
      weight: 400
  colorSystem:
    primary:
      - "#00d4ff"
      - "#0099cc"
    background:
      - "#0f1419"
      - "#1a2332"
      - "#232f3e"
    text:
      - "#f0f6fc"
      - "#8b949e"
      - "#64748b"
    functional:
      - "#52c41a"
      - "#fa8c16"
      - "#ff4d4f"
todos:
  - id: create-apikey-page
    content: 创建 ApiKeyManagement.js 页面组件（Tab布局：我的密钥 + 系统密钥）
    status: completed
  - id: add-routes
    content: 在 App.js 中注册 /system-apps 和 /api-keys 路由
    status: completed
    dependencies:
      - create-apikey-page
  - id: add-menu-items
    content: 在 Sidebar.js 菜单中添加"系统应用"和"API管理"菜单项
    status: completed
  - id: build-verify
    content: 重新构建前端并验证功能
    status: completed
    dependencies:
      - create-apikey-page
      - add-routes
      - add-menu-items
---

## 产品概述

完善多区域视频监控平台的前端管理功能，补充系统应用管理和 API Key 管理的前端界面，使其完整可用。

## 核心功能

### 1. 系统应用管理界面（已完成）

- ✅ 系统应用列表展示（分页、搜索、筛选）
- ✅ 创建/编辑/删除系统应用
- ✅ 启用/禁用系统应用
- ✅ 批量操作
- ✅ App Key 复制功能

### 2. API Key 管理界面（待实现）

- API Key 列表展示（用户级 + 系统级 Tab切换）
- 创建新的 API Key（名称、描述、过期时间）
- 创建时一次性显示完整 Secret Key（带复制功能）
- 启用/禁用 API Key
- 删除 API Key（带确认）
- 状态指示（活跃、禁用、过期）

### 3. 导航和路由（待配置）

- 侧边栏添加菜单项入口
- App.js 注册路由

## 用户体验要求

- 遵循工业风设计系统
- 支持暗色/亮色主题切换
- Secret Key 安全处理（仅创建时显示一次）

## 技术栈

### 前端技术

- **框架**: React 18.x
- **UI库**: Ant Design 5.x
- **样式方案**: Tailwind CSS + CSS Variables
- **路由**: React Router 6.x
- **HTTP客户端**: Axios

### 后端API（已实现）

- `ApiKeyController.java` - 用户级/系统级API Key管理
- 权限控制：用户级所有登录用户可操作，系统级仅ADMIN可操作

## 实施方案

### API 封装（已完成）

```javascript
// frontend/src/utils/api.js - 已包含
export const apiKeyApi = {
  // 用户级
  createForUser: (data) => axios.post('/api-keys/me', data),
  listForUser: () => axios.get('/api-keys/me'),
  updateUserKeyStatus: (id, status) => axios.put(`/api-keys/me/${id}/status`, { status }),
  deleteUserKey: (id) => axios.delete(`/api-keys/me/${id}`),
  
  // 系统级
  createForSystem: (data) => axios.post('/api-keys/system', data),
  listForSystem: () => axios.get('/api-keys/system'),
  updateSystemKeyStatus: (id, status) => axios.put(`/api-keys/system/${id}/status`, { status }),
  deleteSystemKey: (id) => axios.delete(`/api-keys/system/${id}`),
};
```

### 安全处理策略

1. Secret Key 仅在创建响应中返回一次
2. 前端使用 Modal 展示完整 Secret Key
3. 提供复制按钮，提示用户立即保存
4. 关闭 Modal 后 Secret Key 不可再见
5. 列表中仅显示截断的 Access Key

## 目录结构

```
frontend/src/
├── pages/
│   ├── SystemAppManagement.js   # [已存在] 系统应用管理
│   └── ApiKeyManagement.js      # [新建] API Key 管理页面
├── components/
│   └── Sidebar.js               # [修改] 添加菜单项
├── App.js                       # [修改] 添加路由
└── utils/
    └── api.js                   # [已包含] API封装
```

## 设计风格

遵循现有工业风设计系统（Industrial Command Center），采用暗色主题为主，霓虹蓝（#00d4ff）作为主强调色。

## 页面布局

### API Key 管理页面

```
┌─────────────────────────────────────────────────────────┐
│ API Key 管理                        [+ 创建新密钥]       │
├─────────────────────────────────────────────────────────┤
│ [我的密钥]  [系统密钥]  ← Tab 切换                       │
├─────────────────────────────────────────────────────────┤
│ 密钥名称 | Access Key | 状态 | 创建时间 | 最后使用 | 操作 │
│ 开发环境 | ak_xxx...  | ●启用 | 2024-01-15 | 2小时前 | [编辑][删除] │
│ 测试密钥 | ak_yyy...  | ○禁用 | 2024-01-10 | -       | [编辑][删除] │
├─────────────────────────────────────────────────────────┤
│ 分页: < 1 2 3 ... >                                     │
└─────────────────────────────────────────────────────────┘
```

### 创建成功 Modal

```
┌─────────────────────────────────────────┐
│ API Key 创建成功                    [X] │
├─────────────────────────────────────────┤
│ 请立即保存您的 Secret Key，            │
│ 关闭后将无法再次查看！                 │
│                                         │
│ Access Key:                             │
│ ┌─────────────────────────────────────┐ │
│ │ ak_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx │ [复制] │
│ └─────────────────────────────────────┘ │
│                                         │
│ Secret Key:                             │
│ ┌─────────────────────────────────────┐ │
│ │ sk_yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy │ [复制] │
│ └─────────────────────────────────────┘ │
│                                         │
│            [我已保存，关闭]              │
└─────────────────────────────────────────┘
```

## 状态指示器颜色

- 活跃（ACTIVE）: 绿色 #52c41a
- 禁用（DISABLED）: 灰色 #64748b
- 过期（EXPIRED）: 橙色 #fa8c16