# Proposal: Video Wall Settings Drawer

## Why

视频墙页面当前的分配布局和画质控制设置直接放置在 Header 中，两个完整的 Card 组件挤在一起，视觉上过于拥挤，影响用户对视频监控画面的关注。同时，用户每次调整设置后需要重新选择，缺乏预设功能来快速切换常用配置。

通过将设置功能分离到侧边抽屉，既可以释放视频墙主区域的空间，又可以提供更丰富的设置选项和预设管理能力。

## What Changes

- **新增侧边设置抽屉**: 将布局和画质控制从 Header 分离到右侧抽屉组件
- **精简布局选择器**: 从 Card 组件优化为 2x2 网格图标选择器
- **精简画质选择器**: 从复杂 Card 组件优化为 Radio Group + Slider 组合
- **新增预设管理功能**: 支持创建、选择、应用、删除预设配置
- **用户偏好持久化**: 实时保存用户配置到数据库，跨设备同步
- **双重存储策略**: localStorage 离线备份 + 数据库持久化

## Capabilities

### New Capabilities

- `video-wall-settings-drawer`: 视频墙设置抽屉，提供布局、画质、预设的统一管理界面
- `video-wall-preset`: 视频墙预设管理，支持用户创建和管理多个配置预设
- `video-wall-preferences`: 用户偏好持久化，自动保存用户配置到数据库

### Modified Capabilities

- (无) 现有 video-wall 相关能力仅为实现调整，不涉及需求变更

## Impact

### 前端影响

- `frontend/src/pages/VideoWall.js`: 移除 Header 中的 Card 组件，添加抽屉触发按钮
- `frontend/src/components/VideoWall/`: 新增抽屉相关组件目录
  - `VideoWallSettingsDrawer.jsx`: 设置抽屉主组件
  - `PresetSelector.jsx`: 预设选择器
  - `LayoutSelector.jsx`: 精简版布局选择器
  - `QualitySelector.jsx`: 精简版画质选择器
- `frontend/src/hooks/useVideoWallSettings.js`: 新增视频墙设置状态管理 Hook
- `frontend/src/api/videoWallConfig.js`: 新增 API 定义

### 后端影响

- `backend/.../controller/VideoWallConfigController.java`: 新增视频墙配置控制器
- `backend/.../service/VideoWallConfigService.java`: 新增服务层
- `backend/.../repository/VideoWallPresetRepository.java`: 新增预设仓库
- `backend/.../repository/VideoWallPreferencesRepository.java`: 新增偏好仓库
- `backend/.../model/VideoWallPreset.java`: 新增预设实体
- `backend/.../model/VideoWallPreferences.java`: 新增偏好实体

### 数据库影响

- 新增 `video_wall_presets` 表: 存储用户预设配置
- 新增 `video_wall_preferences` 表: 存储用户当前偏好

### API 影响

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/video-wall/preferences | 获取用户偏好 |
| PUT | /api/video-wall/preferences | 更新用户偏好 |
| GET | /api/video-wall/presets | 获取预设列表 |
| POST | /api/video-wall/presets | 创建新预设 |
| PUT | /api/video-wall/presets/{id} | 更新预设 |
| DELETE | /api/video-wall/presets/{id} | 删除预设 |
| POST | /api/video-wall/presets/{id}/apply | 应用预设 |
| POST | /api/video-wall/presets/{id}/set-default | 设置默认预设 |
