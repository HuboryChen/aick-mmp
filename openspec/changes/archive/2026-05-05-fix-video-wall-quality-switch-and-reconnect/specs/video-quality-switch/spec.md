## ADDED Requirements

### Requirement: 画质切换需要用户确认

当用户在视频墙上切换画质时，系统 SHALL 弹出确认对话框，用户确认后才执行画质切换。

#### Scenario: 弹出画质切换确认对话框
- **WHEN** 用户在画质控制器中选择不同画质
- **THEN** 系统 SHALL 弹出 QualityConfirmDialog
- **AND** 对话框 SHALL 显示目标画质名称和预估效果描述
- **AND** 视频流 SHALL 继续播放当前画质不受影响

#### Scenario: 用户确认后执行切换
- **WHEN** 用户点击对话框的"确定"按钮
- **THEN** 对话框 SHALL 进入 loading 状态（按钮显示加载指示器）
- **AND** 视频流 SHALL 继续播放旧画质直到新连接建立
- **AND** 当前 WebRTC 连接 SHALL 断开
- **AND** 使用新画质参数新建 WebRTC 连接
- **AND** 新连接建立后，视频流 SHALL 以新画质播放
- **AND** 成功回调 SHALL 持久化配置到后端和 localStorage

#### Scenario: 用户取消切换
- **WHEN** 用户点击对话框的"取消"按钮
- **THEN** 系统 SHALL 关闭对话框
- **AND** 视频流 SHALL 保持当前画质不变

#### Scenario: 画质切换时连接失败
- **WHEN** 画质切换后 WebRTC 连接建立失败
- **AND** 重连次数已超过上限
- **THEN** 对话框 SHALL 显示错误信息
- **AND** 系统 SHALL 保持在上一次成功连接的画质（如果存在）
- **AND** 用户可点击"重试"按钮重新尝试切换

#### Scenario: 多摄像头同时切换不同画质
- **WHEN** 视频墙处于 2x2 分屏模式
- **AND** 用户同时调整多个摄像头的画质
- **THEN** 每个摄像头 SHALL 独立弹出确认对话框
- **AND** 某个摄像头的连接失败 SHALL 不影响其他摄像头

### Requirement: 画质切换时数据持久化

用户切换画质后，新画质 SHALL 持久化到后端配置和前端缓存。

#### Scenario: 画质配置持久化到后端
- **WHEN** 用户切换画质
- **AND** 新连接成功建立
- **THEN** 新画质 SHALL 通过 `streamingApi.updateQuality()` 保存到后端
- **AND** 后端配置 SHALL 在用户下次访问时作为默认画质加载

#### Scenario: 画质配置备份到 localStorage
- **WHEN** 用户切换画质
- **AND** 新连接成功建立
- **THEN** 新画质 SHALL 备份到 `localStorage.videoWallQuality`
- **AND** 当后端配置加载失败时，系统 SHALL 使用 localStorage 中的备份
