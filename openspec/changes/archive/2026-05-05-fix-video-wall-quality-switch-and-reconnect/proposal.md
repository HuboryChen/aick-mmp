## Why

视频墙的画质切换功能无法真正生效，用户调整画质后视频流仍使用旧参数播放。此外，摄像头连接失败时系统缺乏有效的自动重连机制，导致视频流中断后无法恢复。这两个问题严重影响用户体验和系统的可用性。

## What Changes

- **修复画质切换失效**：切换画质时完全重建 WebRTC 连接，确保新参数立即生效
- **实现前端自动重连**：在 WebRTC 连接断开时采用指数退避策略自动重连，并限制最大重试次数
- **实现后端真正重连**：`EdgeStreamServiceImpl.restartStream()` 真正重新建立流连接，而非仅改状态
- **增强重连配置**：添加前端和后端重连策略的可配置参数（最大重试次数、退避间隔等）
- **添加断流检测**：在 CameraStream 组件中添加更健壮的连接状态检测

## Capabilities

### New Capabilities

- `video-quality-switch`: 视频画质动态切换能力。用户切换画质后，系统弹出确认对话框，用户确认后断开当前 WebRTC 连接，使用新参数重新建立连接，确保画质立即生效。
- `stream-auto-reconnect`: 视频流自动重连能力。当 WebRTC 连接断开时，前端采用指数退避策略自动重连；重连成功后系统根据当前网络质量自适应恢复画质。后端通过事件驱动和定时任务双重机制执行真正的流重建。
- `stream-connection-health`: 流连接健康状态管理。统一管理连接状态、错误类型和重试计数，通过 Context 驱动 UI 展示（角落徽章 + 全局状态栏）。

### Modified Capabilities

- `edge-node-failover`: 重连机制补强。后端 `restartFailedStreams()` 定时任务作为兜底，事件驱动的 `StreamFailedEventListener` 异步处理器实现真正的流重建逻辑。

## Impact

- **前端代码**：
  - `frontend/src/components/CameraStream.js`：添加 WebRTC 重连逻辑，修复画质切换时的连接重建
  - `frontend/src/hooks/useVideoWallConfig.js`：添加重连配置管理
- **后端代码**：
  - `backend/aick-mmp-edge/src/.../EdgeStreamServiceImpl.java`：`restartStream()` 实现真正的重连逻辑
  - `backend/aick-mmp-edge/src/.../EdgeStreamServiceImpl.java`：`restartFailedStreams()` 定时任务补强
- **API**：无新增 API，复用现有 `streaming` 相关接口
- **配置**：
  - `frontend/src/config/videoConfig.js`：新增重连相关配置项
  - 后端 `application-edge.yml`：新增重连相关配置
