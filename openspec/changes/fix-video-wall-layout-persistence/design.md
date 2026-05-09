## Context

当前视频墙布局设置持久化存在以下问题：

1. **加载优先级反转**：`useVideoWallSettings` 中 DB 数据优先于 localStorage，但 DB 保存有 500ms 防抖延迟，导致用户在"完成"后立即导航离开时 DB 未更新，而回访页面时 DB 旧数据覆盖了 localStorage 最新数据
2. **防抖定时器在卸载时被清除**：`useEffect` 清理函数执行 `clearTimeout`，使得导航离开时 DB 保存请求从未发出
3. **Drawer 持有独立 hook 实例**：`VideoWallSettingsDrawer` 和 `VideoWall` 各自调用 `useVideoWallSettings()`，导致保存操作重复、loading 状态不一致、潜在的竞态问题

## Goals / Non-Goals

**Goals:**
- 视频墙布局设置在所有场景下可靠持久化（刷新、导航离开再返回）
- 用户显式点击"完成"后数据立即保存到 DB
- 消除两个 hook 实例之间的数据不一致
- 保持与现有后端 `videoWallConfigApi` 接口兼容

**Non-Goals:**
- 不修改后端 API 接口
- 不改变预设（preset）功能的现有设计
- 不涉及摄像头 stream 连接的持久化

## Decisions

### 决策 1：配置加载优先级改为 localStorage → DB 合并

**方案**：加载配置时，同时读取 DB 和 localStorage，以 localStorage 为主数据源合并覆盖。

**Rationale**：
- localStorage 写入是同步的，始终保有用户最新操作
- DB 数据可能因防抖/网络延迟而滞后
- 合并策略：localStorage 覆盖 DB 中相同字段，DB 补充 localStorage 缺失的字段

**替代方案**：仅调整加载顺序为 localStorage 优先，DB 降级为备份。此方案的问题是 DB 中的预设关联数据（如 `lastPresetId`）可能丢失。

### 决策 2：显式保存操作改为即时 DB 写入

**方案**：`handleDone` 和 `handleApplySettings` 中的保存操作改为即时调用 `videoWallConfigApi.updatePreferences()`，不再经过防抖。

**Rationale**：
- 用户点击"完成"是一种显式确认行为，期望设置被保存
- 500ms 防抖在页面导航场景下不可靠
- 防抖消除组件卸载时丢失保存的风险

**替代方案**：在组件卸载前先完成 DB 保存（`beforeunload` 或 `unmount` 时同步保存），但浏览器不支持在卸载期间可靠地执行异步请求。

### 决策 3：Drawer 通过 props 接收父组件的设置方法

**方案**：移除 `VideoWallSettingsDrawer` 中的 `useVideoWallSettings()` 独立调用，改为接受 `config`、`setLayout`、`setQuality`、`setBitrate` 等 props。

**Rationale**：
- 消除两个独立 hook 实例的双写问题
- 避免两个实例独立加载/保存导致的状态不一致
- 父组件作为唯一数据源，职责清晰

**影响**：
- Drawer 内部 `LayoutSelector`/`QualitySelector` 仍然使用本地 state 做编辑态临时存储（点击完成前不提交）
- Drawer 不再需要 `applyPreset` 等方法（通过 props 从父组件传入或直接在 Drawer 中回调父组件）

### 决策 4：保留 Drawer 本地 state 作为编辑缓冲区

**方案**：Drawer 内部维持 `localLayout`、`localQuality`、`localBitrate` 等临时状态，点击"完成"时才统一提交到父组件 hook。

**Rationale**：
- 用户在 Drawer 内的操作不应立即影响视频墙（避免频繁刷新摄像头流）
- 提交时一次性更新布局/画质/码率，减少不必要的后端请求

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 移除防抖后，保存请求频率增加 | 仅在显式确认操作（"完成"按钮）时即时保存；Drawer 内的预设选择和拖拽调整仍保留防抖 |
| DB 保存可能因网络失败 | 已有错误处理：保存失败时不影响本地使用，console.error 记录日志 |
| localStorage 可能被清空 | 当 localStorage 无数据时回退到 DB（DB 数据至少是上次成功保存的版本） |
