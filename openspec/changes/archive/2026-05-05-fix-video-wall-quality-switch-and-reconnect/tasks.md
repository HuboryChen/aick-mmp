## 1. 前端基础设施 - StreamHealthContext

- [ ] 1.1 在 `frontend/src/contexts/` 下创建 `StreamHealthContext.js`
- [ ] 1.2 实现连接状态枚举（idle/connecting/connected/reconnecting/disconnected/failed/closed）
- [ ] 1.3 实现 Context Provider，包含状态（connectionState, retryCount, error, healthMetrics）和方法（updateConnectionState, incrementRetry, resetRetry, setError, updateHealthMetrics）
- [ ] 1.4 在 `VideoWall.js` 中用 `StreamHealthContext.Provider` 包裹 `CameraStreamList`

## 2. 前端重连逻辑 - useReconnect Hook

- [ ] 2.1 在 `frontend/src/hooks/` 下创建 `useReconnect.js`
- [ ] 2.2 实现指数退避算法 `calculateDelay(retryCount, config)`：初始 1s，最大 30s，抖动因子 0.3
- [ ] 2.3 实现 `useReconnect(cameraId, config)`：返回 { reconnect, stopReconnect, retryCount, maxRetries, isReconnecting }
- [ ] 2.4 在 `pc.onconnectionstatechange` 中检测 disconnected/failed 状态，调用 reconnect
- [ ] 2.5 实现最大重试限制（5次），超过后停止并设置 failed 状态
- [ ] 2.6 实现自适应画质恢复：根据 RTCPeerConnection.getStats() 的 RTT 和 PacketLoss 决定恢复画质

## 3. 前端画质切换确认对话框

- [ ] 3.1 在 `frontend/src/components/` 下创建 `QualityConfirmDialog.js`
- [ ] 3.2 实现对话框 UI：显示目标画质名称、预估效果描述、"确定"/"取消"按钮
- [ ] 3.3 点击"确定"后，对话框进入 loading 状态（按钮禁用 + spinner）
- [ ] 3.4 点击"取消"关闭对话框，视频保持当前画质
- [ ] 3.5 连接失败时显示错误信息和"重试"按钮
- [ ] 3.6 在 `VideoQualityController.js` 中集成对话框，点击画质选择时弹出

## 4. CameraStream 重建连接

- [ ] 4.1 修改 `CameraStream.js` 中的 `handleQualityChange()`
- [ ] 4.2 实现：当前连接正在重建时，禁止重复触发
- [ ] 4.3 实现：`stopStream()` 断开当前连接 → 更新 quality state → `startStream()` 重建
- [ ] 4.4 重建连接成功后：持久化配置到后端 + localStorage
- [ ] 4.5 重建连接失败时：降级到 480p 再试，仍失败则显示最终错误，保持旧连接播放
- [ ] 4.6 修复 `useEffect` 依赖数组，添加 `camera?.id` 和 `currentQuality`

## 5. 前端状态展示组件

- [ ] 5.1 在 `frontend/src/components/` 下创建 `ConnectionStatusBadge.js`
- [ ] 5.2 实现角落徽章：connected(绿点+画质标签)、reconnecting(橙点+"第X/Y次"+倒计时)、failed(红点+"连接失败"+重试按钮)
- [ ] 5.3 在 `CameraStream.js` 中集成 `ConnectionStatusBadge`
- [ ] 5.4 在 `frontend/src/components/` 下创建 `GlobalReconnectBar.js`
- [ ] 5.5 实现全局状态栏：显示重连中/失败的摄像头数量和重试次数
- [ ] 5.6 在 `VideoWall.js` 中集成 `GlobalReconnectBar`，仅多摄像头（>1）时显示

## 6. 前端配置外部化

- [ ] 6.1 在 `frontend/src/config/videoConfig.js` 中添加 `stream.reconnect.*` 配置节
- [ ] 6.2 配置项：enabled, maxRetries(5), initialDelayMs(1000), maxDelayMs(30000), jitterFactor(0.3)
- [ ] 6.3 在 `useReconnect` 和 Context 中读取配置参数
- [ ] 6.4 添加画质映射表：每种画质对应的码率和网络质量阈值

## 7. 后端 - EdgeStreamServiceImpl 真正重连实现

- [ ] 7.1 修改 `EdgeStreamServiceImpl.java` 的 `restartStream()` 方法
- [ ] 7.2 实现真正重建流连接：closeStreamConnection() → 获取 CameraInfo → createStreamClient() → establishWebRtcSession()
- [ ] 7.3 在 `restartStream()` 中正确递增/重置 `connectionRetries`
- [ ] 7.4 修改 `restartFailedStreams()` 定时任务调用真正的 `restartStream()` 而非假实现
- [ ] 7.5 添加异常处理和日志记录

## 8. 后端 - 事件驱动重连

- [ ] 8.1 在 `backend/aick-mmp-edge/src/.../event/` 下创建 `StreamFailedEvent.java`
- [ ] 8.2 事件包含：cameraId, edgeNodeId, errorType, timestamp
- [ ] 8.3 在 `EdgeStreamServiceImpl` 中注入 `ApplicationEventPublisher`
- [ ] 8.4 在 `handleStreamError()` 中发布 `StreamFailedEvent`
- [ ] 8.5 创建 `StreamFailedEventListener.java`（@Async），执行真正的重连
- [ ] 8.6 在 Edge 应用主类或配置类启用 @Async

## 9. 后端配置

- [ ] 9.1 在 `application-edge.yml` 中添加 `stream.reconnect.*` 配置节
- [ ] 9.2 配置项：enabled, max-retries(3), retry-interval-seconds(60)
- [ ] 9.3 在 `EdgeStreamServiceImpl` 中通过 `@Value` 注入配置

## 10. 测试验证

- [ ] 10.1 单元测试：`useReconnect` 指数退避算法各种边界情况
- [ ] 10.2 单元测试：StreamHealthContext 状态转换
- [ ] 10.3 集成测试：画质切换端到端（对话框→loading→重建→成功）
- [ ] 10.4 手工测试：拔网线测试自动重连和自适应画质恢复
- [ ] 10.5 手工测试：多摄像头同时重连时的全局状态栏
- [ ] 10.6 后端测试：验证 `restartStream()` 真正执行了流重建（添加日志验证）
- [ ] 10.7 端到端：16 个摄像头同时切换不同画质的性能和稳定性
