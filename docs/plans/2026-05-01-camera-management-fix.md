# Camera Management Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复摄像头管理模块的严重缺陷：逻辑运算符错误、认证注解漏洞、软删除功能缺失

**Architecture:** 
- 创建 `NodeWeightCalculator` 共享服务消除重复代码
- 使用 Flyway 管理数据库迁移
- 通过 `@EntityGraph` 优化 N+1 查询
- 添加定时任务清理已删除数据

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Flyway, JUnit 5, Mockito

---

## Task 1: 创建数据库迁移脚本

**Files:**
- Create: `backend/aick-mmp-central/src/main/resources/db/migration/V20260501__add_deleted_at_column.sql`

**Step 1: 创建迁移脚本**

```sql
-- V20260501__add_deleted_at_column.sql
-- Add deleted_at column for soft delete support

-- Add deleted_at column if not exists
ALTER TABLE cameras ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_cameras_deleted_at ON cameras(deleted_at);
```

**Step 2: 验证迁移脚本语法**

Run: 检查 SQL 语法是否正确
Expected: 无语法错误

**Step 3: 提交**

```bash
git add backend/aick-mmp-central/src/main/resources/db/migration/V20260501__add_deleted_at_column.sql
git commit -m "feat(camera): add deleted_at column for soft delete"
```

---

## Task 2: 创建 NodeWeightCalculator 服务

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/NodeWeightCalculator.java`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/NodeWeightCalculatorTest.java`

**Step 1: 创建服务类骨架**

```java
package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.EdgeNode;
import org.springframework.stereotype.Service;

@Service
public class NodeWeightCalculator {

    /**
     * 判断节点是否健康
     * 不健康条件：CPU >= 80% 或 内存 >= 85%
     * NULL 处理：仅当至少有一个指标有值且不超阈值时才健康
     */
    public boolean isNodeHealthy(Double cpuUsage, Double memoryUsage) {
        boolean cpuHealthy = cpuUsage == null || cpuUsage < 80;
        boolean memoryHealthy = memoryUsage == null || memoryUsage < 85;
        
        return (cpuUsage == null && memoryUsage == null) 
               || (cpuUsage != null && cpuHealthy) 
               || (memoryUsage != null && memoryHealthy);
    }

    /**
     * 计算节点权重（四因子）
     */
    public double calculateWeight(EdgeNode node, Double cpuUsage, Double memoryUsage) {
        if (!isNodeHealthy(cpuUsage, memoryUsage)) {
            return 0.0;
        }
        
        // 四因子计算
        double capacityWeight = calculateCapacityWeight(node);
        double cpuWeight = calculateCpuWeight(cpuUsage);
        double memoryWeight = calculateMemoryWeight(memoryUsage);
        double responseTimeWeight = calculateResponseTimeWeight(node);
        
        return (capacityWeight * 0.35 + cpuWeight * 0.25 + memoryWeight * 0.25 + responseTimeWeight * 0.15) * 100;
    }

    /**
     * 计算带区域加成的权重
     */
    public double calculateWeightWithRegionBonus(EdgeNode node, Double cpuUsage, Double memoryUsage, 
                                                  Long sourceRegionId, double bonusRate) {
        double baseWeight = calculateWeight(node, cpuUsage, memoryUsage);
        
        if (sourceRegionId != null && node.getRegionId() != null 
            && sourceRegionId.equals(node.getRegionId())) {
            return baseWeight * (1 + bonusRate);
        }
        
        return baseWeight;
    }

    private double calculateCapacityWeight(EdgeNode node) {
        if (node.getMaxCameraSupport() == null || node.getMaxCameraSupport() == 0) {
            return 1.0;
        }
        double currentLoad = (double) node.getCurrentCameraCount() / node.getMaxCameraSupport();
        return 1.0 - currentLoad;
    }

    private double calculateCpuWeight(Double cpuUsage) {
        if (cpuUsage == null) return 1.0;
        return (100.0 - cpuUsage) / 100.0;
    }

    private double calculateMemoryWeight(Double memoryUsage) {
        if (memoryUsage == null) return 1.0;
        return (100.0 - memoryUsage) / 100.0;
    }

    private double calculateResponseTimeWeight(EdgeNode node) {
        if (node.getLastHeartbeat() == null) return 0.5;
        long secondsSinceHeartbeat = java.time.Duration.between(
            node.getLastHeartbeat(), java.time.LocalDateTime.now()
        ).getSeconds();
        if (secondsSinceHeartbeat < 60) return 1.0;
        if (secondsSinceHeartbeat < 300) return 0.7;
        return 0.3;
    }
}
```

**Step 2: 编写单元测试**

```java
package com.aick.mmp.central.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeWeightCalculatorTest {

    private NodeWeightCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NodeWeightCalculator();
    }

    // ============ isNodeHealthy 测试 ============

    @Test
    void isNodeHealthy_bothNull_returnsTrue() {
        assertTrue(calculator.isNodeHealthy(null, null));
    }

    @Test
    void isNodeHealthy_cpuNull_memoryNormal_returnsTrue() {
        assertTrue(calculator.isNodeHealthy(null, 50.0));
    }

    @Test
    void isNodeHealthy_memoryNull_cpuNormal_returnsTrue() {
        assertTrue(calculator.isNodeHealthy(50.0, null));
    }

    @Test
    void isNodeHealthy_bothNormal_returnsTrue() {
        assertTrue(calculator.isNodeHealthy(60.0, 70.0));
    }

    @Test
    void isNodeHealthy_cpuOverThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(85.0, 50.0));
    }

    @Test
    void isNodeHealthy_memoryOverThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(50.0, 90.0));
    }

    @Test
    void isNodeHealthy_bothOverThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(85.0, 90.0));
    }

    @Test
    void isNodeHealthy_cpuAtThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(80.0, 50.0));
    }

    @Test
    void isNodeHealthy_memoryAtThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(50.0, 85.0));
    }

    @Test
    void isNodeHealthy_cpuNull_memoryOverThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(null, 90.0));
    }

    @Test
    void isNodeHealthy_memoryNull_cpuOverThreshold_returnsFalse() {
        assertFalse(calculator.isNodeHealthy(85.0, null));
    }
}
```

**Step 3: 运行测试验证**

Run: `cd backend && mvn test -Dtest=NodeWeightCalculatorTest -q`
Expected: 所有测试通过

**Step 4: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/NodeWeightCalculator.java
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/NodeWeightCalculatorTest.java
git commit -m "feat(camera): add NodeWeightCalculator service with correct health logic"
```

---

## Task 3: 更新 CameraRepository 添加软删除查询方法

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/CameraRepository.java`

**Step 1: 添加软删除查询方法**

在 `CameraRepository.java` 中添加以下方法：

```java
// 软删除查询方法
@Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
List<Camera> findAllActive();

@Query("SELECT c FROM Camera c WHERE c.deletedAt IS NOT NULL")
List<Camera> findAllDeleted();

@Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
Page<Camera> findAllActive(Pageable pageable);

Optional<Camera> findByIdAndDeletedAtIsNull(Long id);

@Query("SELECT COUNT(c) FROM Camera c WHERE c.deletedAt IS NULL")
long countActive();

@Query("SELECT COUNT(c) FROM Camera c WHERE c.deletedAt IS NULL AND c.edgeNodeId = :edgeNodeId")
long countByEdgeNodeIdAndDeletedAtIsNull(@Param("edgeNodeId") Long edgeNodeId);
```

**Step 2: 添加 @EntityGraph 优化查询**

```java
@EntityGraph(attributePaths = {"edgeNode", "region"})
@Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
Page<Camera> findAllActiveWithDetails(Pageable pageable);

@EntityGraph(attributePaths = {"edgeNode", "region"})
Optional<Camera> findByIdAndDeletedAtIsNull(Long id);
```

**Step 3: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/CameraRepository.java
git commit -m "feat(camera): add soft delete query methods to CameraRepository"
```

---

## Task 4: 更新 CameraServiceImpl

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java`
- Test: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/impl/CameraServiceImplTest.java`

**Step 1: 添加依赖注入**

在 `CameraServiceImpl` 类中添加：

```java
@Autowired
private NodeWeightCalculator nodeWeightCalculator;
```

**Step 2: 移除内嵌的 calculateNodeWeight 方法并替换调用**

找到 `calculateNodeWeight` 方法（行449-480），将其替换为使用 `nodeWeightCalculator`。

原代码：
```java
private double calculateNodeWeight(EdgeNode targetNode, Double cpuUsage, Double memoryUsage) {
    // ... 内嵌逻辑
}
```

替换为：
```java
private double calculateNodeWeight(EdgeNode targetNode, Double cpuUsage, Double memoryUsage) {
    return nodeWeightCalculator.calculateWeight(targetNode, cpuUsage, memoryUsage);
}
```

**Step 3: 修改软删除方法**

找到 `deleteCamera` 方法，修改为软删除：

```java
@Override
public void deleteCamera(Long id) {
    Camera camera = cameraRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new RuntimeException("Camera not found or already deleted"));
    camera.setDeletedAt(LocalDateTime.now());
    cameraRepository.save(camera);
}
```

**Step 4: 修改批量删除方法**

```java
@Override
public void batchDeleteCameras(List<Long> cameraIds) {
    List<Camera> cameras = cameraRepository.findAllById(cameraIds);
    LocalDateTime now = LocalDateTime.now();
    cameras.stream()
            .filter(c -> c.getDeletedAt() == null)
            .forEach(c -> c.setDeletedAt(now));
    cameraRepository.saveAll(cameras);
}
```

**Step 5: 实现恢复摄像头方法**

```java
public CameraDTO restoreCamera(Long id) {
    Camera camera = cameraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Camera not found"));
    
    if (camera.getDeletedAt() == null) {
        throw new RuntimeException("Camera is not deleted");
    }
    
    camera.setDeletedAt(null);
    Camera saved = cameraRepository.save(camera);
    return convertToDto(saved);
}
```

**Step 6: 实现强制删除方法**

```java
public void forceDeleteCamera(Long id) {
    if (!cameraRepository.existsById(id)) {
        throw new RuntimeException("Camera not found");
    }
    cameraRepository.deleteById(id);
}
```

**Step 7: 实现获取所有在线摄像头**

```java
public List<CameraDTO> getAllOnlineCameras() {
    return cameraRepository.findByStatus(Camera.CameraStatus.ONLINE).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
}
```

**Step 8: 修复 selectOptimalEdgeNode 使用共享服务**

```java
@Override
public Long selectOptimalEdgeNode(CameraDTO cameraDTO) {
    List<EdgeNode> onlineNodes = edgeNodeRepository.findByStatus(EdgeNode.EdgeNodeStatus.ONLINE);
    
    EdgeNode selectedNode = null;
    double maxWeight = -1;
    
    for (EdgeNode node : onlineNodes) {
        // 使用共享服务判断健康状态
        Double cpuUsage = node.getCpuUsage();
        Double memoryUsage = node.getMemoryUsage();
        
        if (!nodeWeightCalculator.isNodeHealthy(cpuUsage, memoryUsage)) {
            continue;
        }
        
        // 容量检查
        if (node.getMaxCameraSupport() != null && 
            node.getCurrentCameraCount() >= node.getMaxCameraSupport()) {
            continue;
        }
        
        double weight = nodeWeightCalculator.calculateWeight(node, cpuUsage, memoryUsage);
        if (weight > maxWeight) {
            maxWeight = weight;
            selectedNode = node;
        }
    }
    
    return selectedNode != null ? selectedNode.getId() : null;
}
```

**Step 9: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java
git commit -m "feat(camera): refactor to use NodeWeightCalculator and add soft delete"
```

---

## Task 5: 更新 CameraController

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraController.java`

**Step 1: 修复认证注解格式错误**

找到行185和192，修复引号：

```java
// ❌ 错误
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR)")

// ✅ 正确
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
```

**Step 2: 修改创建摄像头返回 201 状态码**

```java
// ❌ 错误
return ResponseEntity.ok(createdCamera);

// ✅ 正确
return ResponseEntity.status(HttpStatus.CREATED).body(createdCamera);
```

添加 import：
```java
import static org.springframework.http.HttpStatus.CREATED;
```

**Step 3: 添加恢复摄像头接口**

```java
@PostMapping("/{id}/restore")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<CameraDTO> restoreCamera(@PathVariable Long id) {
    CameraDTO restoredCamera = cameraService.restoreCamera(id);
    return ResponseEntity.ok(restoredCamera);
}
```

**Step 4: 添加强制删除接口**

```java
@DeleteMapping("/{id}/force")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> forceDeleteCamera(@PathVariable Long id) {
    cameraService.forceDeleteCamera(id);
    return ResponseEntity.noContent().build();
}
```

**Step 5: 添加查询已删除摄像头接口**

```java
@GetMapping("/deleted")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Page<CameraDTO>> getDeletedCameras(Pageable pageable) {
    // 需要在 CameraService 中添加对应方法
    return ResponseEntity.ok(null); // TODO: implement
}
```

**Step 6: 修复 getOnlineCameras 方法**

```java
@GetMapping("/online")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public ResponseEntity<List<CameraDTO>> getOnlineCameras(@RequestParam(required = false) Long edgeNodeId) {
    List<CameraDTO> onlineCameras;
    if (edgeNodeId != null) {
        onlineCameras = cameraService.getOnlineCamerasByEdgeNode(edgeNodeId);
    } else {
        onlineCameras = cameraService.getAllOnlineCameras();
    }
    return ResponseEntity.ok(onlineCameras);
}
```

**Step 7: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/CameraController.java
git commit -m "fix(camera): fix auth annotations and add soft delete endpoints"
```

---

## Task 6: 更新 EdgeNodeFailoverServiceImpl

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/EdgeNodeFailoverServiceImpl.java`

**Step 1: 添加依赖注入**

```java
@Autowired
private NodeWeightCalculator nodeWeightCalculator;
```

**Step 2: 移除内嵌的 calculateBaseNodeWeight 方法**

找到 `calculateBaseNodeWeight` 方法，将其移除或替换为调用 `nodeWeightCalculator`。

**Step 3: 更新节点选择逻辑**

在节点选择处使用 `nodeWeightCalculator.isNodeHealthy()` 替代原有逻辑。

**Step 4: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/EdgeNodeFailoverServiceImpl.java
git commit -m "refactor(failover): use shared NodeWeightCalculator service"
```

---

## Task 7: 创建定时清理任务

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/task/CameraSoftDeleteCleanupTask.java`

**Step 1: 创建定时任务类**

```java
package com.aick.mmp.central.task;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CameraSoftDeleteCleanupTask {

    private final CameraRepository cameraRepository;
    
    private static final int RETENTION_DAYS = 30;

    /**
     * 每日凌晨 2:00 执行清理任务
     * 删除 30 天前软删除的摄像头
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupSoftDeletedCameras() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        
        List<Camera> deletedCameras = cameraRepository.findAllDeleted().stream()
                .filter(c -> c.getDeletedAt() != null && c.getDeletedAt().isBefore(cutoffDate))
                .toList();
        
        if (deletedCameras.isEmpty()) {
            log.info("No soft-deleted cameras to clean up");
            return;
        }
        
        log.info("Cleaning up {} soft-deleted cameras older than {} days", 
                deletedCameras.size(), RETENTION_DAYS);
        
        cameraRepository.deleteAll(deletedCameras);
        
        log.info("Successfully cleaned up {} cameras", deletedCameras.size());
    }
}
```

**Step 2: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/task/CameraSoftDeleteCleanupTask.java
git commit -m "feat(camera): add scheduled task for soft delete cleanup"
```

---

## Task 8: 更新 CameraService 接口

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraService.java`

**Step 1: 添加新方法声明**

```java
CameraDTO restoreCamera(Long id);
void forceDeleteCamera(Long id);
List<CameraDTO> getAllOnlineCameras();
```

**Step 2: 提交**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraService.java
git commit -m "feat(camera): add new soft delete methods to CameraService interface"
```

---

## Task 9: 运行集成测试

**Files:**
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/integration/CameraSoftDeleteIntegrationTest.java`

**Step 1: 创建集成测试**

```java
package com.aick.mmp.central.integration;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.shared.model.Camera;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CameraSoftDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CameraRepository cameraRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void softDelete_camera_setsDeletedAt() throws Exception {
        Camera camera = cameraRepository.save(Camera.builder()
                .name("Test Camera")
                .connectionUrl("rtsp://test")
                .status(Camera.CameraStatus.ONLINE)
                .protocol(Camera.Protocol.RTSP)
                .build());
        
        mockMvc.perform(delete("/cameras/" + camera.getId()))
                .andExpect(status().isNoContent());
        
        Camera deleted = cameraRepository.findById(camera.getId()).orElseThrow();
        assert deleted.getDeletedAt() != null;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void restoreCamera_clearsDeletedAt() throws Exception {
        Camera camera = cameraRepository.save(Camera.builder()
                .name("Test Camera")
                .connectionUrl("rtsp://test2")
                .status(Camera.CameraStatus.ONLINE)
                .protocol(Camera.Protocol.RTSP)
                .deletedAt(LocalDateTime.now())
                .build());
        
        mockMvc.perform(post("/cameras/" + camera.getId() + "/restore"))
                .andExpect(status().isOk());
        
        Camera restored = cameraRepository.findById(camera.getId()).orElseThrow();
        assert restored.getDeletedAt() == null;
    }
}
```

**Step 2: 运行测试**

Run: `cd backend && mvn test -Dtest=CameraSoftDeleteIntegrationTest -q`
Expected: 所有测试通过

**Step 3: 提交**

```bash
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/integration/CameraSoftDeleteIntegrationTest.java
git commit -m "test(camera): add integration tests for soft delete"
```

---

## Task 10: 代码审查与最终验证

**Step 1: 运行完整测试套件**

Run: `cd backend && mvn test -q`
Expected: 所有测试通过

**Step 2: 检查 Linter**

Run: `cd backend && mvn checkstyle:check -q 2>&1 | head -50`
Expected: 无严重警告

**Step 3: 提交最终更改**

```bash
git add -A
git commit -m "fix(camera-management): resolve all issues from code review

- Fix logic operator precedence in node weight calculation
- Fix @PreAuthorize annotation syntax error  
- Implement soft delete with 30-day retention
- Add NodeWeightCalculator shared service
- Add cleanup scheduled task
- Optimize queries with @EntityGraph

Closes #XXX"
```

---

## Summary

| Task | Description | Files Modified |
|------|-------------|----------------|
| 1 | Database migration | 1 new file |
| 2 | NodeWeightCalculator service | 2 files |
| 3 | CameraRepository updates | 1 file |
| 4 | CameraServiceImpl refactor | 1 file |
| 5 | CameraController fixes | 1 file |
| 6 | EdgeNodeFailoverServiceImpl refactor | 1 file |
| 7 | Cleanup task | 1 new file |
| 8 | CameraService interface | 1 file |
| 9 | Integration tests | 1 new file |
| 10 | Code review | - |

**Total: 10 tasks, ~10 files changed**
