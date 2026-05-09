## ADDED Requirements

### Requirement: 连接断开时自动重连

当 WebRTC 连接状态变为 `disconnected` 或 `failed` 时，前端 SHALL 自动尝试重新连接，采用指数退避策略。

#### Scenario: 连接短暂断开后自动恢复
- **WHEN** WebRTC 连接状态变为 `disconnected`
- **AND** 断开持续时间小于 initialDelayMs（1000ms）
- **THEN** 系统 SHALL 等待 initialDelayMs
- **AND** 尝试重新建立 WebRTC 连接
- **AND** 如果重连成功，视频流 SHALL 恢复正常播放

#### Scenario: 指数退避重连
- **WHEN** WebRTC 连接失败并触发重连
- **THEN** 重连延迟 SHALL 按以下公式计算：`delay = min(initialDelay * 2^retryCount * (1 ± jitterFactor), maxDelayMs)`
- **AND** jitterFactor SHALL 为 0.3（±30% 随机抖动）
- **AND** 最大延迟 SHALL 为 30000ms

#### Scenario: 重试次数限制
- **WHEN** WebRTC 连接连续失败
- **AND** 重试次数达到 maxRetries（5次）
- **THEN** 系统 SHALL 停止自动重连
- **AND** 视频窗口 SHALL 显示最终错误状态
- **AND** 错误消息 SHALL 显示"连接中断，已停止重连，请检查网络或刷新页面"

#### Scenario: 重连成功后清除重试计数
- **WHEN** WebRTC 重连成功
- **THEN** 重试计数 SHALL 重置为 0
- **AND** 下次连接断开时，延迟从 initialDelayMs 重新开始计算

### Requirement: 重连成功后自适应画质恢复

重连成功后，系统 SHALL 根据当前网络质量自动调整恢复画质。

#### Scenario: 网络质量优秀时恢复原画质
- **WHEN** WebRTC 重连成功
- **AND** RTCPeerConnection.getStats() 报告：RTT < 100ms 且 PacketLoss < 1%
- **THEN** 系统 SHALL 恢复用户之前选择的画质

#### Scenario: 网络质量一般时降一级画质
- **WHEN** WebRTC 重连成功
- **AND** RTCPeerConnection.getStats() 报告：RTT 在 100-300ms 或 PacketLoss 在 1-3%
- **THEN** 系统 SHALL 自动降一级画质播放
- **AND** 在视频角落显示"网络质量一般，已自动调整画质"提示

#### Scenario: 网络质量差时降两级画质
- **WHEN** WebRTC 重连成功
- **AND** RTCPeerConnection.getStats() 报告：RTT > 300ms 或 PacketLoss > 3%
- **THEN** 系统 SHALL 自动降两级画质播放

#### Scenario: 自适应降级仍然失败
- **WHEN** 重连后按自适应策略选择的画质仍然失败
- **THEN** 系统 SHALL 降级到最低画质（480p）
- **AND** 如果 480p 仍然失败 SHALL 停止重连并显示最终错误

### Requirement: 重连时 UI 状态展示

重连过程中，前端 SHALL 通过角落状态徽章和全局状态栏向用户展示当前状态。

#### Scenario: 单摄像头角落状态徽章
- **WHEN** 系统正在执行重连
- **AND** 当前重试次数大于 0
- **THEN** 视频窗口角落 SHALL 显示 ConnectionStatusBadge
- **AND** 徽章 SHALL 显示橙色圆点 + "第 X/Y 次重连中..."
- **AND** 徽章 SHALL 显示倒计时

#### Scenario: 单摄像头重连最终失败
- **WHEN** 重连次数达到 maxRetries
- **THEN** 徽章 SHALL 显示红色圆点 + "连接失败"
- **AND** 徽章 SHALL 显示手动重试按钮

#### Scenario: 多摄像头全局状态栏
- **WHEN** 视频墙有多个摄像头正在重连
- **THEN** VideoWall 顶部 SHALL 显示 GlobalReconnectBar
- **AND** 状态栏 SHALL 显示"X 个摄像头正在重连... 第 Y 次重试中"
- **AND** 当部分摄像头失败时，状态栏 SHALL 显示"X 个摄像头连接失败，请检查网络"

#### Scenario: 单摄像头时隐藏全局状态栏
- **WHEN** 视频墙只有 1 个摄像头
- **THEN** 全局状态栏 SHALL 隐藏
- **AND** 重连状态 SHALL 仅通过角落徽章展示

### Requirement: 后端异步重连处理

当边缘节点检测到摄像头连接失败时，后端 SHALL 异步触发重连流程。

#### Scenario: 边缘节点检测到连接失败
- **WHEN** 边缘节点检测到摄像头流连接失败
- **AND** 失败类型为 ERROR
- **AND** 当前重试次数小于 max-retries（3次）
- **THEN** 节点 SHALL 发布 `StreamFailedEvent` 事件
- **AND** 异步处理器 SHALL 触发 `restartStream()` 逻辑

#### Scenario: 定时任务兜底重连
- **WHEN** `restartFailedStreams()` 定时任务执行（每60秒）
- **THEN** 任务 SHALL 扫描所有状态为 ERROR 的流
- **AND** 对满足重连条件的流 SHALL 执行真正的重连操作
- **AND** 重连结果 SHALL 更新到 `activeStreams` 映射中

#### Scenario: 重连达到最大次数后停止
- **WHEN** 单个流的 `connectionRetries` 达到 max-retries（3次）
- **THEN** 该流 SHALL 被移出重试候选列表
- **AND** 流状态 SHALL 保持为 ERROR
- **AND** 系统 SHALL 等待外部干预（如管理员手动重启或节点恢复）

### Requirement: 重连配置可调整

前端和后端的重连策略参数 SHALL 从配置文件读取，支持运行时调整。

#### Scenario: 前端重连参数配置
- **WHEN** `videoConfig.js` 加载时
- **THEN** 重连参数 SHALL 从 `stream.reconnect.*` 配置节读取
- **AND** 包含以下参数：
  - `enabled`: 是否启用自动重连
  - `maxRetries`: 最大重试次数
  - `initialDelayMs`: 初始延迟（毫秒）
  - `maxDelayMs`: 最大延迟（毫秒）
  - `jitterFactor`: 抖动因子

#### Scenario: 后端重连参数配置
- **WHEN** `application-edge.yml` 加载时
- **THEN** 重连参数 SHALL 从 `stream.reconnect.*` 配置节读取
- **AND** 包含以下参数：
  - `enabled`: 是否启用自动重连
  - `max-retries`: 最大重试次数
  - `retry-interval-seconds`: 重试间隔（秒）
