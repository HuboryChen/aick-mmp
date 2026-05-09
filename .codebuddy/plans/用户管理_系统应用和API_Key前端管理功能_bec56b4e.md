---
name: 用户管理、系统应用和API Key前端管理功能
overview: 为系统补充前端管理界面,包括系统应用(System App)管理和API Key管理功能,用户管理界面已存在但可能需要优化。后端API已全部实现,重点在前端页面开发和API集成。
design:
  architecture:
    framework: react
  fontSystem:
    fontFamily: Inter, Noto Sans SC
    heading:
      size: 20px
      weight: 600
    subheading:
      size: 16px
      weight: 500
    body:
      size: 14px
      weight: 400
  colorSystem:
    primary:
      - "#00d4ff"
      - "#0284c7"
      - "#0088cc"
    background:
      - "#0a0e17"
      - "#141820"
      - "#1a1f2e"
    text:
      - "#ffffff"
      - "#94a3b8"
      - "#64748b"
    functional:
      - "#00ff88"
      - "#ff4757"
      - "#fbbf24"
todos:
  - id: extend-api-utils
    content: 扩展 API 工具模块，添加系统应用和 API Key 的 API 封装
    status: completed
  - id: create-system-app-page
    content: 创建系统应用管理页面组件（列表、搜索、创建、编辑、删除）
    status: completed
    dependencies:
      - extend-api-utils
  - id: create-api-key-page
    content: 创建 API Key 管理页面组件（用户级和系统级密钥管理）
    status: completed
    dependencies:
      - extend-api-utils
  - id: update-sidebar-menu
    content: 更新侧边栏导航菜单，添加系统应用和 API Key 管理菜单项
    status: completed
  - id: update-app-routes
    content: 更新 App.js 路由配置，集成新页面路由
    status: completed
    dependencies:
      - create-system-app-page
      - create-api-key-page
---

## 产品概述

完善多区域视频监控平台的前端管理功能，补充系统应用管理和 API Key 管理的前端界面。

## 核心功能

### 1. 系统应用管理界面

- 系统应用列表展示（分页、搜索、筛选）
- 创建新的系统应用（名称、描述、权限配置）
- 编辑系统应用信息（名称、描述、权限、状态）
- 删除系统应用（带确认）
- 启用/禁用系统应用
- 查看系统应用详情（包括关联的 API Key 数量、边缘节点数量）
- 批量操作（批量删除、批量启用/禁用）

### 2. API Key 管理界面

- API Key 列表展示（分用户级和系统级）
- 创建新的 API Key（用户级：当前用户创建；系统级：管理员创建）
- 查看 API Key 详情（仅创建时显示完整 Secret Key，后续不可见）
- 启用/禁用 API Key
- 删除 API Key
- 查看 API Key 使用记录
- API Key 状态指示（活跃、过期、禁用）

### 3. 用户体验增强

- 所有页面遵循工业风设计系统
- 支持暗色/亮色主题切换
- 响应式设计，支持移动端访问
- 流畅的动画效果（入场动画、状态变化动画）
- 完善的错误提示和操作反馈

## 技术栈选择

### 前端技术

- **框架**: React 18.x（现有）
- **UI库**: Ant Design 5.x（现有）
- **样式方案**: Tailwind CSS + CSS Variables（三层样式架构）
- **路由**: React Router 6.x（现有）
- **HTTP客户端**: Axios（现有）
- **语言**: JavaScript（尊重现有代码风格）

### 后端技术

- **框架**: Spring Boot 3.2.5 + Java 21（已实现）
- **认证**: JWT + AK/SK 双重认证机制（已实现）

## 实施方案

### 整体策略

遵循现有的工业风设计系统和三层样式架构，创建符合规范的管理界面。参考 CameraManagement.js 和 Dashboard.js 的实现模式，确保风格一致性。

### 核心设计原则

#### 1. 三层样式架构

```
Layer 1: Design Tokens (theme.css)
├── CSS Variables 定义颜色、间距、阴影等
└── 支持暗色/亮色主题切换

Layer 2: Component Classes (index.css)
├── @layer components 定义可复用组件类
└── 使用 @apply 封装常用样式组合

Layer 3: Utility Classes (Tailwind in JSX)
├── 直接在 JSX 中使用原子化工具类
└── 内联 style 仅用于动态计算值
```

#### 2. 组件复用策略

- 使用共享工业风组件库：IndustrialCard、StatusIndicator、PageHeader、GlowButton
- 统一的页面容器布局
- 统一的表格样式和交互模式

#### 3. API 封装规范

```javascript
// 系统应用 API
export const systemAppApi = {
  list: (params) => axios.get('/system-apps', { params }),
  get: (id) => axios.get(`/system-apps/${id}`),
  create: (data) => axios.post('/system-apps', data),
  update: (id, data) => axios.put(`/system-apps/${id}`, data),
  delete: (id) => axios.delete(`/system-apps/${id}`),
};

// API Key 管理 API
export const apiKeyApi = {
  // 用户级 API Key
  createForUser: (data) => axios.post('/api-keys/me', data),
  listForUser: () => axios.get('/api-keys/me'),
  updateUserKeyStatus: (id, status) => axios.put(`/api-keys/me/${id}/status`, { status }),
  deleteUserKey: (id) => axios.delete(`/api-keys/me/${id}`),
  
  // 系统级 API Key
  createForSystem: (data) => axios.post('/api-keys/system', data),
  listForSystem: () => axios.get('/api-keys/system'),
  updateSystemKeyStatus: (id, status) => axios.put(`/api-keys/system/${id}/status`, { status }),
  deleteSystemKey: (id) => axios.delete(`/api-keys/system/${id}`),
};
```

### 性能优化策略

#### 1. 数据加载优化

- 使用分页加载，默认每页 20 条记录
- 搜索防抖，避免频繁请求
- 关键数据缓存到 localStorage（如权限列表）

#### 2. 渲染性能优化

- 使用 React.memo 避免不必要的重渲染
- 表格虚拟滚动（如果数据量大）
- 条件渲染优化，减少 DOM 节点

#### 3. 动画性能优化

- 使用 CSS animations 代替 JS 动画
- 支持 prefers-reduced-motion 媒体查询
- 使用 transform 和 opacity 实现动画（GPU 加速）

### 实施细节

#### 1. 系统应用管理页面关键功能

- **权限配置**: 使用多选框展示可用权限（EDGE_REGISTER、EDGE_HEARTBEAT、EDGE_CONFIG_UPDATE）
- **状态管理**: 使用 Tag 组件展示状态（ACTIVE、INACTIVE、SUSPENDED）
- **关联信息**: 在详情中显示关联的 API Key 数量和边缘节点数量

#### 2. API Key 管理页面关键功能

- **Secret Key 安全**: 创建时一次性显示，后续不可见，提供复制功能
- **类型区分**: 使用 Tab 分离用户级和系统级 API Key
- **过期时间**: 支持设置过期时间，显示剩余有效天数
- **最后使用时间**: 显示最后使用时间，帮助识别未使用的 Key

#### 3. 安全考虑

- API Key 的 Secret Key 仅在创建时返回一次
- 前端不存储 Secret Key
- 敏感操作需要二次确认（删除、禁用）
- 操作日志记录（可选，后端已实现）

### 目录结构

```
frontend/src/
├── components/
│   └── ui/                       # 工业风共享组件（已存在）
├── pages/
│   ├── SystemAppManagement.js   # [NEW] 系统应用管理页面
│   ├── ApiKeyManagement.js      # [NEW] API Key 管理页面
│   └── UserManagement.js        # [MODIFY] 优化为工业风设计
├── utils/
│   └── api.js                    # [MODIFY] 添加系统应用和 API Key 的 API 封装
├── App.js                        # [MODIFY] 添加新路由
└── components/
    └── Sidebar.js                # [MODIFY] 添加菜单项
```

### 数据流设计

```
用户操作 → Ant Design 组件 → 事件处理函数 → API 调用 → 
后端处理 → 返回数据 → 更新状态 → 组件重渲染
```

### 错误处理机制

- 统一的错误拦截器（axios.js）
- 用户友好的错误提示（message.error）
- 网络错误重试机制（可选）
- 权限错误自动跳转登录页

### 测试策略

- 组件单元测试（可选）
- 集成测试（手动）
- E2E 测试（可选）
- 浏览器兼容性测试（Chrome、Firefox、Safari、Edge）

## 设计系统：Industrial Command Center（工业指挥中心风格）

系统应用管理和 API Key 管理页面遵循现有的工业风设计系统，采用暗色主题为主，支持亮色主题切换。使用霓虹蓝（#00d4ff）作为主强调色，配合深色背景，营造出专业的工业监控氛围。

## 页面布局设计

### 1. 系统应用管理页面

```
┌─────────────────────────────────────────────────────────┐
│ [顶部导航栏]  系统名称  主题切换  用户信息  退出      │
├─────────────────────────────────────────────────────────┤
│ [侧边栏]  │  [主内容区]                               │
│ 仪表盘   │  ┌─────────────────────────────────────┐   │
│ 视频墙   │  │ 系统应用管理         [+ 新建应用]  │   │
│ 摄像头   │  ├─────────────────────────────────────┤   │
│ 边缘节点 │  │ 筛选: [状态▼]  搜索: [________]     │   │
│ 地区管理 │  ├─────────────────────────────────────┤   │
│ 视频回放 │  │ 应用名称 | 描述 | 状态 | 权限 | 操作│   │
│ 系统设置 │  │ Edge-Main | 边缘节点主应用 | ●在线 │   │
│ 个人中心 │  │ API-Gateway | API网关服务 | ●在线  │   │
│ ─────── │  │ ...                                  │   │
│ 系统应用 │  ├─────────────────────────────────────┤   │
│ API管理  │  │ 分页: < 1 2 3 ... >                 │   │
│          │  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2. API Key 管理页面

```
┌─────────────────────────────────────────────────────────┐
│ [顶部导航栏]                                            │
├─────────────────────────────────────────────────────────┤
│ [侧边栏]  │  [主内容区]                               │
│          │  ┌─────────────────────────────────────┐   │
│          │  │ API Key 管理        [+ 创建新密钥]  │   │
│          │  ├─────────────────────────────────────┤   │
│          │  │ [我的密钥] [系统密钥]  ← Tab 切换   │   │
│          │  ├─────────────────────────────────────┤   │
│          │  │ 密钥名称 | Access Key | 状态 | 最后使用│
│          │  │ 开发环境 | ak_xxxx | ●启用 | 2小时前│   │
│          │  │ 测试密钥 | ak_yyyy | ○禁用 | -      │   │
│          │  │ ...                                  │   │
│          │  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 关键组件设计

### 工业风卡片容器

- 毛玻璃背景效果
- 发光边框（可选）
- 顶部强调线（渐变色）
- 圆角设计（12px）
- 微妙的阴影效果

### 状态指示器

- 在线状态：绿色呼吸灯（#00ff88）
- 离线状态：红色（#ff4757）
- 警告状态：橙色（#fbbf24）
- 禁用状态：灰色（#64748b）

### 表格设计

- 深色背景（var(--color-bg-card)）
- 边框颜色（var(--color-border)）
- 鼠标悬停高亮（var(--color-bg-elevated)）
- 状态 Tag 使用对应颜色

### 按钮设计

- 主按钮：渐变背景（霓虹蓝）
- 次要按钮：边框样式
- 危险按钮：红色系
- 发光效果（可选）