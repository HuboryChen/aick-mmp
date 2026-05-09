## Why

当前系统中各类卡片组件（统计卡片、普通卡片、列表卡片、视频卡片）的布局间距存在设计不一致的问题。主要表现为：
- **gutter 值混乱**：部分页面使用 `[16, 16]`，部分使用 `[8, 8]` 或单一数值 `16`
- **间距规格不统一**：区块间距有 `mb-4`、`mb-6`、内联 `marginBottom: 16` 等多种写法
- **Card padding 不一致**：统计卡片用 20px，普通卡片用 16px，紧凑卡片用 12px，但未形成规范
- **响应式支持缺失**：缺少针对移动端的间距适配

这种不一致导致：
1. 页面视觉体验不连贯，用户感受割裂
2. 代码维护困难，新增页面时缺乏统一参考
3. UI 还原度低，不同人实现的效果差异大

## What Changes

1. **统一 gutter 规范**：所有页面统一使用 `gutter={[16, 16]}`，视频墙不再特殊化
2. **统一间距规格**：定义 `mb-6`（区块间距）、`mb-4`（卡片行间距）、`mt-3`（组件内间距）的使用场景
3. **统一 Card padding**：统计卡片 20px，普通卡片 16px，紧凑卡片 12px
4. **添加响应式支持**：为关键页面添加移动端间距适配
5. **消除内联样式**：将所有内联 margin/padding 改为 Tailwind 类

**受影响的页面**：
- Dashboard.js
- Analytics.jsx
- CameraDiscovery.js
- VideoWall.js
- EdgeNodeManagement.js
- AlertList.js
- AlertManagement.js

## Capabilities

### New Capabilities

- `card-layout-tokens`: 定义卡片布局与间距的设计 Token，包括 gutter 规格、间距层级、Card padding 规范
- `card-spacing-responsive`: 响应式间距支持，确保移动端有合适的卡片间距

### Modified Capabilities

- `frontend-design-tokens`: 扩展 design-tokens.md，补充布局间距相关规范（新增 `docs/frontend/card-layout-spacing-guide.md` 作为详细指南）

## Impact

- **样式文件**：`theme.css`、`index.css` 需补充统一 Card 样式类
- **页面文件**：7 个页面需按新规范调整
- **无 API 变更**：纯前端样式调整，不影响后端接口
- **无数据库变更**：不涉及数据层
