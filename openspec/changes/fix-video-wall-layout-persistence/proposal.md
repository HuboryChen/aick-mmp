## Why

视频墙的布局设置（1/4/9/16路）在用户点击"完成"后虽然视觉上生效，但刷新页面或导航到其它页面再返回时布局会恢复为修改前的状态。这是因为数据持久化链路中存在多个缺陷：防抖定时器在组件卸载时被清除导致 DB 未更新、加载优先级设计导致最新的 localStorage 数据被忽略、以及两个独立 hook 实例相互干扰。

## What Changes

- **修复 useVideoWallSettings 的加载优先级**：将 localStorage 作为首要数据源，DB 数据作为辅助补充，确保用户最近的更改始终被加载
- **移除组件级联卸载时的防抖取消**：用户在显式点击"完成"后的 DB 保存改为即时执行，而非防抖延迟执行
- **消除 Drawer 中独立的 hook 实例**：VideoWallSettingsDrawer 不再创建自己的 `useVideoWallSettings` 实例，改为通过 props 接收父组件的配置和方法
- **优化双写问题**：布局修改只通过一个路径保存，避免两个 hook 实例各自保存和防抖导致的竞态问题

## Capabilities

### New Capabilities
- `video-wall-settings-persistence`: 视频墙设置（布局、画质、码率、摄像头选择）的可靠持久化能力，确保用户设置在各页面间保持一致

### Modified Capabilities
- `video-wall-redesign`: 增强布局切换功能的行为描述，明确持久化要求

## Impact

### 前端
- `frontend/src/hooks/useVideoWallSettings.js` — 重写加载优先级逻辑，修改保存策略
- `frontend/src/components/VideoWall/VideoWallSettingsDrawer.jsx` — 移除独立 hook 实例，改为 props 驱动
- `frontend/src/pages/VideoWall.js` — 调整配置传递方式，支撑 Drawer 的 props 驱动模式

### 后端
无影响，仅涉及前端逻辑改动。
