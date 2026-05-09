# Camera Config Templates Specification

## ADDED Requirements

### Requirement: 配置模板管理

系统 SHALL 支持摄像头配置模板的创建、查询、更新和删除操作。每个模板定义了特定品牌/型号摄像头的标准配置，包括协议类型、默认端口、URL 路径模板和预设参数。

#### Scenario: 创建配置模板
- **WHEN** 管理员调用 `POST /api/camera-config-templates` 并提交模板信息
- **THEN** 系统 SHALL 创建新的配置模板
- **AND** 返回创建的模板包含生成的 ID
- **AND** 模板信息包括：品牌名称、型号、协议类型、默认端口、URL 路径模板、预设参数

#### Scenario: 查询所有配置模板
- **WHEN** 用户调用 `GET /api/camera-config-templates`
- **THEN** 系统 SHALL 返回所有可用的配置模板列表
- **AND** 每个模板包含基本信息和使用计数

#### Scenario: 按品牌查询配置模板
- **WHEN** 用户调用 `GET /api/camera-config-templates?brand={brandName}`
- **THEN** 系统 SHALL 返回指定品牌的所有配置模板

#### Scenario: 更新配置模板
- **WHEN** 管理员调用 `PUT /api/camera-config-templates/{id}` 并提交更新信息
- **THEN** 系统 SHALL 更新指定的配置模板
- **AND** 返回更新后的模板信息

#### Scenario: 删除配置模板
- **WHEN** 管理员调用 `DELETE /api/camera-config-templates/{id}`
- **THEN** 系统 SHALL 软删除指定的配置模板（设置 deletedAt）
- **AND** 如果该模板已被摄像头使用， SHALL 返回错误提示

---

### Requirement: URL 模板变量替换

配置模板中的 URL 路径 SHALL 支持变量占位符，在使用模板时自动替换为实际值。

#### Scenario: 使用模板生成完整 URL
- **WHEN** 用户选择配置模板并填写 IP 地址、用户名、密码等信息
- **THEN** 系统 SHALL 使用模板变量替换生成完整的连接 URL
- **AND** 支持的变量包括：{ip}、{port}、{username}、{password}、{channel}、{stream}

#### Scenario: RTSP 模板示例
- **WHEN** 模板 URL 路径为 `rtsp://{username}:{password}@{ip}:{port}/h264/ch1/main/av_stream`
- **AND** 用户输入 IP 为 192.168.1.101，用户名为 admin，密码为 123456，端口为 554
- **THEN** 系统 SHALL 生成 URL：`rtsp://admin:123456@192.168.1.101:554/h264/ch1/main/av_stream`

#### Scenario: 变量缺失时使用默认值
- **WHEN** URL 模板包含可选变量
- **AND** 用户未提供该变量的值
- **THEN** 系统 SHALL 使用模板中定义的默认值

---

### Requirement: 预置配置模板

系统 SHALL 预置常见摄像头品牌的配置模板，用户开箱即用。

#### Scenario: 系统初始化时加载预置模板
- **WHEN** 系统首次启动或检测到无预置模板时
- **THEN** 系统 SHALL 自动加载预置的摄像头品牌模板
- **AND** 预置模板包括：海康威视、大华、宇视、天地伟业等主流品牌

#### Scenario: 预置模板不可删除
- **WHEN** 管理员尝试删除预置模板
- **THEN** 系统 SHALL 返回错误提示预置模板不可删除
- **AND** 允许管理员复制预置模板并创建自定义模板

#### Scenario: 预置模板可更新
- **WHEN** 系统版本升级包含新的预置模板
- **THEN** 系统 SHALL 自动合并新的预置模板
- **AND** 不覆盖用户已修改的自定义模板

---

### Requirement: 模板使用统计

系统 SHALL 跟踪每个配置模板的使用情况，用于统计和优化。

#### Scenario: 记录模板使用
- **WHEN** 用户使用某个模板创建摄像头
- **THEN** 系统 SHALL 增加该模板的使用计数
- **AND** 记录最后使用时间

#### Scenario: 查询模板使用统计
- **WHEN** 管理员调用 `GET /api/camera-config-templates/statistics`
- **THEN** 系统 SHALL 返回所有模板的使用统计信息
- **AND** 包括：使用次数、最后使用时间、关联的摄像头数量

---

### Requirement: 模板验证

系统 SHALL 验证配置模板的有效性，确保模板格式正确且可用。

#### Scenario: 创建模板时验证 URL 格式
- **WHEN** 管理员提交包含 URL 路径模板的配置模板
- **THEN** 系统 SHALL 验证 URL 模板格式是否正确
- **AND** 变量占位符格式必须为 {variable_name}
- **AND** 如果格式错误， SHALL 返回详细的错误信息

#### Scenario: 测试模板生成 URL
- **WHEN** 管理员调用 `POST /api/camera-config-templates/{id}/test` 并提供测试参数
- **THEN** 系统 SHALL 使用模板生成测试 URL
- **AND** 返回生成的完整 URL 供验证

---

### Requirement: 添加摄像头时使用模板

摄像头添加表单 SHALL 支持从模板加载配置，简化用户操作。

#### Scenario: 选择模板自动填充表单
- **WHEN** 用户在添加摄像头表单中选择配置模板
- **THEN** 系统 SHALL 自动填充协议类型、默认端口、URL 路径模板
- **AND** 用户只需填写 IP 地址、用户名、密码等必要信息

#### Scenario: 模板参数填充后可编辑
- **WHEN** 用户选择模板后表单被自动填充
- **THEN** 系统 SHALL 允许用户修改任何自动填充的字段
- **AND** 用户可以根据实际情况调整配置

#### Scenario: 根据模板生成最终 URL
- **WHEN** 用户提交添加摄像头表单且选择了模板
- **THEN** 系统 SHALL 使用模板变量替换生成最终的 connectionUrl
- **AND** 保存到 Camera 实体

---

### Requirement: 模板导入导出

系统 SHALL 支持配置模板的导入导出，便于跨环境迁移和备份。

#### Scenario: 导出配置模板
- **WHEN** 管理员调用 `GET /api/camera-config-templates/export`
- **THEN** 系统 SHALL 返回所有配置模板的 JSON 格式数据
- **AND** 数据格式包含所有模板的完整信息

#### Scenario: 导入配置模板
- **WHEN** 管理员调用 `POST /api/camera-config-templates/import` 并上传 JSON 文件
- **THEN** 系统 SHALL 解析文件并验证模板格式
- **AND** 成功的模板被导入，失败的返回错误信息

#### Scenario: 导入时处理 ID 冲突
- **WHEN** 导入的模板 ID 与现有模板冲突
- **THEN** 系统 SHALL 为导入的模板生成新的 ID
- **AND** 保持模板的其他属性不变
