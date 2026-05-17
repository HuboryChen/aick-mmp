## ADDED Requirements

### Requirement: 客流统计

AI微服务 SHALL 实现基于深度学习的客流统计功能，统计指定区域的进出人数。

#### Scenario: 实时人数统计
- **WHEN** 收到摄像头帧数据且配置了客流统计时
- **THEN** 系统 SHALL 使用YOLOv8检测画面中的行人
- **AND** 使用ByteTrack对行人进行跟踪
- **AND** 根据跟踪轨迹判断进出方向
- **AND** 返回当前人数、进门数、出门数

#### Scenario: 区域人数统计
- **WHEN** 配置了多个统计区域时
- **THEN** 系统 SHALL 分别统计每个区域内的人数
- **AND** 返回各区域的进出人数
- **AND** 包含区域ID和区域类型

#### Scenario: 置信度过滤
- **WHEN** 检测到的行人置信度低于阈值时
- **THEN** 系统 SHALL 忽略该检测结果
- **AND** 默认置信度阈值为0.5

---

### Requirement: 行为识别

AI微服务 SHALL 实现基于姿态估计的行为识别，检测异常行为并产生告警。

#### Scenario: 滞留检测
- **WHEN** 检测到人员在指定区域内停留超过阈值时间时
- **THEN** 系统 SHALL 触发滞留告警
- **AND** 包含滞留时长、人员位置
- **AND** 告警类型为LOITERING
- **AND** 严重级别为INFO

#### Scenario: 区域闯入检测
- **WHEN** 检测到人员进入禁止区域时
- **THEN** 系统 SHALL 触发闯入告警
- **AND** 包含闯入时间、人员位置
- **AND** 告警类型为INTRUSION
- **AND** 严重级别为WARNING

#### Scenario: 聚集检测
- **WHEN** 检测到指定区域内人员数量超过阈值时
- **THEN** 系统 SHALL 触发聚集告警
- **AND** 包含聚集人数、区域信息
- **AND** 告警类型为GATHERING
- **AND** 严重级别为INFO

#### Scenario: 跌倒检测
- **WHEN** 检测到人员姿态异常（高度突降、姿态角度异常）时
- **THEN** 系统 SHALL 触发跌倒告警
- **AND** 包含跌倒位置、时间
- **AND** 告警类型为FALL_DETECTION
- **AND** 严重级别为CRITICAL

#### Scenario: 行为分析参数配置
- **WHEN** 初始化行为分析模块时
- **THEN** 系统 SHALL 支持以下配置参数：
  - `loitering_threshold_seconds`：滞留阈值，默认30秒
  - `gathering_threshold_count`：聚集人数阈值，默认10人
  - `fall_detection_enabled`：跌倒检测开关，默认false
  - `alert_confidence_threshold`：告警置信度阈值，默认0.7

---

### Requirement: 姿态估计

AI微服务 SHALL 提供基于ONNX Runtime的姿态估计能力，输出人体关键点坐标。

#### Scenario: 关键点检测
- **WHEN** 收到帧数据时
- **THEN** 系统 SHALL 检测画面中的所有人体
- **AND** 输出人体关键点坐标
- **AND** 包含每个关键点的置信度

---

### Requirement: 统计结果输出

AI微服务 SHALL 将分析结果通过gRPC实时返回，并异步输出到Kafka消息队列供Java系统消费处理。

#### Scenario: 发布客流统计结果
- **WHEN** 客流统计完成时
- **THEN** 系统 SHALL 发布结果到 `ai-passenger-stats` Topic
- **AND** 包含cameraId、timestamp、enterCount、exitCount、insideCount

#### Scenario: 发布行为告警
- **WHEN** 检测到异常行为时
- **THEN** 系统 SHALL 发布告警到 `ai-behavior-events` Topic
- **AND** 包含eventType、level、position、description、timestamp
