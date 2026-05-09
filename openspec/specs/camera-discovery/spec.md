# Camera Discovery Specification

## ADDED Requirements

### Requirement: 网络扫描任务管理

系统 SHALL 支持创建和管理网络扫描任务，用于发现网络中的摄像头设备。

#### Scenario: 创建扫描任务
- **WHEN** 用户调用 `POST /api/camera-discovery/scan` 并指定网段（如 192.168.1.0/24）
- **THEN** 系统 SHALL 创建新的扫描任务
- **AND** 返回任务 ID 和初始状态

#### Scenario: 查询扫描任务状态
- **WHEN** 用户调用 `GET /api/camera-discovery/scan/{taskId}`
- **THEN** 系统 SHALL 返回任务的当前状态
- **AND** 状态包括：PENDING、RUNNING、COMPLETED、FAILED
- **AND** 包含进度百分比和已扫描设备数

#### Scenario: 取消扫描任务
- **WHEN** 用户调用 `POST /api/camera-discovery/scan/{taskId}/cancel`
- **AND** 任务状态为 PENDING 或 RUNNING
- **THEN** 系统 SHALL 停止扫描任务
- **AND** 返回已发现的部分结果

---

### Requirement: 网络段扫描

系统 SHALL 支持对指定网段进行 IP 扫描，发现潜在摄像头设备。

#### Scenario: 扫描指定网段
- **WHEN** 扫描任务开始执行
- **AND** 指定网段为 192.168.1.0/24
- **THEN** 系统 SHALL 遍历 192.168.1.1 到 192.168.1.254
- **AND** 对每个 IP 进行连通性测试

#### Scenario: 限制扫描并发数
- **WHEN** 扫描任务执行中
- **THEN** 系统 SHALL 控制并发扫描数量不超过配置值（默认 50）
- **AND** 避免对网络造成过大压力

#### Scenario: 扫描超时处理
- **WHEN** 某个 IP 的扫描响应超过超时时间（默认 3 秒）
- **THEN** 系统 SHALL 标记该 IP 为不可达
- **AND** 继续扫描下一个 IP

---

### Requirement: 摄像头设备识别

系统 SHALL 能够识别网络扫描发现中的常见摄像头设备。

#### Scenario: 识别常见摄像头品牌
- **WHEN** 扫描发现可访问的 IP
- **THEN** 系统 SHALL 尝试识别设备类型
- **AND** 通过常见端口（554、80、8080、8554 等）和 HTTP 响应头判断
- **AND** 支持识别：海康威视、大华、宇视、天地伟业等主流品牌

#### Scenario: 识别设备型号
- **WHEN** 设备被识别为摄像头
- **THEN** 系统 SHALL 尝试获取设备型号信息
- **AND** 通过设备信息页面或 ONVIF 接口获取

#### Scenario: 未知设备标记
- **WHEN** 扫描发现可访问的 IP
- **AND** 系统无法识别设备类型
- **THEN** 系统 SHALL 标记为"未知设备"
- **AND** 允许用户手动指定设备类型

---

### Requirement: 扫描结果管理

系统 SHALL 管理扫描结果，支持查看、选择和批量操作。

#### Scenario: 获取扫描结果列表
- **WHEN** 扫描任务完成
- **AND** 用户调用 `GET /api/camera-discovery/scan/{taskId}/results`
- **THEN** 系统 SHALL 返回发现的所有设备列表
- **AND** 每个设备包含：IP、端口、设备类型、品牌、型号、在线状态

#### Scenario: 选择设备添加
- **WHEN** 用户从扫描结果中选择一个或多个设备
- **THEN** 系统 SHALL 根据设备信息预填充添加摄像头表单
- **AND** 自动匹配配置模板（如果品牌/型号已知）

#### Scenario: 一键添加选中设备
- **WHEN** 用户选择多个设备并点击"一键添加"
- **AND** 每个设备有足够的配置信息
- **THEN** 系统 SHALL 批量创建摄像头
- **AND** 根据设备 IP 地区自动分配边缘节点
- **AND** 返回添加结果报告

---

### Requirement: 连通性测试

系统 SHALL 支持对单个 IP 地址进行连通性测试，用于验证摄像头是否可访问。

#### Scenario: 测试单个 IP 连通性
- **WHEN** 用户调用 `POST /api/camera-discovery/test` 并指定 IP 地址和端口
- **THEN** 系统 SHALL 测试该 IP 和端口的连通性
- **AND** 返回测试结果（成功/失败）和响应时间

#### Scenario: 测试摄像头 URL 可访问性
- **WHEN** 用户提供完整的摄像头 URL
- **THEN** 系统 SHALL 尝试连接该 URL
- **AND** 返回连接状态和可能的错误信息

#### Scenario: 批量连通性测试
- **WHEN** 用户提交多个 IP 地址进行批量测试
- **THEN** 系统 SHALL 并发测试所有 IP
- **AND** 返回每个 IP 的测试结果

---

### Requirement: 扫描任务历史

系统 SHALL 保留扫描任务历史，便于用户查看之前的扫描结果。

#### Scenario: 查询扫描任务列表
- **WHEN** 用户调用 `GET /api/camera-discovery/scans`
- **THEN** 系统 SHALL 返回最近的扫描任务列表
- **AND** 每个任务包含：任务 ID、网段、状态、创建时间、完成时间、发现设备数

#### Scenario: 删除扫描任务
- **WHEN** 管理员调用 `DELETE /api/camera-discovery/scan/{taskId}`
- **THEN** 系统 SHALL 删除指定的扫描任务
- **AND** 同时删除关联的扫描结果

#### Scenario: 扫描结果保留期限
- **WHEN** 扫描任务完成超过保留期限（默认 7 天）
- **THEN** 系统 SHALL 自动清理旧任务和结果
- **AND** 释放存储空间

---

### Requirement: 扫描配置

系统 SHALL 提供可配置的扫描参数，以适应不同网络环境。

#### Scenario: 配置扫描超时时间
- **WHEN** 管理员修改扫描超时配置
- **THEN** 配置 SHALL 在下次扫描任务中生效
- **AND** 支持的配置包括：IP 连接超时、设备识别超时

#### Scenario: 配置扫描并发数
- **WHEN** 管理员修改扫描并发数配置
- **THEN** 配置 SHALL 在下次扫描任务中生效
- **AND** 范围限制在 10-200 之间

#### Scenario: 配置常见端口列表
- **WHEN** 管理员修改扫描端口列表
- **THEN** 系统 SHALL 在扫描时使用新的端口列表
- **AND** 默认端口包括：554(RTSP)、80(HTTP)、8080(HTTP)、8554(RTSP)
