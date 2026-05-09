# Camera Batch Import Specification

## ADDED Requirements

### Requirement: 批量导入模板管理

系统 SHALL 提供标准的批量导入模板，用户下载后填写摄像头信息。

#### Scenario: 下载导入模板
- **WHEN** 用户调用 `GET /api/camera-batch-import/template`
- **THEN** 系统 SHALL 返回 Excel 格式的导入模板文件
- **AND** 模板包含必要的列：摄像头名称、IP 地址、用户名、密码、地区、品牌、型号

#### Scenario: 模板包含说明
- **WHEN** 用户打开导入模板
- **THEN** 模板 SHALL 包含示例数据
- **AND** 每个列 SHALL 包含格式说明和必填标记

#### Scenario: 支持多种格式
- **WHEN** 用户需要导入数据
- **THEN** 系统 SHALL 支持 Excel (.xlsx) 和 CSV 格式
- **AND** 根据 Content-Type 自动识别格式

---

### Requirement: 批量导入任务管理

系统 SHALL 支持创建和跟踪批量导入任务，处理大量摄像头数据。

#### Scenario: 创建批量导入任务
- **WHEN** 用户调用 `POST /api/camera-batch-import/import` 并上传文件
- **THEN** 系统 SHALL 创建导入任务
- **AND** 解析文件内容验证格式
- **AND** 返回任务 ID 和初步验证结果

#### Scenario: 查询导入任务状态
- **WHEN** 用户调用 `GET /api/camera-batch-import/import/{taskId}`
- **THEN** 系统 SHALL 返回任务状态
- **AND** 状态包括：PENDING、VALIDATING、IMPORTING、COMPLETED、FAILED
- **AND** 包含进度百分比和处理结果统计

#### Scenario: 取消导入任务
- **WHEN** 用户调用 `POST /api/camera-batch-import/import/{taskId}/cancel`
- **AND** 任务状态为 PENDING 或 VALIDATING
- **THEN** 系统 SHALL 取消导入任务
- **AND** 返回取消结果

---

### Requirement: 数据验证

系统 SHALL 在导入前验证数据的有效性，确保数据的正确性。

#### Scenario: 验证必填字段
- **WHEN** 系统解析导入文件
- **THEN** 系统 SHALL 验证每行数据的必填字段
- **AND** 必填字段包括：摄像头名称、IP 地址、地区
- **AND** 如果必填字段缺失， SHALL 标记该行为错误

#### Scenario: 验证 IP 地址格式
- **WHEN** 系统解析 IP 地址字段
- **THEN** 系统 SHALL 验证 IP 地址格式是否正确
- **AND** 支持 IPv4 格式验证
- **AND** 如果格式错误， SHALL 标记该行为错误

#### Scenario: 验证地区有效性
- **WHEN** 系统解析地区字段
- **THEN** 系统 SHALL 验证地区名称或 ID 是否存在
- **AND** 如果地区不存在， SHALL 标记该行为错误

#### Scenario: 验证品牌型号匹配
- **WHEN** 系统解析品牌和型号字段
- **THEN** 系统 SHALL 验证是否存在对应的配置模板
- **AND** 如果没有匹配模板， SHALL 标记为警告（允许继续导入）

---

### Requirement: 模板自动匹配

系统 SHALL 根据导入数据中的品牌和型号自动匹配配置模板。

#### Scenario: 自动匹配配置模板
- **WHEN** 导入数据包含品牌和型号信息
- **THEN** 系统 SHALL 查找匹配的配置模板
- **AND** 如果找到完全匹配的模板， SHALL 自动应用
- **AND** 生成对应的协议类型和 URL 模板

#### Scenario: 模板匹配失败处理
- **WHEN** 导入数据包含品牌和型号信息
- **AND** 系统找不到匹配的配置模板
- **THEN** 系统 SHALL 使用默认配置
- **AND** 标记该行为警告，提示用户后续手动配置

#### Scenario: 仅提供品牌时匹配
- **WHEN** 导入数据只包含品牌信息
- **THEN** 系统 SHALL 查找该品牌下最常用的模板
- **AND** 如果找到， SHALL 应用该模板

---

### Requirement: 边缘节点自动分配

系统 SHALL 根据摄像头的地区信息自动分配最优边缘节点。

#### Scenario: 按地区自动分配边缘节点
- **WHEN** 导入数据包含地区信息
- **AND** 系统执行批量导入
- **THEN** 系统 SHALL 查找该地区的在线边缘节点
- **AND** 选择负载最低的节点自动分配
- **AND** 如果无可用节点， SHALL 标记为待分配状态

#### Scenario: 手动指定边缘节点
- **WHEN** 导入数据包含边缘节点 ID 或名称
- **THEN** 系统 SHALL 使用指定的边缘节点
- **AND** 验证节点存在且在线

#### Scenario: 批量分配规则配置
- **WHEN** 管理员配置批量导入分配规则
- **THEN** 系统 SHALL 根据规则自动分配边缘节点
- **AND** 支持的规则包括：优先同地区、优先低负载、随机分配

---

### Requirement: 导入结果报告

系统 SHALL 提供详细的导入结果报告，显示成功、失败和警告的记录。

#### Scenario: 生成导入结果报告
- **WHEN** 批量导入任务完成
- **THEN** 系统 SHALL 生成详细的结果报告
- **AND** 报告包含：总数、成功数、失败数、警告数
- **AND** 列出所有错误行的原因和位置

#### Scenario: 下载错误报告
- **WHEN** 导入任务完成且有错误
- **AND** 用户点击"下载错误报告"
- **THEN** 系统 SHALL 生成包含错误信息的文件
- **AND** 文件包含原始数据、错误原因、修正建议

#### Scenario: 重试失败记录
- **WHEN** 用户从错误报告中修正数据
- **AND** 重新上传修正后的文件
- **THEN** 系统 SHALL 跳过已成功导入的记录
- **AND** 只处理失败和警告的记录

---

### Requirement: 导入任务历史

系统 SHALL 保留批量导入任务历史，便于审计和追踪。

#### Scenario: 查询导入任务列表
- **WHEN** 用户调用 `GET /api/camera-batch-import/imports`
- **THEN** 系统 SHALL 返回最近的导入任务列表
- **AND** 每个任务包含：任务 ID、文件名、状态、创建时间、完成时间、处理结果

#### Scenario: 查看导入任务详情
- **WHEN** 用户调用 `GET /api/camera-batch-import/import/{taskId}/details`
- **THEN** 系统 SHALL 返回任务的详细信息
- **AND** 包括：原始数据、处理结果、创建的摄像头列表

#### Scenario: 删除导入任务
- **WHEN** 管理员调用 `DELETE /api/camera-batch-import/import/{taskId}`
- **THEN** 系统 SHALL 删除指定的导入任务
- **AND** 同时删除关联的临时文件

---

### Requirement: 导入进度通知

系统 SHALL 在导入过程中提供实时进度通知。

#### Scenario: WebSocket 进度推送
- **WHEN** 导入任务状态变化时
- **THEN** 系统 SHALL 通过 WebSocket 推送进度更新
- **AND** 推送消息包含：任务 ID、当前状态、进度百分比、处理记录数

#### Scenario: 完成后通知
- **WHEN** 导入任务完成（成功或失败）
- **THEN** 系统 SHALL 推送完成通知
- **AND** 包含结果摘要

#### Scenario: 错误时通知
- **WHEN** 导入任务失败
- **THEN** 系统 SHALL 立即推送错误通知
- **AND** 包含错误原因和堆栈信息（仅管理员可见）
