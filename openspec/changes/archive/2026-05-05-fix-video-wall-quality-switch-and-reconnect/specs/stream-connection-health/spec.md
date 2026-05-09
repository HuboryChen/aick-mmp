## ADDED Requirements

### Requirement: 连接状态统一管理

系统 SHALL 通过 `StreamHealthContext` 统一管理视频流的连接健康状态、重试计数和错误信息。

#### Scenario: Context 提供连接状态
- **WHEN** `StreamHealthContext` 初始化
- **THEN** 初始连接状态 SHALL 为 `idle`
- **AND** 初始重试计数 SHALL 为 0
- **AND** 初始错误信息 SHALL 为 null

#### Scenario: Context 提供状态更新方法
- **WHEN** `StreamHealthContext` 渲染
- **THEN** 上下文 SHALL 提供 `updateConnectionState(state)` 方法
- **AND** 上下文 SHALL 提供 `incrementRetry()` 方法
- **AND** 上下文 SHALL 提供 `resetRetry()` 方法
- **AND** 上下文 SHALL 提供 `setError(message)` 方法

#### Scenario: 状态变更触发 UI 更新
- **WHEN** 上下文中的连接状态变更
- **THEN** 订阅该上下文的组件 SHALL 立即重新渲染
- **AND** UI SHALL 根据新状态显示对应的视觉反馈

### Requirement: 连接状态类型定义

系统 SHALL 定义清晰的连接状态枚举。

#### Scenario: 连接状态枚举值
- **WHEN** WebRTC 连接状态变化
- **THEN** 连接状态 SHALL 映射为以下枚举值之一：
  - `idle`: 初始状态，未建立连接
  - `connecting`: 正在建立连接
  - `connected`: 连接已建立，视频流正常
  - `reconnecting`: 连接断开，正在重连
  - `disconnected`: 连接已断开（临时）
  - `failed`: 连接失败（永久）
  - `closed`: 连接已关闭

### Requirement: 健康指标收集

系统 SHALL 收集并暴露流健康指标。

#### Scenario: 收集连接质量指标
- **WHEN** 视频流播放时
- **THEN** 系统 SHALL 定期收集以下指标：
  - `packetLoss`: 包丢失率
  - `roundTripTime`: 往返延迟（毫秒）
  - `jitter`: 抖动（毫秒）
- **AND** 指标 SHALL 通过 RTCPeerConnection.getStats() 获取

#### Scenario: 指标异常时提前预警
- **WHEN** 任何健康指标超过阈值（packetLoss > 5%, RTT > 500ms）
- **THEN** 系统 SHALL 在控制台记录警告日志
- **AND** 如果持续恶化 SHALL 考虑提前断开重连
