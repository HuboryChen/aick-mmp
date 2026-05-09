# Implementation Tasks

## 1. 数据库迁移

- [x] 1.1 为 Recording 表添加 deleted_at 列
- [x] 1.2 为 Recording 表添加 orphaned_at 列
- [x] 1.3 为 Recording 表添加 orphaned_by 列
- [x] 1.4 创建 Recording 表 deleted_at 和 orphaned_at 的索引
- [x] 1.5 创建 RecordingSchedule 表（新建）
- [x] 1.6 添加 RecordingSchedule 表的时间槽关联表

## 2. 后端 - Recording 软删除支持

- [x] 2.1 在 Recording.java 实体添加 deletedAt 字段和 @Where 注解
- [x] 2.2 在 Recording.java 实体添加 orphanedAt、orphanedBy 字段
- [x] 2.3 在 RecordingRepository 添加软删除和恢复方法
- [x] 2.4 在 RecordingService 实现 softDelete 方法
- [x] 2.5 在 RecordingService 实现 restore 方法
- [x] 2.6 在 RecordingService 添加 includeDeleted 参数支持
- [x] 2.7 添加软删除相关单元测试 ✓

## 3. 后端 - 摄像头删除级联录像处理

- [x] 3.1 在 CameraServiceImpl 删除方法中添加录像级联处理逻辑
- [x] 3.2 添加 markOrphanedRecordings 方法
- [x] 3.3 添加孤立录像查询接口
- [x] 3.4 添加孤立录像清理接口
- [x] 3.5 添加级联处理单元测试 ✓

## 4. 后端 - 摄像头统计聚合API

- [x] 4.1 在 CameraController 添加 /statistics/summary 接口
- [x] 4.2 在 CameraService 添加统计查询方法
- [x] 4.3 实现按区域统计功能
- [x] 4.4 实现按节点统计功能
- [x] 4.5 添加统计接口单元测试 ✓

## 5. 后端 - API权限优化

- [x] 5.1 修改 CameraController.search 的权限配置
- [x] 5.2 添加 ADMIN、OPERATOR、VIEWER 角色支持
- [x] 5.3 统一批量操作返回值格式 ✓
- [x] 5.4 添加权限修改集成测试 ✓

## 6. 后端 - 边缘节点状态双向同步

- [x] 6.1 创建 CameraStatusReport DTO
- [x] 6.2 创建 EdgeHeartbeatRequest 扩展类（包含摄像头状态）
- [x] 6.3 在 EdgeNodeService 添加摄像头状态处理方法
- [x] 6.4 修改边缘节点 SDK 添加摄像头状态上报能力
- [x] 6.5 添加状态同步单元测试

## 7. 后端 - 录像计划管理

- [x] 7.1 创建 RecordingSchedule 实体
- [x] 7.2 创建 TimeSlot 嵌入类
- [x] 7.3 创建 RecordingScheduleRepository
- [x] 7.4 创建 RecordingScheduleService
- [x] 7.5 在 CameraController 添加录像计划 CRUD 接口
- [x] 7.6 实现录像计划同步接口（供边缘节点调用）
- [x] 7.7 添加录像计划管理单元测试

## 8. 后端 - 移动侦测录像

- [x] 8.1 扩展 RecordingSchedule 实体添加移动侦测字段
- [x] 8.2 创建 MotionEvent 实体
- [x] 8.3 创建 MotionEventRepository
- [x] 8.4 添加移动侦测事件上报接口
- [x] 8.5 添加移动侦测历史查询接口
- [x] 8.6 在边缘节点 SDK 添加移动侦测事件生成器
- [x] 8.7 添加移动侦测录像集成测试

## 9. 前端 - 录像管理增强

- [x] 9.1 在 CameraStream.js 添加录像列表入口
- [x] 9.2 添加已删除录像查询和恢复功能
- [x] 9.3 添加孤立录像展示和清理功能
- [x] 9.4 添加统计概览组件

## 10. 前端 - 录像计划配置页面

- [x] 10.1 创建 RecordingSchedulePage 页面
- [x] 10.2 添加录像计划列表组件
- [x] 10.3 添加录像计划表单（支持定时、移动侦测模式）
- [x] 10.4 添加时间槽配置组件
- [x] 10.5 添加灵敏度配置组件
- [x] 10.6 添加移动侦测历史查看组件

## 11. 集成测试与验证

- [x] 11.1 软删除流程端到端测试
- [x] 11.2 摄像头删除级联录像测试
- [x] 11.3 统计API性能和准确性测试
- [x] 11.4 边缘节点状态同步测试
- [x] 11.5 录像计划创建和同步测试
- [x] 11.6 移动侦测录像功能测试
