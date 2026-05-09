## Context

### 当前状态

视频墙功能 (`VideoWall.js` + `CameraStream.js`) 支持多摄像头实时视频流播放和画质切换。当前实现存在两个核心缺陷：

**问题1：画质切换不生效**
- 用户在 `VideoQualityController` 中切换画质（1080p → 720p）
- `CameraStream.handleQualityChange()` 仅调用 `streamingApi.updateQuality()` 更新后端配置
- WebRTC 连接仍然使用初始化时建立的旧参数，视频流实际码率/分辨率未变化
- 只有 UI 状态更新了，用户看到的是"假生效"

**问题2：连接失败无自动重连**
- WebRTC 连接断开时，`pc.onconnectionstatechange` 仅记录日志
- `connectionState === 'failed' || 'disconnected'` 时只设置错误状态
- 前端没有任何重连逻辑，用户需要手动刷新页面
- 后端 `restartFailedStreams()` 是"假实现"，仅改状态字段，没有真正重建流连接

### 约束

- 不能破坏现有 API 兼容性
- 改动不能影响其他模块的 WebRTC 使用
- 前端重连需要避免"抖动"（无限重试导致界面闪烁）
- 后端重连需要防止对边缘节点造成压力

## Goals / Non-Goals

**Goals:**
- 画质切换真正生效（WebRTC 连接在画质变化时重建）
- 前端连接断开时自动重连（指数退避 + 最大重试限制）
- 后端 `restartFailedStreams()` 真正执行重连操作
- 提供可配置的重连策略参数

**Non-Goals:**
- 不实现多路复用流（单连接多流）
- 不改变 WebRTC 的 SDP/ICE 协商协议
- 不实现跨节点故障转移（已有 `edge-node-failover` 处理）

## Decisions

### Decision 1: 画质切换时重建连接而非热切换

**选择**：切换画质时完全断开并重建 WebRTC 连接 (`stopStream()` → `startStream()`)

**理由**：
- WebRTC 不支持运行时动态修改轨道的分辨率/码率约束（需要重新协商）
- 热切换需要完整的 SDP 重新协商，成本与重建连接相当
- 重建连接可以确保新参数立即生效（用户期望的即时反馈）

**备选考虑**：
- [热切换] 尝试 `RTCRtpSender.setParameters()` — 受限于浏览器支持和 ICE 重协商，实际不可行
- [半热切换] 只重建轨道而非整个连接 — 需要修改流媒体协议，复杂度高

---

### Decision 2: 前端重连采用指数退避策略

**选择**：使用指数退避 + 抖动的重连，最大重试 5 次，初始间隔 1 秒

**参数设计**：
```
initialDelay = 1000ms
maxDelay = 30000ms
maxRetries = 5
jitterFactor = 0.3  // ±30% 随机抖动，避免雷群效应
delay = min(initialDelay * 2^retryCount * (1 + jitter), maxDelay)
```

**理由**：
- 指数退避避免在网络抖动时频繁重连
- 抖动因子防止多个摄像头同时重连造成边缘节点压力
- 5 次重试覆盖大多数瞬时故障（网络抖动 ~15s）
- 超过 5 次后停止重连，等待用户干预或节点恢复

---

### Decision 3: 后端重连使用异步事件触发

**选择**：摄像头连接失败时发布 `StreamFailedEvent` 事件，由 `@Async` 处理器异步重连

**理由**：
- 与现有 `restartFailedStreams()` 定时任务互补（定时任务兜底 + 事件立即触发）
- 异步处理避免阻塞主线程
- 事件机制便于未来扩展（如发送告警通知）

---

### Decision 4: 重连状态通过 React Context 统一管理

**选择**：新增 `StreamHealthContext` 封装连接状态、重试计数、错误信息

**理由**：
- 避免 `CameraStream` 组件重连逻辑与渲染逻辑耦合
- 便于在 VideoWall 级别统一显示重连状态（如"正在重连... 第2次"）
- Context 可以跨多个 CameraStream 实例共享重连配置

---

### Decision 5: 配置外部化

**选择**：重连参数从 `videoConfig.js` 和后端 `application-edge.yml` 读取

```
frontend:
  stream:
    reconnect:
      enabled: true
      maxRetries: 5
      initialDelayMs: 1000
      maxDelayMs: 30000
      jitterFactor: 0.3

backend (edge):
  stream:
    reconnect:
      enabled: true
      max-retries: 3
      retry-interval-seconds: 60
```

**理由**：
- 运维可以在不修改代码的情况下调整策略
- 前端和后端参数独立配置，互不干扰
- 便于 A/B 测试不同重连策略

## Risks / Trade-offs

**[风险] 重连时画面短暂黑屏**
→ **缓解**：在 `stopStream()` 和 `startStream()` 之间添加 loading 状态，避免闪烁

**[风险] 频繁重连对边缘节点造成压力**
→ **缓解**：前端抖动 + 后端异步 + 定时任务兜底，形成三级防护

**[风险] 重建 WebRTC 连接失败时进入死循环**
→ **缓解**：严格限制最大重试次数，UI 展示最终错误状态

**[风险] 画质切换时用户正在观看**
→ **缓解**：当前设计是"激进切换"（立即断开），未来可加确认对话框

## Open Questions

1. 画质切换时是否需要用户确认对话框？当前实现无确认，直接切换。
2. 重连成功后的"恢复"状态如何展示？当前仅隐藏错误状态。
3. 后端边缘节点的 `restartStream()` 是否需要真正的流媒体协议重连实现？当前是 mock。
