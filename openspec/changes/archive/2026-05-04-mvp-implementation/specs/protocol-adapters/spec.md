## ADDED Requirements

### Requirement: RTSP协议连接
系统 SHALL 支持通过RTSP协议连接摄像头并获取视频流。

#### Scenario: 成功连接RTSP摄像头
- **GIVEN** 摄像头配置有效的RTSP地址（rtsp://ip:port/stream）
- **WHEN** 系统尝试连接该摄像头
- **THEN** 系统 SHALL 成功建立RTSP连接
- **AND** 系统 SHALL 返回连接成功状态

#### Scenario: RTSP连接认证
- **GIVEN** 摄像头配置了用户名和密码
- **WHEN** 系统使用正确的凭据连接
- **THEN** 系统 SHALL 成功通过RTSP认证
- **AND** 系统 SHALL 获取视频流

#### Scenario: RTSP连接失败
- **GIVEN** 摄像头RTSP地址无效或网络不通
- **WHEN** 系统尝试连接
- **THEN** 系统 SHALL 在10秒后超时
- **AND** 系统 SHALL 返回连接失败错误

### Requirement: RTSP流测试
系统 SHALL 支持测试RTSP连接的有效性，无需启动完整视频流。

#### Scenario: 测试RTSP连接成功
- **WHEN** 管理员点击"测试连接"按钮
- **THEN** 系统 SHALL 尝试建立RTSP连接
- **AND** 系统 SHALL 验证视频流格式有效性
- **AND** 系统 SHALL 返回"连接成功"消息

#### Scenario: 测试RTSP连接失败
- **WHEN** 系统尝试连接无效的RTSP地址
- **THEN** 系统 SHALL 在5秒内返回错误
- **AND** 错误信息 SHALL 包含失败原因（网络不通/认证失败/格式错误）

### Requirement: ONVIF设备发现
系统 SHALL 支持通过ONVIF协议发现网络中的摄像头设备。

#### Scenario: 发现ONVIF设备
- **WHEN** 管理员点击"发现设备"按钮
- **THEN** 系统 SHALL 发送ONVIF探测消息到网络
- **AND** 系统 SHALL 收集响应的ONVIF设备列表
- **AND** 系统 SHALL 显示设备信息（IP、厂商、型号）

#### Scenario: 获取ONVIF设备能力
- **GIVEN** 已发现ONVIF设备
- **WHEN** 系统查询设备能力
- **THEN** 系统 SHALL 返回设备支持的功能（PTZ、视频编码等）
- **AND** 系统 SHALL 返回设备支持的分辨率列表

### Requirement: ONVIF视频流获取
系统 SHALL 支持通过ONVIF协议获取摄像头的视频流地址。

#### Scenario: 获取ONVIF流地址
- **GIVEN** 有效的ONVIF设备
- **WHEN** 系统请求媒体流URI
- **THEN** 系统 SHALL 返回RTSP流地址
- **AND** 流地址 SHALL 可用于视频播放

### Requirement: 协议适配器工厂
系统 SHALL 提供协议适配器工厂，根据协议类型自动选择对应的适配器。

#### Scenario: 自动选择RTSP适配器
- **GIVEN** 摄像头协议类型为RTSP
- **WHEN** 系统获取协议适配器
- **THEN** 系统 SHALL 返回RtspProtocolAdapter实例

#### Scenario: 自动选择ONVIF适配器
- **GIVEN** 摄像头协议类型为ONVIF
- **WHEN** 系统获取协议适配器
- **THEN** 系统 SHALL 返回OnvifProtocolAdapter实例

#### Scenario: 不支持的协议类型
- **GIVEN** 摄像头协议类型为UNSUPPORTED
- **WHEN** 系统尝试获取适配器
- **THEN** 系统 SHALL 抛出异常"不支持的协议类型"

### Requirement: 协议连接池
系统 SHALL 维护协议连接池，复用连接以提高性能。

#### Scenario: 复用已有连接
- **GIVEN** 摄像头A已建立RTSP连接
- **WHEN** 再次请求摄像头A的视频流
- **THEN** 系统 SHALL 复用已有连接
- **AND** 系统 SHALL 不创建新的TCP连接

#### Scenario: 连接超时释放
- **GIVEN** RTSP连接空闲超过5分钟
- **WHEN** 连接池清理任务执行
- **THEN** 系统 SHALL 关闭空闲连接
- **AND** 系统 SHALL 释放相关资源
