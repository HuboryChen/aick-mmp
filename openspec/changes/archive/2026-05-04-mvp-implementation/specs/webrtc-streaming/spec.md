## ADDED Requirements

### Requirement: WebRTC视频流启动
系统 SHALL 支持通过WebRTC协议启动摄像头视频流，建立低延迟的实时视频传输通道。

#### Scenario: 成功启动视频流
- **WHEN** 用户在前端选择摄像头并点击播放
- **THEN** 系统 SHALL 建立与摄像头的RTSP连接
- **AND** 系统 SHALL 通过Janus Gateway创建WebRTC会话
- **AND** 系统 SHALL 返回SDP Offer给前端
- **AND** 前端 SHALL 与Janus建立WebRTC连接
- **AND** 视频流 SHALL 在前端播放器中显示

#### Scenario: 摄像头离线无法启动流
- **WHEN** 用户尝试启动离线摄像头的视频流
- **THEN** 系统 SHALL 返回错误信息"摄像头离线，无法启动视频流"
- **AND** 前端 SHALL 显示错误提示

#### Scenario: 并发流数超限
- **WHEN** 用户尝试启动第17路视频流（超过16路限制）
- **THEN** 系统 SHALL 返回错误信息"并发视频流数超过限制（最大16路）"
- **AND** 前端 SHALL 提示用户先关闭其他视频流

### Requirement: WebRTC SDP协商
系统 SHALL 支持完整的WebRTC SDP协商流程，包括Offer生成和Answer处理。

#### Scenario: 生成WebRTC Offer
- **WHEN** 前端请求启动视频流
- **THEN** 系统 SHALL 生成有效的WebRTC SDP Offer
- **AND** Offer SHALL 包含视频编解码信息（H.264/H.265）
- **AND** Offer SHALL 包含ICE候选信息

#### Scenario: 处理WebRTC Answer
- **WHEN** 前端发送WebRTC SDP Answer
- **THEN** 系统 SHALL 验证Answer格式有效性
- **AND** 系统 SHALL 完成WebRTC连接建立
- **AND** 系统 SHALL 开始传输视频数据

### Requirement: 视频流停止
系统 SHALL 支持停止正在播放的视频流，并释放相关资源。

#### Scenario: 用户停止视频流
- **WHEN** 用户点击停止按钮或切换页面
- **THEN** 系统 SHALL 断开WebRTC连接
- **AND** 系统 SHALL 停止RTSP流拉取
- **AND** 系统 SHALL 释放Janus会话资源
- **AND** 系统 SHALL 更新流会话状态为已结束

#### Scenario: 会话超时自动停止
- **WHEN** 视频流持续30分钟无用户操作
- **THEN** 系统 SHALL 自动停止该视频流
- **AND** 系统 SHALL 释放相关资源
- **AND** 前端 SHALL 显示"会话已超时"提示

### Requirement: 视频质量调整
系统 SHALL 支持动态调整视频流的质量参数（分辨率、码率）。

#### Scenario: 用户切换视频质量
- **WHEN** 用户在前端选择不同的视频质量（480p/720p/1080p）
- **THEN** 系统 SHALL 重新协商WebRTC参数
- **AND** 视频流 SHALL 以新的质量继续播放
- **AND** 系统 SHALL 记录质量切换日志

### Requirement: 流状态监控
系统 SHALL 提供视频流状态的实时监控和查询。

#### Scenario: 查询流状态
- **WHEN** 前端请求获取视频流状态
- **THEN** 系统 SHALL 返回当前流状态（STREAMING/PAUSED/ERROR）
- **AND** 系统 SHALL 返回视频统计信息（分辨率、码率、帧率、延迟）

#### Scenario: 流异常检测
- **WHEN** 视频流连接中断或出现错误
- **THEN** 系统 SHALL 标记流状态为ERROR
- **AND** 系统 SHALL 尝试自动重连（最多3次）
- **AND** 系统 SHALL 通知前端显示错误信息
