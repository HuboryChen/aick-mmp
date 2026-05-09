# Motion Detection Recording Specification

## ADDED Requirements

### Requirement: 移动侦测录像配置

管理员 SHALL 能够为摄像头配置移动侦测录像功能，包括灵敏度阈值、检测区域和触发后录制时长。

#### Scenario: 启用移动侦测录像
- **WHEN** 管理员调用 `POST /api/recordings/schedules` 并设置 mode=MOTION_DETECTED
- **AND** 提供 sensitivityLevel（1-10）、detectionAreas、postMotionDurationSeconds 配置
- **THEN** 系统 SHALL 创建移动侦测录像计划
- **AND** 配置同步到边缘节点

#### Scenario: 灵敏度配置验证
- **WHEN** 管理员配置的 sensitivityLevel 不在 1-10 范围内
- **THEN** 系统 SHALL 返回 400 错误
- **AND** 提示灵敏度必须在 1-10 之间

---

### Requirement: 移动侦测参数

移动侦测录像 SHALL 支持可配置的检测参数，包括灵敏度、检测区域和后录制时长。

#### Scenario: 配置检测区域
- **WHEN** 管理员配置 detectionAreas 为多个矩形坐标
- **THEN** 系统 SHALL 存储这些区域配置
- **AND** 边缘节点仅在指定区域内检测移动

#### Scenario: 配置后录制时长
- **WHEN** 管理员设置 postMotionDurationSeconds=60
- **AND** 移动检测触发后
- **THEN** 边缘节点 SHALL 在运动停止后继续录制60秒

---

### Requirement: 移动侦测状态上报

边缘节点 SHALL 将移动侦测事件上报到中央服务，用于监控和分析。

#### Scenario: 上报移动侦测事件
- **WHEN** 边缘节点检测到移动
- **THEN** 节点 SHALL 调用 `POST /api/edge/recordings/motion-events`
- **AND** 包含 cameraId、timestamp、motionIntensity、affectedAreas

#### Scenario: 查询移动侦测历史
- **WHEN** 管理员调用 `GET /api/recordings/motion-events?cameraId={id}&startTime={}&endTime={}`
- **THEN** 系统 SHALL 返回指定时间范围内的移动侦测事件列表
- **AND** 包含事件详情和统计信息

---

### Requirement: 移动侦测与录像关联

移动侦测触发的录像 SHALL 与移动事件记录关联，便于后续回放和分析。

#### Scenario: 创建移动侦测录像记录
- **WHEN** 移动侦测触发录像开始
- **THEN** 系统 SHALL 创建 Recording 记录
- **AND** 设置 recordingType=MOTION_DETECTED
- **AND** 记录关联的 motionEventId

#### Scenario: 查询移动侦测录像
- **WHEN** 调用 `GET /api/recordings?cameraId={id}&type=MOTION_DETECTED`
- **THEN** 系统 SHALL 返回仅由移动侦测触发的录像列表
- **AND** 每条记录包含触发事件信息
