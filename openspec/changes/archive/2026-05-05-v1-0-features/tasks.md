# v1.0 功能实现任务清单

## 1. 区域管理 (Region Management)

### 1.1 数据库层
- [x] 1.1.1 创建 Region 实体类，包含 parent_id 递归关系
- [x] 1.1.2 创建 RegionRepository 仓库接口
- [x] 1.1.3 实现软删除（deleted_at 时间戳）
- [x] 1.1.4 创建区域树形结构查询方法

### 1.2 领域层
- [x] 1.2.1 创建 RegionService 服务接口
- [x] 1.2.2 实现区域 CRUD 操作
- [x] 1.2.3 实现区域移动逻辑（带循环引用检测）
- [x] 1.2.4 实现递归删除（带 force 参数）
- [x] 1.2.5 实现区域查询和搜索（flat/tree、名称搜索、层级过滤）

### 1.3 应用层
- [x] 1.3.1 创建 RegionController REST 控制器
- [x] 1.3.2 实现区域树形结构 API（GET /api/v1/regions）
- [x] 1.3.3 实现区域移动 API（PATCH /api/v1/regions/{id}/move）
- [x] 1.3.4 实现区域摄像头关联 API（GET /api/v1/regions/{id}/cameras）
- [x] 1.3.5 实现区域统计 API（GET /api/v1/regions/{id}/stats）

### 1.4 前端
- [x] 1.4.1 创建区域管理页面组件
- [x] 1.4.2 实现区域树形展示组件（支持拖拽）
- [x] 1.4.3 实现区域 CRUD 表单
- [x] 1.4.4 实现区域移动交互

---

## 2. CDN节点管理 (CDN Node Management)

### 2.1 数据库层
- [x] 2.1.1 创建 CdnNode 实体类
- [x] 2.1.2 创建 CdnNodeLoad 负载历史实体
- [x] 2.1.3 创建 CdnNodeRepository 仓库接口
- [x] 2.1.4 创建负载数据存储接口

### 2.2 领域层
- [x] 2.2.1 创建 CdnNodeService 服务接口
- [x] 2.2.2 实现 CDN 节点 CRUD 操作
- [x] 2.2.3 实现节点连通性测试
- [x] 2.2.4 实现 WLC（加权最小连接数）选择算法
- [x] 2.2.5 实现地理邻近性 + WLC 混合算法

### 2.3 基础设施层
- [x] 2.3.1 创建健康检查定时任务（每30秒）
- [x] 2.3.2 实现节点状态更新逻辑
- [x] 2.3.3 实现负载数据上报 API

### 2.4 应用层
- [x] 2.4.1 创建 CdnNodeController REST 控制器
- [x] 2.4.2 实现节点注册 API（POST /api/v1/cdn-nodes）
- [x] 2.4.3 实现节点列表 API（GET /api/v1/cdn-nodes）
- [x] 2.4.4 实现节点详情 API（GET /api/v1/cdn-nodes/{id}）
- [x] 2.4.5 实现节点负载查询 API（GET /api/v1/cdn-nodes/{id}/load）
- [x] 2.4.6 实现节点统计 API（GET /api/v1/cdn-nodes/{id}/stats）
- [x] 2.4.7 实现负载上报 API（POST /api/v1/cdn-nodes/{id}/report）

### 2.5 前端
- [x] 2.5.1 创建 CDN 节点管理页面
- [x] 2.5.2 实现节点状态监控面板
- [x] 2.5.3 实现负载信息展示图表
- [x] 2.5.4 实现节点添加/编辑表单

---

## 3. 告警规则配置 (Alert Rule Configuration)

### 3.1 数据库层
- [x] 3.1.1 创建 AlertRule 实体类（JSON 格式条件存储）
- [x] 3.1.2 创建 AlertCondition 条件实体
- [x] 3.1.3 创建 AlertNotification 通知配置实体
- [x] 3.1.4 创建 AlertEscalation 升级配置实体
- [x] 3.1.5 创建 AlertRuleTemplate 模板实体

### 3.2 领域层
- [x] 3.2.1 创建 AlertRuleService 服务接口
- [x] 3.2.2 实现告警规则 CRUD 操作
- [x] 3.2.3 实现 JSON 条件解析引擎（支持 AND/OR/嵌套）
- [x] 3.2.4 实现条件评估逻辑
- [x] 3.2.5 实现冷却期（Cooldown）管理
- [x] 3.2.6 实现规则测试功能

### 3.3 通知层
- [x] 3.3.1 创建 NotificationChannel 接口
- [x] 3.3.2 实现邮件通知渠道（EmailNotificationChannel）
- [x] 3.3.3 实现短信通知渠道（SmsNotificationChannel）
- [x] 3.3.4 实现 WebSocket 应用内通知渠道
- [x] 3.3.5 实现 Webhook 通知渠道
- [x] 3.3.6 实现通知发送失败重试机制

### 3.4 升级层
- [x] 3.4.1 创建 EscalationService 升级服务
- [x] 3.4.2 实现升级定时检查任务
- [x] 3.4.3 实现多级升级逻辑

### 3.5 应用层
- [x] 3.5.1 创建 AlertRuleController REST 控制器
- [x] 3.5.2 实现规则列表 API（GET /api/v1/alert-rules）
- [x] 3.5.3 实现规则详情 API（GET /api/v1/alert-rules/{id}）
- [x] 3.5.4 实现规则创建 API（POST /api/v1/alert-rules）
- [x] 3.5.5 实现规则更新 API（PUT /api/v1/alert-rules/{id}）
- [x] 3.5.6 实现规则删除 API（DELETE /api/v1/alert-rules/{id}）
- [x] 3.5.7 实现规则启用/禁用 API（PATCH /api/v1/alert-rules/{id}/enable|disable）
- [x] 3.5.8 实现规则测试 API（POST /api/v1/alert-rules/{id}/test）
- [x] 3.5.9 实现规则模板列表 API（GET /api/v1/alert-rules/templates）
- [x] 3.5.10 实现规则历史查询 API（GET /api/v1/alert-rules/{id}/history）

### 3.6 前端
- [x] 3.6.1 创建告警规则管理页面
- [x] 3.6.2 实现规则条件编辑器（支持 AND/OR 可视化）
- [x] 3.6.3 实现通知渠道配置组件
- [x] 3.6.4 实现升级配置组件
- [x] 3.6.5 实现规则模板选择器
- [x] 3.6.6 实现规则测试结果展示

---

## 4. 系统设置 (System Settings)

### 4.1 数据库层
- [x] 4.1.1 创建 SystemConfig 实体类
- [x] 4.1.2 创建 ConfigCategory 分类实体
- [x] 4.1.3 创建 ConfigHistory 配置变更历史实体
- [x] 4.1.4 创建 SystemConfigRepository 仓库接口

### 4.2 领域层
- [x] 4.2.1 创建 SystemConfigService 服务接口
- [x] 4.2.2 实现配置分组管理
- [x] 4.2.3 实现配置值验证逻辑
- [x] 4.2.4 实现配置回滚功能

### 4.3 应用层
- [x] 4.3.1 创建 SystemConfigController REST 控制器
- [x] 4.3.2 实现配置获取 API（GET /api/v1/system-configs）
- [x] 4.3.3 实现配置更新 API（PUT /api/v1/system-configs）
- [x] 4.3.4 实现配置重置 API（POST /api/v1/system-configs/reset）
- [x] 4.3.5 实现配置历史查询 API（GET /api/v1/system-configs/history）
- [x] 4.3.6 实现配置回滚 API（POST /api/v1/system-configs/rollback）
- [x] 4.3.7 实现邮件配置测试 API（POST /api/v1/system-configs/email/test）

### 4.4 前端
- [x] 4.4.1 创建系统设置页面
- [x] 4.4.2 实现视频参数配置面板
- [x] 4.4.3 实现录像设置配置面板
- [x] 4.4.4 实现负载均衡配置面板
- [x] 4.4.5 实现安全策略配置面板
- [x] 4.4.6 实现通知设置配置面板
- [x] 4.4.7 实现配置变更历史展示
- [x] 4.4.8 实现配置回滚功能

---

## 5. 数据分析与报表 (Data Analysis and Reports)

### 5.1 数据库层
- [x] 5.1.1 创建 AnalyticsData 实体类（按日聚合）
- [x] 5.1.2 创建 ReportSubscription 报表订阅实体
- [x] 5.1.3 创建 AnalyticsRepository 仓库接口
- [x] 5.1.4 实现时间序列数据查询优化

### 5.2 领域层
- [x] 5.2.1 创建 AnalyticsService 服务接口
- [x] 5.2.2 实现设备利用率统计（在线率、故障率、MTBF、MTTR）
- [x] 5.2.3 实现网络带宽分析（趋势、峰值）
- [x] 5.2.4 实现存储容量分析（趋势、增长率）
- [x] 5.2.5 实现故障统计分析（类型分布、趋势）

### 5.3 报表层
- [x] 5.3.1 创建 ReportService 报表服务接口
- [x] 5.3.2 实现 Excel 报表生成（使用 Apache POI）
- [x] 5.3.3 实现报表模板定义
- [x] 5.3.4 创建 ReportSubscriptionService 订阅服务
- [x] 5.3.5 实现定时报表发送任务

### 5.4 应用层
- [x] 5.4.1 创建 AnalyticsController REST 控制器
- [x] 5.4.2 实现设备利用率 API（GET /api/v1/analytics/device-usage）
- [x] 5.4.3 实现带宽分析 API（GET /api/v1/analytics/bandwidth）
- [x] 5.4.4 实现存储分析 API（GET /api/v1/analytics/storage）
- [x] 5.4.5 实现故障统计 API（GET /api/v1/analytics/failures）
- [x] 5.4.6 实现报表导出 API（POST /api/v1/analytics/reports/export）
- [x] 5.4.7 实现订阅管理 API（CRUD）

### 5.5 前端
- [x] 5.5.1 创建数据分析页面
- [x] 5.5.2 实现设备利用率图表（折线图/柱状图）
- [x] 5.5.3 实现带宽趋势图表
- [x] 5.5.4 实现存储容量图表
- [x] 5.5.5 实现故障热力图
- [x] 5.5.6 创建报表配置页面
- [x] 5.5.7 实现报表订阅管理
- [x] 5.5.8 实现实时数据 WebSocket 推送

---

## 6. 仪表盘增强 (Dashboard Redesign)

### 6.1 后端
- [x] 6.1.1 扩展 DashboardStats 包含新功能统计
- [x] 6.1.2 添加区域统计到仪表盘
- [x] 6.1.3 添加 CDN 节点统计到仪表盘
- [x] 6.1.4 添加活跃告警统计到仪表盘

### 6.2 前端
- [x] 6.2.1 更新仪表盘布局
- [x] 6.2.2 添加区域管理快捷入口
- [x] 6.2.3 添加 CDN 节点状态卡片
- [x] 6.2.4 添加告警统计卡片
- [x] 6.2.5 添加系统设置快捷入口
- [x] 6.2.6 实现数据分析快捷入口

---

## 7. 技术基础设施

### 7.1 数据库迁移
- [x] 7.1.1 创建 regions 表
- [x] 7.1.2 创建 cdn_nodes 表
- [x] 7.1.3 创建 cdn_node_load_history 表
- [x] 7.1.4 创建 alert_rules 表
- [x] 7.1.5 创建 alert_conditions 表
- [x] 7.1.6 创建 alert_notifications 表
- [x] 7.1.7 创建 alert_escalations 表
- [x] 7.1.8 创建 alert_rule_templates 表
- [x] 7.1.9 创建 system_configs 表
- [x] 7.1.10 创建 config_history 表
- [x] 7.1.11 创建 analytics_data 表
- [x] 7.1.12 创建 report_subscriptions 表
- [x] 7.1.13 添加摄像头表 region_id 外键

### 7.2 WebSocket 配置
- [x] 7.2.1 配置告警通知 WebSocket 端点
- [x] 7.2.2 配置数据分析实时推送端点

### 7.3 定时任务
- [x] 7.3.1 配置 CDN 健康检查任务（每30秒）
- [x] 7.3.2 配置告警评估任务
- [x] 7.3.3 配置告警升级检查任务
- [x] 7.3.4 配置报表定时发送任务
- [x] 7.3.5 配置数据聚合任务（每日）

### 7.4 文档更新
- [x] 7.4.1 更新 AI2AI/后端架构信息.md
- [x] 7.4.2 更新 AI2AI/前端架构信息.md
- [x] 7.4.3 更新 AI2AI/协议和数据.md
