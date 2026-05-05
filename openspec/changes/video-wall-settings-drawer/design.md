# Design: Video Wall Settings Drawer

## Context

### 背景

视频墙页面当前在 Header 中同时展示 SplitScreenController（布局控制）和 VideoQualityController（画质控制）两个 Card 组件，导致页面头部过于拥挤，影响用户对监控画面的关注。

现有组件结构：
- `SplitScreenController.jsx`: 2x2 网格按钮选择器，Card 包裹
- `VideoQualityController.jsx`: Select + Slider + 状态卡片，Card 包裹
- 两者放置在 `VideoWall.jsx` 的 Header 中

### 现有代码参考

- `frontend/src/components/SplitScreenController.jsx`: 布局控制器
- `frontend/src/components/VideoQualityController.jsx`: 画质控制器
- `frontend/src/hooks/useVideoWallConfig.js`: 配置管理 Hook
- `backend/.../model/UserConfig.java`: 现有用户配置实体

### 约束

- 保持与 Ant Design 组件库的一致性
- 使用现有的 JWT 认证机制
- 利用现有的 `user_configs` 表或新建专用表

## Goals / Non-Goals

**Goals:**

- 将布局和画质设置从 Header 分离到侧边抽屉，释放视频墙主区域空间
- 提供更丰富的设置选项和交互体验
- 支持用户预设管理（创建、选择、应用、删除）
- 实现用户偏好的持久化存储（数据库 + localStorage 双备份）
- 设置实时生效，无需确认

**Non-Goals:**

- 不改变现有视频播放和连接逻辑
- 不修改摄像头管理功能
- 不涉及权限控制调整
- 不实现跨用户共享预设（预设仅属于创建者）

## Decisions

### Decision 1: 抽屉 vs 标签页 vs 下拉面板

**选择**: 侧边抽屉（Drawer）

**理由**:
- 抽屉从右侧滑出，不遮挡视频墙主区域
- 展开后空间充足，可容纳预设管理、更多设置项
- 用户可以随时收起/展开，控制感强
- 与 Ant Design 的 Drawer 组件天然契合

**替代方案考虑**:
- 标签页: 需要完全切换页面，不适合实时监控场景
- 下拉面板: 空间有限，无法容纳预设管理

### Decision 2: 独立表 vs 复用 UserConfig

**选择**: 新建独立表（video_wall_presets + video_wall_preferences）

**理由**:
- 视频墙配置结构与通用 UserConfig 不同（包含嵌套的 cameraIds）
- 预设需要关联多个字段（layout, quality, bitrate, cameraIds）
- 独立表便于后续扩展（如预设分享、分类）
- 查询性能更好（独立索引）

**替代方案考虑**:
- 复用 UserConfig: 可行但查询复杂，JSON 存储不够规范化

### Decision 3: 数据存储策略

**选择**: 数据库为主，localStorage 为备

**理由**:
- 数据库保证跨设备同步
- localStorage 提供离线能力，加快页面加载
- `useVideoWallConfig.js` 已有类似实现模式可参考

**存储优先级**:
1. 尝试从数据库加载
2. 数据库失败则从 localStorage 恢复
3. 两者都无则使用默认值

### Decision 4: 预设模型设计

**选择**: 预设存储完整配置快照

```typescript
interface VideoWallPreset {
  id: number;
  name: string;
  layout: '1' | '4' | '9' | '16';
  quality: '480p' | '720p' | '1080p';
  bitrate: number;
  cameraIds: number[];
  isDefault: boolean;
}
```

**理由**:
- 预设作为配置快照，应用时直接覆盖当前状态
- 避免预设与当前配置的复杂同步逻辑
- 用户可清晰看到每个预设的完整配置

### Decision 5: 系统内置预设

**选择**: 硬编码内置预设 + 用户偏好优先

**内置预设**（硬编码，不可删除）:
```javascript
const BUILT_IN_PRESETS = [
  { id: 'sys-1', name: '单屏监控', layout: '1', quality: '1080p', bitrate: 4096, cameraIds: [], isDefault: false },
  { id: 'sys-2', name: '四分屏', layout: '4', quality: '720p', bitrate: 2048, cameraIds: [], isDefault: true },
  { id: 'sys-3', name: '九宫格', layout: '9', quality: '480p', bitrate: 1024, cameraIds: [], isDefault: false },
];
```

**理由**:
- 系统预设作为兜底，确保用户始终有可用的配置
- 内置预设不可删除，避免用户误删导致无配置可用
- 用户偏好（Preferences）优先于系统预设，加载时用户偏好覆盖系统预设

### Decision 6: 预设排序

**选择**: 拖拽自定义排序

**理由**:
- 用户可自由调整预设顺序，按使用习惯排列
- 使用 React DnD 或 @dnd-kit 库实现拖拽
- 排序结果保存到数据库

### Decision 7: 预设编辑

**选择**: 完整可编辑（名称 + 配置）

**理由**:
- 用户可修改预设名称
- 用户可修改预设配置（layout, quality, bitrate, cameraIds）
- 灵活满足用户需求，无需删除重建

### Decision 8: 数据同步策略

**选择**: 双向同步（防抖 500ms）

**理由**:
- 每次更改同时写入 localStorage 和数据库
- localStorage 保证离线可用和快速加载
- 数据库保证跨设备同步
- 防抖避免频繁写入

**实现**:
```javascript
const savePreferences = debounce(async (config) => {
  localStorage.setItem('VIDEO_WALL_CONFIG', JSON.stringify(config));
  await api.updatePreferences(config);
}, 500);
```

### Decision 9: 偏好与预设的关系

**选择**: 偏好优先于预设

**理由**:
- 用户选择预设后，可以继续调整
- 调整后的值会覆盖预设
- 下次打开时，显示的是调整后的值，不是预设的原始值
- 预设本身不会被修改

**实现**:
```javascript
// 选择预设
const applyPreset = (presetId) => {
  const preset = presets.find(p => p.id === presetId);
  setConfig(preset);
  setActivePresetId(presetId);
};

// 手动调整
const setLayout = (layout) => {
  setConfig(prev => ({ ...prev, layout }));
  setActivePresetId(null); // 取消预设激活
};
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 数据库写入失败 | localStorage 保留备份；下次打开时恢复 |
| 实时保存增加数据库压力 | 使用防抖（500ms），合并频繁操作 |
| 抽屉状态与视频墙状态不同步 | 使用单一数据源（useVideoWallSettings Hook） |
| 预设过多影响列表性能 | 使用虚拟滚动或分页加载 |

## Open Questions

1. ~~预设数量限制~~ → **已解决：无限制**
2. ~~默认预设~~ → **已解决：有系统内置预设 + 用户偏好优先**
3. ~~预设排序~~ → **已解决：拖拽自定义排序**
