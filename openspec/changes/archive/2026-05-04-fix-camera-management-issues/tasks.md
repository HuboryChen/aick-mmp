## 1. Database Migration

- [x] 1.1 添加 `deleted_at` 字段到 `cameras` 表
- [x] 1.2 创建 Flyway 或 Liquibase 迁移脚本
- [x] 1.3 验证迁移脚本可逆

## 2. NodeWeightCalculator 服务

- [x] 2.1 创建 `NodeWeightCalculator` 服务类
- [x] 2.2 修复逻辑运算符优先级问题
- [x] 2.3 实现四因子权重计算逻辑
- [x] 2.4 添加同区域加成计算
- [x] 2.5 编写单元测试覆盖所有场景

## 3. CameraServiceImpl 修复

- [x] 3.1 移除内嵌的 `calculateNodeWeight` 方法
- [x] 3.2 注入并使用 `NodeWeightCalculator` 服务
- [x] 3.3 实现软删除逻辑（设置 `deletedAt`）
- [x] 3.4 修改查询方法自动过滤已删除记录
- [x] 3.5 实现恢复已删除摄像头功能
- [x] 3.6 优化 N+1 查询（添加 `@EntityGraph`）
- [x] 3.7 修复批量更新的容量检查逻辑

## 4. CameraController 修复

- [x] 4.1 修复 `@PreAuthorize` 注解格式（添加缺失的引号）
- [x] 4.2 修改创建摄像头返回 201 状态码
- [x] 4.3 实现 `getOnlineCameras` 方法
- [x] 4.4 实现 `stopCameraStream` 方法
- [x] 4.5 实现 `testConnection` 方法

## 5. EdgeNodeFailoverServiceImpl 修复

- [x] 5.1 移除内嵌的 `calculateBaseNodeWeight` 方法
- [x] 5.2 注入并使用 `NodeWeightCalculator` 服务
- [x] 5.3 验证故障转移流程使用正确的权重计算

## 6. EdgeNodeServiceImpl 优化

- [x] 6.1 复用 `RestTemplate` 或使用 `@Bean` 管理

## 7. 集成测试

- [x] 7.1 编写软删除功能集成测试
- [x] 7.2 编写权重计算服务集成测试
- [x] 7.3 验证故障转移使用共享服务

## 8. 代码审查

- [x] 8.1 审查所有修改的文件
- [x] 8.2 验证 linter 检查通过
- [x] 8.3 确认没有引入新的问题
