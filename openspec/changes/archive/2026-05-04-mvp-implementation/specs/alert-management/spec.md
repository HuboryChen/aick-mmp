## ADDED Requirements

### Requirement: 告警规则创建
系统 SHALL 支持管理员创建告警规则，配置监控指标和触发条件。

#### Scenario: 成功创建告警规则
- **WHEN** 管理员填写告警规则表单并提交
- **THEN** 系统 SHALL 验证规则名称唯一性
- **AND** 系统 SHALL 验证阈值配置有效性
- **AND** 系统 SHALL 保存告警规则到数据库
- **AND** 系统 SHALL 返回创建成功消息

#### Scenario: 创建重复名称的规则
- **WHEN** 管理员提交已存在的规则名称
- **THEN** 系统 SHALL 返回错误信息"规则名称已存在"
- **AND** 系统 SHALL 拒绝创建请求

### Requirement: 告警规则编辑
系统 SHALL 支持管理员修改已有的告警规则。

#### Scenario: 成功编辑告警规则
- **WHEN** 管理员修改规则配置并提交
- **THEN** 系统 SHALL 验证修改后的配置有效性
- **AND** 系统 SHALL 更新数据库中的规则记录
- **AND** 系统 SHALL 保持原有规则ID不变

### Requirement: 告警规则启用/禁用
系统 SHALL 支持启用或禁用告警规则，禁用后不再触发告警。

#### Scenario: 禁用告警规则
- **WHEN** 管理员点击禁用按钮
- **THEN** 系统 SHALL 将规则状态更新为DISABLED
- **AND** 该规则 SHALL 不再触发新的告警

#### Scenario: 启用告警规则
- **WHEN** 管理员点击启用按钮
- **THEN** 系统 SHALL 将规则状态更新为ENABLED
- **AND** 该规则 SHALL 恢复监控和告警功能

### Requirement: 告警规则删除
系统 SHALL 支持管理员删除不再需要的告警规则。

#### Scenario: 成功删除告警规则
- **WHEN** 管理员确认删除告警规则
- **THEN** 系统 SHALL 从数据库中删除该规则
- **AND** 系统 SHALL 保留该规则产生的历史告警记录

### Requirement: 告警触发
系统 SHALL 根据告警规则自动监控系统指标，并在满足条件时触发告警。

#### Scenario: CPU使用率超过阈值触发告警
- **GIVEN** 存在一条CPU使用率告警规则（阈值：>80%）
- **WHEN** 边缘节点CPU使用率超过80%并持续5分钟
- **THEN** 系统 SHALL 创建一条告警记录
- **AND** 告警级别 SHALL 为WARNING
- **AND** 系统 SHALL 通过WebSocket推送告警到前端

#### Scenario: 摄像头离线触发告警
- **GIVEN** 存在一条摄像头离线告警规则
- **WHEN** 摄像头心跳超时超过60秒
- **THEN** 系统 SHALL 创建一条告警记录
- **AND** 告警级别 SHALL 为ERROR
- **AND** 系统 SHALL 推送告警通知到Dashboard

#### Scenario: 告警冷却期避免重复告警
- **GIVEN** 告警规则配置了10分钟冷却时间
- **WHEN** 同一条件在10分钟内多次满足
- **THEN** 系统 SHALL 只触发一次告警
- **AND** 后续满足条件 SHALL 被忽略直到冷却期结束

### Requirement: 告警记录查询
系统 SHALL 支持查询历史告警记录，支持筛选和分页。

#### Scenario: 查询所有告警记录
- **WHEN** 用户访问告警记录页面
- **THEN** 系统 SHALL 返回告警记录列表
- **AND** 列表 SHALL 包含告警时间、级别、内容、状态
- **AND** 列表 SHALL 按时间倒序排列

#### Scenario: 按级别筛选告警
- **WHEN** 用户选择筛选条件"仅显示ERROR级别"
- **THEN** 系统 SHALL 只返回ERROR级别的告警记录
- **AND** 分页 SHALL 基于筛选后的结果

#### Scenario: 按时间范围筛选
- **WHEN** 用户选择时间范围（最近24小时）
- **THEN** 系统 SHALL 只返回该时间范围内的告警记录

### Requirement: 告警处理
系统 SHALL 支持用户对告警进行处理，记录处理结果。

#### Scenario: 标记告警为已处理
- **WHEN** 用户点击告警的"处理"按钮
- **AND** 用户填写处理备注
- **THEN** 系统 SHALL 将告警状态更新为RESOLVED
- **AND** 系统 SHALL 记录处理人和处理时间
- **AND** 系统 SHALL 保存处理备注

#### Scenario: 批量处理告警
- **WHEN** 用户选择多条告警并点击"批量处理"
- **THEN** 系统 SHALL 将所有选中告警标记为RESOLVED
- **AND** 系统 SHALL 记录批量处理操作

### Requirement: 告警统计
系统 SHALL 提供告警统计信息，用于系统监控分析。

#### Scenario: 查询告警统计
- **WHEN** Dashboard加载时
- **THEN** 系统 SHALL 返回告警统计数据
- **AND** 统计 SHALL 包含：今日告警数、未处理告警数、按级别分布
