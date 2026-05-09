## Why

AICK-MMP多区域视频监控平台的MVP版本需要实现核心视频流播放功能和告警系统，以满足产品v0.5版本的基本要求。当前项目已完成用户认证、摄像头管理、边缘节点管理等基础功能，但核心的WebRTC视频流播放、告警规则配置等P0优先级功能尚未实现，无法提供完整的视频监控体验。

## What Changes

### 新增功能

1. **WebRTC视频流播放**
   - 前端CameraStream组件完整WebRTC实现
   - 与Janus Gateway媒体服务器集成
   - 支持多画面视频墙实时播放

2. **StreamingService核心方法实现**
   - `startStream(Long cameraId)` - 按摄像头ID启动流
   - `getStreamStatus(Long cameraId)` - 获取流状态
   - `generateWebRtcOffer()` - 生成WebRTC SDP Offer
   - `processWebRtcAnswer()` - 处理WebRTC Answer
   - 其他视频流控制方法

3. **告警系统**
   - AlertRule实体和数据库表
   - AlertRecord实体和数据库表
   - 告警规则配置API和页面
   - 告警通知机制（系统内）

4. **协议适配器完善**
   - RTSP协议适配器完整实现
   - ONVIF协议适配器基础实现
   - 协议连接测试功能

5. **系统监控增强**
   - 实时系统指标更新
   - 告警列表和通知
   - 系统活动日志

### 修改功能

- CameraStream组件 - 从占位符实现改为完整WebRTC播放
- StreamingService - 实现所有默认方法
- Dashboard页面 - 集成实时告警数据

## Capabilities

### New Capabilities

- `webrtc-streaming`: WebRTC实时视频流传输，包括SDP协商、ICE连接、媒体播放
- `alert-management`: 告警规则配置和告警记录管理，支持阈值告警和通知
- `protocol-adapters`: 摄像头协议适配器（RTSP/ONVIF/GB28181），负责设备连接和视频流获取

### Modified Capabilities

- 无（本次为纯新增功能实现）

## Impact

### 后端影响
- `backend/aick-mmp-central/service/StreamingService.java` - 实现所有接口方法
- `backend/aick-mmp-central/controller/StreamingController.java` - 新增WebRTC相关端点
- `backend/aick-mmp-shared/model/` - 新增AlertRule、AlertRecord实体
- `backend/aick-mmp-shared/adapter/protocol/` - 完善协议适配器实现

### 前端影响
- `frontend/src/components/CameraStream.js` - 重写为完整WebRTC实现
- `frontend/src/pages/VideoWall.js` - 集成真实视频流
- `frontend/src/pages/AlertManagement.js` - 新增告警规则配置页面
- `frontend/src/utils/api.js` - 新增告警相关API

### 基础设施影响
- Janus Gateway配置和集成
- 数据库表结构新增（alert_rules, alert_records）

### API影响
- 新增 `/api/v1/streaming/webrtc/offer` - 获取WebRTC Offer
- 新增 `/api/v1/streaming/webrtc/answer` - 提交WebRTC Answer
- 新增 `/api/v1/alerts/rules` - 告警规则CRUD
- 新增 `/api/v1/alerts/records` - 告警记录查询
