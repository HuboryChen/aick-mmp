## Context

### 当前状态

摄像头管理模块（Camera Management）是系统的核心业务模块，负责管理摄像头设备、边缘节点关联、视频流分发和录像存储。当前系统架构：

- **中央服务**：处理业务逻辑、API接口、流媒体服务（Janus集成）
- **边缘服务**：部署在现场的节点，负责本地摄像头控制和视频采集
- **数据层**：MySQL存储，JPA实体模型

### 已知问题

1. **Recording实体缺少软删除**：无deletedAt字段，删除后无法恢复
2. **删除摄像头时未处理录像**：关联录像保留但逻辑失效
3. **API权限过严**：搜索API仅OPERATOR可用，限制了其他角色使用
4. **缺乏统计能力**：无法快速获取摄像头状态分布、节点负载等信息
5. **状态同步不完整**：仅节点心跳，缺少摄像头级别状态上报

### 约束条件

- 需要向后兼容，不破坏现有API契约
- 数据库迁移需要支持回滚
- 边缘节点升级需要平滑过渡

## Goals / Non-Goals

**Goals:**
- 实现Recording软删除和级联删除逻辑
- 优化API权限设计，扩大可用范围
- 新增摄像头统计聚合API
- 实现边缘节点↔摄像头状态双向同步
- 设计录像计划管理基础架构

**Non-Goals:**
- 不实现完整的录像计划执行引擎（由边缘节点独立实现）
- 不修改现有的流媒体协议（Janus/WebRTC）
- 不涉及用户权限体系的根本性改造

## Decisions

### D1: Recording软删除字段设计

**决策**：为Recording添加`is_deleted`布尔字段和`deleted_at`时间戳组合

```java
@Where(clause = "is_deleted = false")
public class Recording { ... }

private Boolean isDeleted = false;      // 主查询条件，效率高
private LocalDateTime deletedAt;        // 记录删除时间，用于清理判断
```

**数据库索引**：
```sql
-- 主查询索引（高效率布尔索引）
ALTER TABLE recordings ADD INDEX idx_recordings_deleted (is_deleted);

-- 清理任务索引（复合索引）
ALTER TABLE recordings ADD INDEX idx_recordings_deleted_at (is_deleted, deleted_at);
```

**理由**：
- `is_deleted`布尔索引查询效率高于datetime
- 保留`deletedAt`用于30天清理策略和审计
- `@Where`注解自动在所有JPA查询添加过滤

**替代方案对比**：
| 方案 | 查询效率 | 清理支持 | 最终选择 |
|------|---------|---------|---------|
| 仅datetime | 低 | 高 | ❌ |
| 仅boolean | 高 | 低 | ❌ |
| boolean + datetime | 高 | 高 | ✅ |

### D2: 删除摄像头时的录像处理策略

**决策**：采用软删除+孤立标记+定时清理策略

```java
// 删除摄像头时
public void deleteCamera(Long cameraId) {
    camera.setDeletedAt(now());
    camera.setDeletionType(DELETED_WITH_RECORDINGS);
    cameraRepository.save(camera);
    
    // 关联录像标记为孤立状态
    recordingRepository.markOrphanedByCameraId(cameraId, now());
}
```

**孤立录像清理策略**：
- **默认保留30天**（可通过配置调整）
- 每天凌晨2点定时任务执行清理
- 支持手动触发清理接口
- 孤立录像仍可查看和恢复

**理由**：
- 保留录像数据以防误删
- 30天窗口足够发现和处理误操作
- 避免数据库无限膨胀
- 支持手动清理作为补充

### D3: 统计API设计

**决策**：使用Redis缓存+定时刷新，返回多维度统计

```java
@GetMapping("/cameras/statistics/summary")
public CameraStatisticsDTO getStatistics(
    @RequestParam(required = false) Long regionId,
    @RequestParam(required = false) Long edgeNodeId,
    @RequestHeader(value = "X-Cache-Refresh", required = false) Boolean refresh
)
```

**返回结构**：
```json
{
  "total": 100,
  "byStatus": {
    "ONLINE": 85,
    "OFFLINE": 10,
    "ERROR": 3,
    "PENDING_ALLOCATION": 2
  },
  "byEdgeNode": [...],
  "totalRecordings": 500,
  "orphanedRecordings": 10,
  "cachedAt": "2026-05-03T23:50:00Z"
}
```

**缓存策略**：
- 每5分钟定时任务刷新统计到Redis
- API直接返回缓存结果
- 缓存TTL设为10分钟（留缓冲）
- 支持 `X-Cache-Refresh: true` 强制刷新

**理由**：
- 避免大表实时扫描影响数据库性能
- 5分钟延迟对统计看板场景可接受
- Redis缓存高并发下表现优秀
- 支持手动刷新满足实时需求

### D4: 边缘节点状态同步机制

**决策**：扩展心跳接口，边缘节点上报摄像头实际状态

```java
// 边缘节点心跳请求
public class EdgeHeartbeatRequest {
    private Long nodeId;
    private NodeStatus nodeStatus;
    private List<CameraStatusReport> cameraStatuses;
}

// 上报内容
public class CameraStatusReport {
    private Long cameraId;
    private CameraStatus status;             // ONLINE / OFFLINE / ERROR
    private String failureReason;
    private Integer bitrate;
    private Integer fps;
    private Integer droppedFrames;
    private LocalDateTime lastFrameTime;
}
```

**处理流程**：
```
Edge节点定时上报摄像头状态
      ↓
Central验证节点身份
      ↓
比较状态差异（old vs new）
      ↓
状态变更 → 更新 + 记录历史
      ↓
无变更 → 跳过
```

**同步策略**：
- **覆盖原则**：边缘节点上报状态 > 中央本地检测
- **变更记录**：每次状态变更记录历史，标记来源 `EDGE_NODE_REPORT`
- **超时检测**：未上报时标记为 UNKNOWN，启动本地检测

**理由**：
- 边缘节点最清楚摄像头真实状态
- 不改变现有心跳协议结构
- 扩展性强，可增加更多监控指标

### D5: 录像计划数据模型

**决策**：独立RecordingSchedule实体，支持分层配置

```java
@Entity
public class RecordingSchedule {
    @Id
    private Long id;
    private Long cameraId;
    
    @Enumerated
    private RecordingMode mode; // CONTINUOUS, SCHEDULED, MOTION_DETECTED
    
    @ElementCollection
    private List<TimeSlot> timeSlots;
    
    private Boolean enabled;
    
    // 移动侦测配置（分层）
    private Integer sensitivityLevel;        // 中央默认值 1-10
    private String detectionAreas;           // JSON数组
    private Integer postMotionDurationSeconds;
    
    // 本地覆盖标记
    private Boolean localOverride;          // 是否被节点本地覆盖
    private String localConfig;             // 本地生效的配置
    
    private LocalDateTime deletedAt;
}

@Embeddable
public class TimeSlot {
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
```

**分层配置策略**：
- 中央服务存储配置模板和默认值
- 边缘节点可本地调整以适应现场环境
- 同步时返回实际生效的配置（模板值或本地覆盖值）

**理由**：
- 解耦录像计划和录像记录
- 支持多种录像模式
- 便于边缘节点独立执行
- 边缘设备环境差异大，需要本地灵活性

### D6: 分布式定时任务

**决策**：使用ShedLock实现多节点定时任务协调

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.0.0</version>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-redis-spring</artifactId>
    <version>5.0.0</version>
</dependency>
```

**定时任务清单**：
| 任务 | 频率 | 锁名称 | 说明 |
|------|------|--------|------|
| 统计刷新 | 每5分钟 | `cameraStatisticsRefresh` | 刷新统计缓存 |
| 孤立录像清理 | 每天2点 | `orphanedRecordingCleanup` | 清理30天+孤立录像 |
| 待分配重试 | 每5分钟 | `pendingAllocationRetry` | 重试待分配摄像头 |
| 计划同步检查 | 每分钟 | `scheduleSyncCheck` | 检测计划变更 |

**任务示例**：
```java
@Scheduled(fixedRate = 300000)
@SchedulerLock(name = "cameraStatisticsRefresh", 
               lockAtMostFor = "5m",
               lockAtLeastFor = "1m")
public void refreshStatistics() {
    // ShedLock自动处理分布式锁
    // 其他节点会自动跳过
}
```

**理由**：
- ShedLock是Spring生态成熟方案
- 自动处理锁续期，防止任务超时未释放
- 注解方式，代码侵入小
- 支持Redis等多种存储后端

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 软删除后查询性能下降 | 添加布尔和时间复合索引 |
| 边缘节点上报延迟导致状态不一致 | 状态同步设置合理超时；本地优先展示 |
| 统计API大表扫描 | Redis缓存+5分钟定时刷新，避免实时扫描 |
| 录像计划配置复杂 | 提供UI引导；默认值简化配置 |
| 多节点定时任务冲突 | ShedLock分布式锁保证单节点执行 |
| Redis不可用 | 统计API降级到实时查询 |

## Migration Plan

### Phase 1: 数据库迁移
```sql
-- Recording表新增字段
ALTER TABLE recordings ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE recordings ADD COLUMN deleted_at DATETIME;
ALTER TABLE recordings ADD COLUMN orphaned_at DATETIME;
ALTER TABLE recordings ADD COLUMN orphaned_by BIGINT;

-- 新增索引
CREATE INDEX idx_recordings_deleted ON recordings(is_deleted);
CREATE INDEX idx_recordings_deleted_at ON recordings(is_deleted, deleted_at);

-- 新建表
CREATE TABLE recording_schedules (...);
CREATE TABLE recording_schedule_time_slots (...);
CREATE TABLE motion_events (...);
```

### Phase 2: 后端服务升级
1. 部署新版本CameraService（含软删除逻辑）
2. 验证录像查询不受影响
3. 验证删除流程正常工作

### Phase 3: 边缘节点升级
1. 分批升级边缘节点
2. 验证心跳上报新格式被正确处理

### Phase 4: 前端部署
1. 部署统计页面和筛选增强
2. 验证功能正常

**回滚策略**：数据库迁移不回滚（软删除字段向后兼容）；代码层面通过Feature Flag控制新功能

## Open Questions

~~1. **Q1**: 删除摄像头的关联录像是否需要定期自动清理？清理周期是多少天？~~
   ✅ **已确认**：定时清理（默认30天）+ 手动清理API

~~2. **Q2**: 统计API是否需要实时计算？还是可以接受5分钟延迟的缓存结果？~~
   ✅ **已确认**：定时缓存方案（每5分钟刷新到Redis）

~~3. **Q3**: 移动侦测灵敏度是否需要在中心配置，还是边缘节点本地配置？~~
   ✅ **已确认**：分层配置（中央定义默认值 + 节点本地覆盖）

## 错误处理与降级策略

### 降级场景

| 场景 | 影响 | 处理策略 |
|------|------|----------|
| Redis不可用 | 统计API无法读取缓存 | 降级到实时查询，记录告警 |
| 边缘节点未上报摄像头状态 | 无法获取真实摄像头状态 | 保持现有状态，启动超时检测 |
| 数据库不可用 | 所有操作失败 | 返回503，启用本地缓存 |
| 定时任务执行失败 | 缓存过期/录像未清理 | 下次执行重试，记录错误日志 |

### 降级实现示例

**统计API降级**：
```java
@GetMapping("/cameras/statistics/summary")
public ResponseEntity<CameraStatisticsDTO> getStatistics() {
    try {
        CameraStatisticsDTO cached = redisTemplate.opsForValue()
            .get("camera:statistics:summary");
        
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }
        
        // 缓存不存在，降级到实时查询
        log.warn("Statistics cache miss, falling back to real-time query");
        return ResponseEntity.ok(cameraService.calculateStatisticsRealTime());
        
    } catch (RedisConnectionException e) {
        // Redis不可用，降级
        log.error("Redis unavailable, using real-time query", e);
        return ResponseEntity.ok(cameraService.calculateStatisticsRealTime());
    }
}
```

**边缘节点状态同步降级**：
```java
@Scheduled(fixedRate = 60000)
public void detectCameraTimeout() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
    
    List<Camera> timedOut = cameraRepository
        .findByLastHeartbeatBeforeAndStatus(threshold, CameraStatus.ONLINE);
    
    for (Camera camera : timedOut) {
        camera.setStatus(CameraStatus.UNKNOWN);
        cameraRepository.save(camera);
    }
}
```
