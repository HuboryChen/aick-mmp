## 1. 数据库模型和表结构

- [x] 1.1 创建 AlertRule 实体类（告警规则）
- [x] 1.2 创建 AlertRecord 实体类（告警记录）
- [x] 1.3 创建 alert_rules 数据库表
- [x] 1.4 创建 alert_records 数据库表
- [x] 1.5 创建 JPA Repository 接口

## 2. 协议适配器实现

- [x] 2.1 完善 ProtocolAdapter 接口定义
- [x] 2.2 实现 RtspProtocolAdapter 完整功能
- [x] 2.3 实现 OnvifProtocolAdapter 基础功能
- [x] 2.4 实现 ProtocolAdapterFactory 工厂类
- [x] 2.5 添加协议连接池管理

## 3. StreamingService 核心方法实现

- [x] 3.1 实现 startStream(Long cameraId) 方法
- [x] 3.2 实现 getStreamStatus(Long cameraId) 方法
- [x] 3.3 实现 generateWebRtcOffer(Long cameraId) 方法
- [x] 3.4 实现 processWebRtcAnswer(Long cameraId, String answer) 方法
- [x] 3.5 实现 stopStream(Long cameraId) 方法
- [x] 3.6 实现 pauseStream/resumeStream 方法
- [x] 3.7 实现 updateStreamQuality 方法
- [x] 3.8 实现 getStreamRecordingUrl 方法
- [x] 3.9 实现 startStreamRecording/stopStreamRecording 方法

## 4. Janus Gateway 集成

- [x] 4.1 添加 Janus Java 客户端依赖
- [x] 4.2 创建 JanusClient 配置类
- [x] 4.3 实现 Janus 会话管理
- [x] 4.4 实现 RTSP 到 WebRTC 流转码配置
- [x] 4.5 添加 Janus 健康检查

## 5. 后端 API 开发

- [x] 5.1 创建 StreamingController WebRTC 相关端点
- [x] 5.2 创建 AlertRuleController 告警规则 CRUD API
- [x] 5.3 创建 AlertRecordController 告警记录查询 API
- [x] 5.4 实现告警触发和通知服务
- [x] 5.5 添加告警规则定时检查任务

## 6. 前端 WebRTC 视频播放

- [x] 6.1 重写 CameraStream 组件 WebRTC 实现
- [x] 6.2 实现 WebRTC SDP Offer/Answer 交换
- [x] 6.3 实现 ICE 候选处理
- [x] 6.4 添加视频流状态显示
- [x] 6.5 实现视频质量切换功能

## 7. 前端告警管理页面

- [x] 7.1 创建 AlertManagement 告警规则配置页面
- [x] 7.2 创建 AlertList 告警记录列表页面
- [x] 7.3 实现告警规则表单（创建/编辑）
- [x] 7.4 实现告警记录筛选和分页
- [x] 7.5 添加告警通知组件（WebSocket）

## 8. 前端 API 和工具

- [x] 8.1 添加 streamingApi WebRTC 相关方法
- [x] 8.2 添加 alertApi 告警相关方法
- [x] 8.3 实现 WebSocket 客户端连接
- [x] 8.4 添加视频流错误处理

## 9. 视频墙集成

- [x] 9.1 更新 VideoWall 页面集成真实视频流
- [x] 9.2 实现多路视频流并发管理
- [x] 9.3 添加视频流布局切换功能
- [x] 9.4 实现视频流全屏播放

## 10. 系统监控增强

- [x] 10.1 更新 Dashboard 实时数据获取
- [x] 10.2 添加告警统计卡片
- [x] 10.3 实现系统活动日志显示
- [x] 10.4 添加实时告警通知弹窗

## 11. Docker 和部署配置

- [x] 11.1 添加 Janus Gateway 到 docker-compose.yml
- [x] 11.2 配置 Janus 与后端服务网络连接
- [x] 11.3 更新应用配置支持 Janus
- [x] 11.4 添加数据库初始化脚本

## 12. 测试和验证

- [x] 12.1 编写 StreamingService 单元测试
- [x] 12.2 编写 AlertService 单元测试
- [x] 12.3 测试 WebRTC 视频流端到端流程
- [x] 12.4 测试告警触发和通知流程
- [x] 12.5 测试协议适配器连接功能
