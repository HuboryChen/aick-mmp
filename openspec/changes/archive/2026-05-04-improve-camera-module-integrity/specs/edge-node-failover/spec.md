# Edge Node Failover Specification

## MODIFIED Requirements

### Requirement: 边缘节点心跳包含摄像头状态

边缘节点心跳请求 SHALL 包含分配给该节点的摄像头状态信息，实现双向状态同步。

#### Scenario: 心跳上报摄像头状态
- **WHEN** 边缘节点调用 `POST /api/edge/heartbeat`
- **THEN** 请求 SHALL 包含 cameraStatuses 数组
- **AND** 每个摄像头状态包含：
  - cameraId: 摄像头ID
  - status: ONLINE/OFFLINE/ERROR
  - failureReason: 失败原因（如适用）
  - bitrate: 当前码率
  - fps: 当前帧率

#### Scenario: 中央服务更新摄像头状态
- **WHEN** 边缘节点心跳包含摄像头状态更新
- **AND** 摄像头状态与中央记录不一致
- **THEN** 中央服务 SHALL 采用边缘节点上报的状态
- **AND** 记录状态变更历史

#### Scenario: 边缘节点上报与本地检测冲突时优先使用上报状态
- **WHEN** 中央服务本地检测到摄像头异常
- **AND** 边缘节点在心跳中报告摄像头 ONLINE
- **THEN** 系统 SHALL 信任边缘节点的上报状态
- **AND** 将摄像头标记为 ONLINE

#### Scenario: 心跳超时未上报摄像头状态
- **WHEN** 边缘节点心跳超时未到达
- **AND** 上次心跳中未包含摄像头状态
- **THEN** 系统 SHALL 将该节点关联的摄像头状态标记为 UNKNOWN
- **AND** 触发状态重新检测流程

---

## ADDED Requirements

### Requirement: 摄像头级别健康检查

系统 SHALL 支持摄像头级别的健康检查，不仅依赖节点心跳，还监控实际视频流状态。

#### Scenario: 定期摄像头健康检查
- **WHEN** 边缘节点检测到摄像头异常（如视频流中断）
- **THEN** 节点 SHALL 立即更新本地状态
- **AND** 在下次心跳时上报到中央服务

#### Scenario: 健康检查异常触发告警
- **WHEN** 摄像头状态变为 ERROR
- **AND** 持续时间超过告警阈值（默认5分钟）
- **THEN** 系统 SHALL 发送告警通知
- **AND** 记录告警事件
