## Context

AICK-MMP项目当前已完成基础架构搭建，包括：
- 用户认证与权限管理（JWT + AK/SK双认证）
- 摄像头CRUD管理
- 边缘节点管理与自动故障转移
- 前端基础UI框架（React + Ant Design）

但核心的视频流播放功能目前仅为占位符实现，告警系统尚未建立。本次设计聚焦于实现MVP版本的核心缺失功能。

## Goals / Non-Goals

**Goals:**
1. 实现WebRTC视频流实时播放（延迟<200ms）
2. 完成StreamingService所有核心方法
3. 建立告警规则配置和告警记录系统
4. 完善RTSP协议适配器实现
5. 集成Janus Gateway作为WebRTC媒体服务器

**Non-Goals:**
- 移动端适配（V1.0功能）
- AI智能分析（V2.0功能）
- 录像存储和回放（P1优先级，MVP后实现）
- 多云部署支持
- 第三方OAuth登录

## Decisions

### 1. WebRTC架构选择

**决策**: 使用Janus Gateway作为WebRTC媒体服务器

**理由**:
- Janus Gateway成熟稳定，支持插件化扩展
- 原生支持RTSP转WebRTC流
- 与现有Java后端技术栈兼容
- 社区活跃，文档完善

**替代方案考虑**:
- Mediasoup: 性能更好但配置复杂，学习成本高
- Kurento: 功能丰富但已停止维护
- 原生WebRTC: 需要自行处理信令和ICE，复杂度太高

### 2. 视频流传输流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   前端      │────▶│   Janus     │────▶│   摄像头    │
│  WebRTC     │◀────│  Gateway    │◀────│  (RTSP)     │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │
       │                   │
       ▼                   ▼
┌─────────────┐     ┌─────────────┐
│  Streaming  │────▶│  Protocol   │
│  Service    │     │  Adapter    │
└─────────────┘     └─────────────┘
```

**流程**:
1. 前端请求启动视频流
2. 后端通过ProtocolAdapter连接摄像头RTSP流
3. 后端将RTSP流推送到Janus Gateway
4. Janus生成WebRTC SDP Offer
5. 前端与Janus建立WebRTC连接
6. 视频流通过WebRTC传输到前端

### 3. 告警系统设计

**决策**: 采用阈值告警模式，支持多级告警

**数据模型**:
```
AlertRule (告警规则)
├── 监控指标 (CPU/内存/摄像头离线等)
├── 阈值配置 (警告/严重)
├── 告警时段
└── 通知方式

AlertRecord (告警记录)
├── 关联规则ID
├── 告警级别
├── 告警内容
├── 发生时间
└── 处理状态
```

**告警触发流程**:
1. 定时任务采集系统指标
2. 对比告警规则阈值
3. 触发告警时创建AlertRecord
4. 通过WebSocket推送前端
5. Dashboard显示告警通知

### 4. 协议适配器设计

**决策**: 使用策略模式实现多协议支持

**接口设计**:
```java
public interface ProtocolAdapter {
    String getProtocolType();  // RTSP/ONVIF/GB28181
    boolean validateUrl(String url);
    boolean testConnection(Camera camera);
    InputStream getStream(Camera camera);
    void startStreamSession(Camera camera);
    void stopStreamSession(String sessionId);
}
```

**MVP阶段实现优先级**:
1. RTSP - 最高优先级，最常用
2. ONVIF - 次优先级，设备发现
3. GB28181 - 可选，国标协议

## Risks / Trade-offs

### 风险1: WebRTC兼容性问题
**风险**: 不同浏览器对WebRTC支持程度不同，可能导致播放失败
**缓解措施**:
- 优先支持Chrome和Firefox（WebRTC支持最好）
- 提供降级方案（如HLS流）
- 前端增加兼容性检测和提示

### 风险2: Janus Gateway性能瓶颈
**风险**: 大量并发视频流可能导致Janus性能下降
**缓解措施**:
- MVP阶段限制每用户最多16路视频
- 流会话30分钟无操作自动断开
- 后续版本考虑Janus集群部署

### 风险3: 协议适配器开发复杂度
**风险**: ONVIF和GB28181协议复杂，实现周期长
**缓解措施**:
- MVP阶段优先实现RTSP
- ONVIF使用开源库（如onvif-java）
- GB28181可延后到V1.0实现

### 风险4: 网络延迟不达标
**风险**: WebRTC传输延迟可能超过200ms目标
**缓解措施**:
- 边缘节点就近部署
- 优化ICE候选选择
- 使用TURN服务器作为备用

## Migration Plan

### 数据库迁移
1. 新增 `alert_rules` 表
2. 新增 `alert_records` 表
3. 可选：修改 `cameras` 表增加字段

### 部署步骤
1. 部署Janus Gateway容器
2. 更新后端服务（StreamingService实现）
3. 更新前端（CameraStream组件）
4. 执行数据库迁移脚本
5. 配置告警规则

### 回滚策略
- 保留原有StreamingService接口作为fallback
- 数据库变更使用事务，失败可回滚
- 前端使用特性开关控制新功能启用

## Open Questions

1. Janus Gateway是否需要集群部署？（MVP阶段单实例即可）
2. 告警通知是否需要在MVP支持邮件/短信？（建议仅系统内通知）
3. 是否需要在MVP支持录像功能？（建议延后到V1.0）
