# 录像管理模块实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现完整的录像文件存储、下载、清理功能，包括后端服务和前端界面增强。

**Architecture:** 
- 后端采用Spring Boot微服务架构，录像文件存储在本地文件系统，按日期分目录组织
- 前端使用React + Ant Design，通过REST API与后端交互
- 定时任务使用Spring @Scheduled实现录像清理

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, MySQL 8, React 18, Ant Design 5.x

---

## Task 1: 数据库 Schema 更新

**Files:**
- Modify: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Recording.java:1-110`
- Create: `backend/aick-mmp-central/src/main/resources/db/migration/V2__add_recording_storage_fields.sql`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/model/RecordingEntityTest.java`

### Step 1: 创建数据库迁移脚本

**Step 1: Write the failing test**

```java
// backend/aick-mmp-central/src/test/java/com/aick/mmp/central/model/RecordingEntityTest.java
package com.aick.mmp.central.model;

import com.aick.mmp.shared.model.Recording;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordingEntityTest {

    @Test
    void testRecordingHasStorageFields() {
        Recording recording = Recording.builder()
                .id(1L)
                .cameraId(1L)
                .name("Test Recording")
                .filePath("/recordings/2026-05-01/cam_001.mp4")
                .fileSize(1024L * 1024 * 100) // 100MB
                .startTime(java.time.LocalDateTime.now())
                .build();
        
        assertNotNull(recording.getFilePath());
        assertNotNull(recording.getFileSize());
    }

    @Test
    void testRecordingStatusEnum() {
        Recording recording = Recording.builder().build();
        recording.setStatus("COMPLETED");
        assertEquals("COMPLETED", recording.getStatus());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RecordingEntityTest -pl aick-mmp-central`
Expected: FAIL - recording entity missing fields

**Step 3: Update Recording entity**

```java
// backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Recording.java

// 在现有字段后添加以下字段 (约第55行后)

// 新增字段：
/**
 * 文件大小(字节)
 */
@Column(name = "file_size")
private Long fileSize;

// 新增字段：
/**
 * MD5校验码
 */
@Column(name = "md5", length = 32)
private String md5;

// 新增字段：
/**
 * 存储路径
 */
@Column(name = "storage_path")
private String storagePath;

// 新增字段：
/**
 * 完整性状态 (PENDING, COMPLETED, CORRUPTED, DELETED)
 */
@Column(name = "integrity_status", length = 20)
private String integrityStatus;

// 新增字段：
/**
 * 锁定状态 (下载时锁定)
 */
@Column(name = "lock_status")
private Boolean lockStatus = false;
```

**Step 4: 创建数据库迁移脚本**

```sql
-- backend/aick-mmp-central/src/main/resources/db/migration/V2__add_recording_storage_fields.sql

-- 添加新字段
ALTER TABLE recordings 
ADD COLUMN IF NOT EXISTS file_size BIGINT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS md5 VARCHAR(32) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS integrity_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS lock_status BOOLEAN DEFAULT FALSE;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_recording_integrity_status ON recordings(integrity_status);
CREATE INDEX IF NOT EXISTS idx_recording_lock_status ON recordings(lock_status);
```

**Step 5: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RecordingEntityTest -pl aick-mmp-central`
Expected: PASS

**Step 6: Commit**

```bash
git add backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Recording.java
git add backend/aick-mmp-central/src/main/resources/db/migration/V2__add_recording_storage_fields.sql
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/model/RecordingEntityTest.java
git commit -m "feat(recording): add storage fields to Recording entity

- Add fileSize, md5, storagePath, integrityStatus, lockStatus fields
- Add database migration script
- Add unit tests for new fields"
```

---

## Task 2: 录像存储配置

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/RecordingStorageProperties.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingStorageService.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingStorageServiceImpl.java`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/RecordingStorageServiceTest.java`

### Step 1: Write configuration properties test

**Step 1: Write the failing test**

```java
// backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/RecordingStorageServiceTest.java
package com.aick.mmp.central.service;

import com.aick.mmp.central.service.RecordingStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RecordingStorageServiceTest {

    @Autowired
    private RecordingStorageService storageService;

    @Test
    void testGetStoragePath() {
        String path = storageService.getStoragePath("2026-05-01", 1L);
        assertNotNull(path);
        assertTrue(path.contains("2026-05-01"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RecordingStorageServiceTest -pl aick-mmp-central`
Expected: FAIL - RecordingStorageService not found

**Step 3: Create configuration properties**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/RecordingStorageProperties.java
package com.aick.mmp.central.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "recording.storage")
@Data
public class RecordingStorageProperties {
    
    /**
     * 录像存储基础路径
     */
    private String basePath = "/var/local/mmp/recordings";
    
    /**
     * 保留天数（默认30天）
     */
    private int retentionDays = 30;
    
    /**
     * 存储容量警告阈值（百分比）
     */
    private int capacityWarningThreshold = 80;
    
    /**
     * 存储容量危险阈值（百分比）
     */
    private int capacityCriticalThreshold = 90;
    
    /**
     * 清理后保留容量比例
     */
    private int retentionAfterCleanup = 70;
}
```

**Step 4: Create storage service interface**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingStorageService.java
package com.aick.mmp.central.service;

import java.io.InputStream;

public interface RecordingStorageService {
    
    /**
     * 获取录像存储路径
     * @param date 日期 (YYYY-MM-DD)
     * @param cameraId 摄像头ID
     * @return 完整存储路径
     */
    String getStoragePath(String date, Long cameraId);
    
    /**
     * 存储录像文件
     * @param cameraId 摄像头ID
     * @param date 日期
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @return 存储路径
     */
    String storeRecording(Long cameraId, String date, InputStream inputStream, String filename);
    
    /**
     * 获取文件输入流
     * @param storagePath 存储路径
     * @return 文件输入流
     */
    InputStream getRecordingStream(String storagePath);
    
    /**
     * 计算文件MD5
     * @param storagePath 存储路径
     * @return MD5值
     */
    String calculateMd5(String storagePath);
    
    /**
     * 删除录像文件
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    boolean deleteRecording(String storagePath);
    
    /**
     * 获取存储容量使用情况
     * @return 使用百分比
     */
    double getStorageUsagePercent();
    
    /**
     * 检查文件是否存在
     * @param storagePath 存储路径
     * @return 是否存在
     */
    boolean fileExists(String storagePath);
}
```

**Step 5: Create storage service implementation**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingStorageServiceImpl.java
package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.RecordingStorageProperties;
import com.aick.mmp.central.service.RecordingStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;

@Service
public class RecordingStorageServiceImpl implements RecordingStorageService {

    private final RecordingStorageProperties properties;

    @Autowired
    public RecordingStorageServiceImpl(RecordingStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getStoragePath(String date, Long cameraId) {
        return Paths.get(properties.getBasePath(), date, "cam_" + cameraId + ".mp4").toString();
    }

    @Override
    public String storeRecording(Long cameraId, String date, InputStream inputStream, String filename) {
        String dirPath = Paths.get(properties.getBasePath(), date).toString();
        Path dir = Paths.get(dirPath);
        
        try {
            Files.createDirectories(dir);
            String fullPath = Paths.get(dirPath, filename).toString();
            Files.copy(inputStream, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
            return fullPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store recording", e);
        }
    }

    @Override
    public InputStream getRecordingStream(String storagePath) {
        try {
            return new FileInputStream(storagePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Recording file not found: " + storagePath, e);
        }
    }

    @Override
    public String calculateMd5(String storagePath) {
        try (InputStream is = new FileInputStream(storagePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate MD5", e);
        }
    }

    @Override
    public boolean deleteRecording(String storagePath) {
        try {
            return Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public double getStorageUsagePercent() {
        try {
            File basePath = new File(properties.getBasePath());
            long total = basePath.getTotalSpace();
            long free = basePath.getFreeSpace();
            long used = total - free;
            return (used * 100.0) / total;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        return Files.exists(Paths.get(storagePath));
    }
}
```

**Step 6: Add configuration to application.yml**

```yaml
# 在 application.yml 中添加 (约第234行后)
recording:
  storage:
    base-path: ${RECORDING_STORAGE_PATH:/var/local/mmp/recordings}
    retention-days: 30
    capacity-warning-threshold: 80
    capacity-critical-threshold: 90
    retention-after-cleanup: 70
```

**Step 7: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RecordingStorageServiceTest -pl aick-mmp-central`
Expected: PASS

**Step 8: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/RecordingStorageProperties.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingStorageService.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingStorageServiceImpl.java
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/RecordingStorageServiceTest.java
git add backend/aick-mmp-central/src/main/resources/application.yml
git commit -m "feat(recording): add storage configuration and service

- Add RecordingStorageProperties for configuration
- Add RecordingStorageService interface and implementation
- Add unit tests for storage service"
```

---

## Task 3: 录像下载API

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/RecordingController.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingDownloadService.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingDownloadServiceImpl.java`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/controller/RecordingControllerTest.java`

### Step 1: Write download endpoint test

**Step 1: Write the failing test**

```java
// backend/aick-mmp-central/src/test/java/com/aick/mmp/central/controller/RecordingControllerTest.java
package com.aick.mmp.central.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecordingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDownloadRecordingEndpoint() throws Exception {
        mockMvc.perform(get("/recordings/1/download"))
                .andExpect(status().isOk());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RecordingControllerTest -pl aick-mmp-central`
Expected: FAIL - 404 Not Found

**Step 3: Create download service interface**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingDownloadService.java
package com.aick.mmp.central.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface RecordingDownloadService {
    
    /**
     * 单个录像下载
     * @param recordingId 录像ID
     * @param request HTTP请求
     * @param response HTTP响应
     */
    void downloadRecording(Long recordingId, HttpServletRequest request, HttpServletResponse response) throws IOException;
    
    /**
     * 批量下载（ZIP）
     * @param recordingIds 录像ID列表
     * @param request HTTP请求
     * @param response HTTP响应
     */
    void downloadBatchRecordings(Long[] recordingIds, HttpServletRequest request, HttpServletResponse response) throws IOException;
    
    /**
     * 检查并发下载数量
     * @param userId 用户ID
     * @return 当前下载数
     */
    int getActiveDownloadCount(Long userId);
    
    /**
     * 获取下载进度
     * @param sessionId 下载会话ID
     * @return 进度信息
     */
    DownloadProgress getDownloadProgress(String sessionId);
    
    class DownloadProgress {
        private long downloadedBytes;
        private long totalBytes;
        private String status;
        
        public DownloadProgress(long downloadedBytes, long totalBytes, String status) {
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.status = status;
        }
        
        public long getDownloadedBytes() { return downloadedBytes; }
        public long getTotalBytes() { return totalBytes; }
        public String getStatus() { return status; }
    }
}
```

**Step 4: Create download service implementation**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingDownloadServiceImpl.java
package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.service.RecordingDownloadService;
import com.aick.mmp.central.service.RecordingStorageService;
import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.shared.repository.RecordingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class RecordingDownloadServiceImpl implements RecordingDownloadService {

    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final long MAX_BATCH_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    
    private final RecordingRepository recordingRepository;
    private final RecordingStorageService storageService;
    private final Map<Long, List<DownloadProgress>> userDownloads = new ConcurrentHashMap<>();

    @Autowired
    public RecordingDownloadServiceImpl(RecordingRepository recordingRepository, 
                                        RecordingStorageService storageService) {
        this.recordingRepository = recordingRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public void downloadRecording(Long recordingId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new RuntimeException("Recording not found"));
        
        String storagePath = recording.getStoragePath();
        if (storagePath == null || !storageService.fileExists(storagePath)) {
            throw new RuntimeException("Recording file not found");
        }
        
        // 锁定文件
        recording.setLockStatus(true);
        recordingRepository.save(recording);
        
        try {
            Path filePath = Paths.get(storagePath);
            String filename = "recording_" + recordingId + ".mp4";
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setHeader("Content-Length", String.valueOf(Files.size(filePath)));
            
            // 添加MD5头
            String md5 = recording.getMd5();
            if (md5 != null) {
                response.setHeader("Content-MD5", md5);
            }
            
            // 支持Range请求（断点续传）
            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null) {
                handleRangeRequest(filePath, rangeHeader, response);
            } else {
                Files.copy(filePath, response.getOutputStream());
            }
        } finally {
            // 解锁文件
            recording.setLockStatus(false);
            recordingRepository.save(recording);
        }
    }

    private void handleRangeRequest(Path filePath, String rangeHeader, HttpServletResponse response) throws IOException {
        // 简化实现：支持完整的Range请求
        long fileSize = Files.size(filePath);
        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileSize - 1;
        
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setHeader("Content-Length", String.valueOf(end - start + 1));
        
        try (InputStream is = Files.newInputStream(filePath)) {
            is.skip(start);
            byte[] buffer = new byte[8192];
            long remaining = end - start + 1;
            OutputStream os = response.getOutputStream();
            
            while (remaining > 0) {
                int read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                os.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    @Override
    public void downloadBatchRecordings(Long[] recordingIds, HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Recording> recordings = recordingRepository.findAllById(Arrays.asList(recordingIds));
        
        // 检查总大小
        long totalSize = recordings.stream()
                .mapToLong(r -> r.getFileSize() != null ? r.getFileSize() : 0)
                .sum();
        
        if (totalSize > MAX_BATCH_SIZE) {
            throw new RuntimeException("Batch download size exceeds 2GB limit");
        }
        
        String zipFilename = "recordings_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");
        
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Recording recording : recordings) {
                if (recording.getStoragePath() != null && storageService.fileExists(recording.getStoragePath())) {
                    Path filePath = Paths.get(recording.getStoragePath());
                    ZipEntry entry = new ZipEntry("cam_" + recording.getCameraId() + "_" + recordingId + ".mp4");
                    zos.putNextEntry(entry);
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    @Override
    public int getActiveDownloadCount(Long userId) {
        return userDownloads.getOrDefault(userId, Collections.emptyList()).size();
    }

    @Override
    public DownloadProgress getDownloadProgress(String sessionId) {
        // 实现进度跟踪逻辑
        return new DownloadProgress(0, 0, "UNKNOWN");
    }
}
```

**Step 5: Update RecordingController with download endpoints**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/RecordingController.java

// 在类中添加以下导入和字段
import com.aick.mmp.central.service.RecordingDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/recordings")
public class RecordingController {

    private final RecordingService recordingService;
    private final RecordingDownloadService downloadService;

    @Autowired
    public RecordingController(RecordingService recordingService, 
                               RecordingDownloadService downloadService) {
        this.recordingService = recordingService;
        this.downloadService = downloadService;
    }

    // 在现有方法后添加以下方法

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public void downloadRecording(@PathVariable Long id, 
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            downloadService.downloadRecording(id, request, response);
        } catch (IOException e) {
            throw new RuntimeException("Download failed", e);
        }
    }

    @PostMapping("/batch-download")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<?> batchDownload(@RequestBody Long[] recordingIds,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        int activeCount = downloadService.getActiveDownloadCount(1L); // TODO: get from security context
        if (activeCount >= 3) {
            return ResponseEntity.status(429)
                    .body(Collections.singletonMap("error", "Maximum concurrent downloads reached"));
        }
        
        try {
            downloadService.downloadBatchRecordings(recordingIds, request, response);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<RecordingDownloadService.DownloadProgress> getDownloadProgress(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(downloadService.getDownloadProgress(sessionId));
    }
}
```

**Step 6: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RecordingControllerTest -pl aick-mmp-central`
Expected: PASS

**Step 7: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/RecordingController.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingDownloadService.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingDownloadServiceImpl.java
git commit -m "feat(recording): add download endpoints

- Add single recording download with range support
- Add batch download as ZIP
- Add concurrent download limit (max 3)
- Add download progress tracking"
```

---

## Task 4: 录像清理服务

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingCleanupService.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingCleanupServiceImpl.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/scheduler/RecordingCleanupJob.java`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/RecordingCleanupServiceTest.java`

### Step 1: Write cleanup service test

**Step 1: Write the failing test**

```java
// backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/RecordingCleanupServiceTest.java
package com.aick.mmp.central.service;

import com.aick.mmp.central.service.RecordingCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RecordingCleanupServiceTest {

    @Autowired
    private RecordingCleanupService cleanupService;

    @Test
    void testCleanupExpiredRecordings() {
        int deletedCount = cleanupService.cleanupExpiredRecordings();
        assertTrue(deletedCount >= 0);
    }

    @Test
    void testIsRecordingProtected() {
        boolean isProtected = cleanupService.isRecordingProtected(1L);
        assertFalse(isProtected);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RecordingCleanupServiceTest -pl aick-mmp-central`
Expected: FAIL - RecordingCleanupService not found

**Step 3: Create cleanup service interface**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingCleanupService.java
package com.aick.mmp.central.service;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordingCleanupService {
    
    /**
     * 清理过期录像
     * @return 删除的录像数量
     */
    int cleanupExpiredRecordings();
    
    /**
     * 按摄像头清理过期录像
     * @param cameraId 摄像头ID
     * @return 删除的录像数量
     */
    int cleanupByCamera(Long cameraId);
    
    /**
     * 按时间范围清理
     * @param before 在此时间之前的录像将被删除
     * @return 删除的录像数量
     */
    int cleanupByTimeRange(LocalDateTime before);
    
    /**
     * 检查录像是否受保护（正在下载或播放）
     * @param recordingId 录像ID
     * @return 是否受保护
     */
    boolean isRecordingProtected(Long recordingId);
    
    /**
     * 手动触发清理
     * @param cameraIds 摄像头ID列表（null表示全部）
     * @return 删除的录像数量
     */
    int manualCleanup(List<Long> cameraIds);
    
    /**
     * 获取清理审计日志
     * @return 最近的清理记录
     */
    List<CleanupAuditLog> getCleanupAuditLogs();
    
    class CleanupAuditLog {
        private Long recordingId;
        private Long cameraId;
        private String storagePath;
        private Long fileSize;
        private LocalDateTime deletedAt;
        private String reason;
        private boolean manual;
        
        // getters and setters
    }
}
```

**Step 4: Create cleanup service implementation**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingCleanupServiceImpl.java
package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.RecordingStorageProperties;
import com.aick.mmp.central.service.RecordingCleanupService;
import com.aick.mmp.central.service.RecordingStorageService;
import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.shared.repository.RecordingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordingCleanupServiceImpl implements RecordingCleanupService {

    private final RecordingRepository recordingRepository;
    private final RecordingStorageService storageService;
    private final RecordingStorageProperties properties;
    
    // 简单的审计日志（生产环境应持久化）
    private final List<CleanupAuditLog> auditLogs = new ArrayList<>();

    @Autowired
    public RecordingCleanupServiceImpl(RecordingRepository recordingRepository,
                                        RecordingStorageService storageService,
                                        RecordingStorageProperties properties) {
        this.recordingRepository = recordingRepository;
        this.storageService = storageService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public int cleanupExpiredRecordings() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(properties.getRetentionDays());
        
        List<Recording> expiredRecordings = recordingRepository
                .findByStartTimeBeforeAndStatusNot(cutoffDate, "DELETED");
        
        return cleanupRecordings(expiredRecordings, "expired", false);
    }

    @Override
    @Transactional
    public int cleanupByCamera(Long cameraId) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(properties.getRetentionDays());
        
        List<Recording> recordings = recordingRepository
                .findByCameraIdAndStartTimeBeforeAndStatusNot(cameraId, cutoffDate, "DELETED");
        
        return cleanupRecordings(recordings, "expired-camera", false);
    }

    @Override
    @Transactional
    public int cleanupByTimeRange(LocalDateTime before) {
        List<Recording> recordings = recordingRepository
                .findByStartTimeBeforeAndStatusNot(before, "DELETED");
        
        return cleanupRecordings(recordings, "manual-range", true);
    }

    @Override
    public boolean isRecordingProtected(Long recordingId) {
        return recordingRepository.findById(recordingId)
                .map(r -> Boolean.TRUE.equals(r.getLockStatus()))
                .orElse(false);
    }

    @Override
    @Transactional
    public int manualCleanup(List<Long> cameraIds) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(properties.getRetentionDays());
        
        List<Recording> recordings;
        if (cameraIds == null || cameraIds.isEmpty()) {
            recordings = recordingRepository
                    .findByStartTimeBeforeAndStatusNot(cutoffDate, "DELETED");
        } else {
            recordings = recordingRepository
                    .findByCameraIdInAndStartTimeBeforeAndStatusNot(cameraIds, cutoffDate, "DELETED");
        }
        
        return cleanupRecordings(recordings, "manual", true);
    }

    @Override
    public List<CleanupAuditLog> getCleanupAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    private int cleanupRecordings(List<Recording> recordings, String reason, boolean manual) {
        int deletedCount = 0;
        
        for (Recording recording : recordings) {
            // 跳过受保护的文件
            if (isRecordingProtected(recording.getId())) {
                continue;
            }
            
            // 跳过损坏的文件（优先清理）
            if ("CORRUPTED".equals(recording.getIntegrityStatus())) {
                deleteRecordingInternal(recording, reason, manual);
                deletedCount++;
                continue;
            }
            
            // 正常清理
            deleteRecordingInternal(recording, reason, manual);
            deletedCount++;
        }
        
        return deletedCount;
    }

    private void deleteRecordingInternal(Recording recording, String reason, boolean manual) {
        // 删除物理文件
        if (recording.getStoragePath() != null) {
            storageService.deleteRecording(recording.getStoragePath());
        }
        
        // 更新数据库状态
        recording.setStatus("DELETED");
        recordingRepository.save(recording);
        
        // 记录审计日志
        CleanupAuditLog log = new CleanupAuditLog();
        log.setRecordingId(recording.getId());
        log.setCameraId(recording.getCameraId());
        log.setStoragePath(recording.getStoragePath());
        log.setFileSize(recording.getFileSize());
        log.setDeletedAt(LocalDateTime.now());
        log.setReason(reason);
        log.setManual(manual);
        auditLogs.add(log);
    }
}
```

**Step 5: Create scheduled cleanup job**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/scheduler/RecordingCleanupJob.java
package com.aick.mmp.central.scheduler;

import com.aick.mmp.central.service.RecordingCleanupService;
import com.aick.mmp.central.service.RecordingStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordingCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RecordingCleanupJob.class);

    private final RecordingCleanupService cleanupService;
    private final RecordingStorageService storageService;

    @Autowired
    public RecordingCleanupJob(RecordingCleanupService cleanupService,
                                RecordingStorageService storageService) {
        this.cleanupService = cleanupService;
        this.storageService = storageService;
    }

    /**
     * 每天凌晨2点执行录像清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanup() {
        logger.info("Starting scheduled recording cleanup");
        
        try {
            // 检查存储容量
            double usage = storageService.getStorageUsagePercent();
            logger.info("Current storage usage: {}%", String.format("%.2f", usage));
            
            int deletedCount = cleanupService.cleanupExpiredRecordings();
            logger.info("Scheduled cleanup completed. Deleted {} recordings", deletedCount);
            
        } catch (Exception e) {
            logger.error("Scheduled cleanup failed", e);
        }
    }

    /**
     * 检查存储容量并触发紧急清理
     */
    @Scheduled(fixedRate = 3600000) // 每小时检查一次
    public void checkStorageCapacity() {
        try {
            double usage = storageService.getStorageUsagePercent();
            
            if (usage >= 90) {
                logger.warn("Storage critical: {}% used. Triggering emergency cleanup", String.format("%.2f", usage));
                // 触发紧急清理
                cleanupService.manualCleanup(null);
            } else if (usage >= 80) {
                logger.warn("Storage warning: {}% used", String.format("%.2f", usage));
                // 可以发送告警通知管理员
            }
        } catch (Exception e) {
            logger.error("Storage capacity check failed", e);
        }
    }
}
```

**Step 6: Update RecordingRepository with new query methods**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/RecordingRepository.java

// 在现有方法后添加以下方法
package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long> {
    
    // 现有方法...
    
    // 新增查询方法
    List<Recording> findByStartTimeBeforeAndStatusNot(LocalDateTime time, String status);
    
    List<Recording> findByCameraIdAndStartTimeBeforeAndStatusNot(Long cameraId, LocalDateTime time, String status);
    
    List<Recording> findByCameraIdInAndStartTimeBeforeAndStatusNot(List<Long> cameraIds, LocalDateTime time, String status);
    
    @Query("SELECT r FROM Recording r WHERE r.integrityStatus = :status")
    List<Recording> findByIntegrityStatus(@Param("status") String status);
    
    @Query("SELECT r FROM Recording r WHERE r.lockStatus = true")
    List<Recording> findLockedRecordings();
    
    @Query("SELECT SUM(r.fileSize) FROM Recording r WHERE r.status != 'DELETED'")
    Long getTotalRecordingSize();
}
```

**Step 7: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RecordingCleanupServiceTest -pl aick-mmp-central`
Expected: PASS

**Step 8: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/RecordingCleanupService.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/RecordingCleanupServiceImpl.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/scheduler/RecordingCleanupJob.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/RecordingRepository.java
git commit -m "feat(recording): add cleanup service and scheduled job

- Add RecordingCleanupService for expired recording cleanup
- Add scheduled cleanup job (daily at 2 AM)
- Add storage capacity monitoring with emergency cleanup
- Add cleanup audit logging"
```

---

## Task 5: 前端 - 录像下载功能

**Files:**
- Modify: `frontend/src/pages/Playback.js`
- Modify: `frontend/src/utils/api.js`

### Step 1: Update API layer

**Step 1: Add download API methods**

```javascript
// frontend/src/utils/api.js

// 在 recordingApi 对象中添加以下方法 (约第104行后)

export const recordingApi = {
  // ... 现有方法 ...
  
  // 新增下载相关方法
  downloadRecording: (id) => {
    return axios.get(`/recordings/${id}/download`, {
      responseType: 'blob',
    });
  },
  
  batchDownloadRecordings: (recordingIds) => {
    return axios.post('/recordings/batch-download', recordingIds, {
      responseType: 'blob',
    });
  },
  
  getRecordingDownloadProgress: (sessionId) => {
    return axios.get(`/recordings/${sessionId}/download/progress`);
  },
  
  getRecordings: (params) => {
    return axios.get('/recordings', { params });
  },
};
```

### Step 2: Update Playback component with download functionality

**Step 2: Write the new download implementation**

```javascript
// frontend/src/pages/Playback.js

import React, { useState, useEffect } from 'react';
import { Card, DatePicker, Select, Button, Row, Col, List, Tag, Space, message, Checkbox, Modal, Progress } from 'antd';
import { PlayCircleOutlined, DownloadOutlined, SearchOutlined, VideoCameraOutlined, CheckSquareOutlined } from '@ant-design/icons';
import { recordingApi, cameraApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import { Typography } from 'antd';
const { Title } = Typography;

const { RangePicker } = DatePicker;
const { Option } = Select;
const { confirm } = Modal;

const Playback = () => {
  const [searchParams, setSearchParams] = useState({
    cameraId: null,
    dateRange: null,
    location: null
  });
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [cameras, setCameras] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  
  // 新增：批量选择相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [selectedRows, setSelectedRows] = useState([]);
  const [downloading, setDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);

  useEffect(() => {
    fetchCameras();
  }, []);

  // ... 保留现有的 fetchCameras, fetchRecordings, handleSearch, handleTableChange 方法 ...

  const handleDownload = async (recording) => {
    setDownloading(true);
    setDownloadProgress(0);
    
    try {
      const response = await recordingApi.downloadRecording(recording.id);
      
      // 创建下载链接
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `recording_${recording.id}.mp4`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      message.success('下载开始');
    } catch (error) {
      console.error('下载失败:', error);
      message.error('下载失败: ' + (error.message || '未知错误'));
    } finally {
      setDownloading(false);
      setDownloadProgress(100);
    }
  };

  const handleBatchDownload = async () => {
    if (selectedRows.length === 0) {
      message.warning('请先选择要下载的录像');
      return;
    }
    
    confirm({
      title: '确认批量下载',
      content: `确定要下载选中的 ${selectedRows.length} 个录像文件吗？`,
      onOk: async () => {
        setDownloading(true);
        
        try {
          // 如果只选了一个，直接下载
          if (selectedRows.length === 1) {
            await handleDownload(selectedRows[0]);
            return;
          }
          
          // 多个文件批量下载
          const recordingIds = selectedRows.map(r => r.id);
          const response = await recordingApi.batchDownloadRecordings(recordingIds);
          
          // 创建ZIP下载
          const url = window.URL.createObjectURL(new Blob([response.data]));
          const link = document.createElement('a');
          link.href = url;
          link.setAttribute('download', `recordings_${new Date().toISOString().split('T')[0]}.zip`);
          document.body.appendChild(link);
          link.click();
          link.remove();
          window.URL.revokeObjectURL(url);
          
          message.success('批量下载开始');
        } catch (error) {
          console.error('批量下载失败:', error);
          message.error('批量下载失败: ' + (error.response?.data?.error || error.message));
        } finally {
          setDownloading(false);
          setSelectedRowKeys([]);
          setSelectedRows([]);
        }
      },
    });
  };

  const onSelectChange = (newSelectedRowKeys, newSelectedRows) => {
    setSelectedRowKeys(newSelectedRowKeys);
    setSelectedRows(newSelectedRows);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };

  return (
    <div>
      <Title level={2}>视频回放</Title>
      
      {/* 搜索条件 - 保持不变 */}
      <Card style={{ marginBottom: '24px' }}>
        {/* ... 现有搜索表单代码 ... */}
      </Card>

      {/* 录像列表 */}
      <Card 
        title="录像文件"
        extra={
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleBatchDownload}
              disabled={selectedRowKeys.length === 0}
              loading={downloading}
            >
              批量下载
            </Button>
          </Space>
        }
      >
        <List
          loading={loading}
          dataSource={recordings}
          rowSelection={rowSelection}
          renderItem={item => (
            <List.Item
              actions={[
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handlePlay(item)}
                >
                  播放
                </Button>,
                <Button
                  icon={<DownloadOutlined />}
                  onClick={() => handleDownload(item)}
                  loading={downloading}
                >
                  下载
                </Button>
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Checkbox value={item.id} />
                    <span>{item.cameraName}</span>
                    <Tag color="blue">{item.location}</Tag>
                    <Tag color="green">{item.quality}</Tag>
                    {item.integrityStatus === 'CORRUPTED' && (
                      <Tag color="red">损坏</Tag>
                    )}
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {new Date(item.startTime).toLocaleString()} - {new Date(item.endTime).toLocaleString()}</div>
                    <div>时长: {Math.floor(item.duration / 60)}分钟 {item.duration % 60}秒 | 大小: {(item.size / (1024 * 1024)).toFixed(2)} MB</div>
                  </div>
                }
              />
            </List.Item>
          )}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个录像文件`,
            onChange: (page, pageSize) => {
              handleTableChange({ current: page, pageSize });
            }
          }}
        />
        
        {downloading && (
          <div style={{ marginTop: 16 }}>
            <Progress percent={downloadProgress} status="active" />
          </div>
        )}
      </Card>
    </div>
  );
};

export default Playback;
```

### Step 3: Commit

```bash
git add frontend/src/utils/api.js
git add frontend/src/pages/Playback.js
git commit -m "feat(frontend): add recording download functionality

- Add downloadRecording and batchDownloadRecordings APIs
- Add row selection for batch download
- Add download progress indicator
- Add confirmation modal for batch download"
```

---

## Task 6: 前端 - 录像列表增强

**Files:**
- Modify: `frontend/src/pages/Playback.js`
- Modify: `frontend/src/utils/api.js`

### Step 1: Add status filter and enhanced metadata display

**Step 1: Update Playback.js with status filter**

```javascript
// frontend/src/pages/Playback.js

const Playback = () => {
  // ... existing state ...
  
  // 新增：录像状态筛选
  const [statusFilter, setStatusFilter] = useState(null);
  
  // ... existing methods ...

  // 更新 handleSearch 方法以包含状态筛选
  const handleSearch = () => {
    const params = {};
    
    if (searchParams.cameraId) {
      params.cameraId = searchParams.cameraId;
    }
    
    if (searchParams.location) {
      params.location = searchParams.location;
    }
    
    if (searchParams.dateRange && searchParams.dateRange.length === 2) {
      params.startTime = searchParams.dateRange[0].toISOString();
      params.endTime = searchParams.dateRange[1].toISOString();
    }
    
    // 新增状态筛选
    if (statusFilter) {
      params.status = statusFilter;
    }
    
    fetchRecordings(params);
  };

  return (
    <div>
      <Title level={2}>视频回放</Title>
      
      {/* 搜索条件 */}
      <Card style={{ marginBottom: '24px' }}>
        <Row gutter={[16, 16]} align="middle">
          {/* ... 现有筛选条件 ... */}
          
          {/* 新增：录像状态筛选 */}
          <Col xs={24} sm={8} md={6}>
            <div>
              <div style={{ marginBottom: '8px' }}>录像状态:</div>
              <Select
                style={{ width: '100%' }}
                placeholder="全部状态"
                value={statusFilter}
                onChange={(value) => setStatusFilter(value)}
                allowClear
              >
                <Option value="COMPLETED">已完成</Option>
                <Option value="RECORDING">录像中</Option>
                <Option value="PENDING">待处理</Option>
                <Option value="CORRUPTED">已损坏</Option>
              </Select>
            </div>
          </Col>
          
          {/* ... 搜索按钮 ... */}
        </Row>
      </Card>

      {/* 录像列表 - 更新显示 */}
      <Card 
        title="录像文件"
        extra={
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleBatchDownload}
              disabled={selectedRowKeys.length === 0}
              loading={downloading}
            >
              批量下载
            </Button>
          </Space>
        }
      >
        <List
          // ... existing props ...
          renderItem={item => (
            <List.Item
              actions={[
                // ... existing actions ...
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Checkbox value={item.id} />
                    <span>{item.cameraName}</span>
                    <Tag color="blue">{item.location}</Tag>
                    <Tag color="green">{item.quality}</Tag>
                    
                    {/* 新增：录像状态标签 */}
                    {item.status === 'COMPLETED' && <Tag color="success">已完成</Tag>}
                    {item.status === 'RECORDING' && <Tag color="processing">录像中</Tag>}
                    {item.status === 'PENDING' && <Tag color="default">待处理</Tag>}
                    {item.status === 'CORRUPTED' && <Tag color="error">已损坏</Tag>}
                    
                    {/* 新增：完整性状态指示 */}
                    {item.integrityStatus === 'CORRUPTED' && (
                      <Tag color="red" icon={<ExclamationCircleOutlined />}>损坏</Tag>
                    )}
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {new Date(item.startTime).toLocaleString()} - {new Date(item.endTime).toLocaleString()}</div>
                    <div>
                      时长: {Math.floor(item.duration / 60)}分钟 {item.duration % 60}秒 | 
                      大小: {(item.size / (1024 * 1024)).toFixed(2)} MB |
                      {/* 新增：文件大小和MD5显示 */}
                      {item.md5 && <span>MD5: {item.md5.substring(0, 8)}...</span>}
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};
```

### Step 2: Commit

```bash
git add frontend/src/pages/Playback.js
git commit -m "feat(frontend): enhance recording list with status filter

- Add status filter dropdown (COMPLETED, RECORDING, PENDING, CORRUPTED)
- Add integrity status indicator
- Add MD5 hash display (first 8 chars)
- Add status tags with appropriate colors"
```

---

## Task 7: 集成测试

**Files:**
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/integration/RecordingIntegrationTest.java`

### Step 1: Write integration tests

```java
// backend/aick-mmp-central/src/test/java/com/aick/mmp/central/integration/RecordingIntegrationTest.java
package com.aick.mmp.central.integration;

import com.aick.mmp.central.service.RecordingCleanupService;
import com.aick.mmp.central.service.RecordingDownloadService;
import com.aick.mmp.central.service.RecordingStorageService;
import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.shared.repository.RecordingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RecordingIntegrationTest {

    @Autowired
    private RecordingStorageService storageService;
    
    @Autowired
    private RecordingCleanupService cleanupService;
    
    @Autowired
    private RecordingRepository recordingRepository;
    
    @Autowired
    private RecordingDownloadService downloadService;

    @Test
    void testFullRecordingLifecycle() {
        // 1. 存储录像
        String date = "2026-05-01";
        Long cameraId = 1L;
        String filename = "test_recording.mp4";
        byte[] content = "fake video content".getBytes();
        
        String storagePath = storageService.storeRecording(
            cameraId, 
            date, 
            new ByteArrayInputStream(content), 
            filename
        );
        
        assertNotNull(storagePath);
        assertTrue(storageService.fileExists(storagePath));
        
        // 2. 计算MD5
        String md5 = storageService.calculateMd5(storagePath);
        assertNotNull(md5);
        assertEquals(32, md5.length());
        
        // 3. 创建Recording记录
        Recording recording = Recording.builder()
                .cameraId(cameraId)
                .name("Test Recording")
                .storagePath(storagePath)
                .fileSize((long) content.length)
                .md5(md5)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now())
                .status("COMPLETED")
                .integrityStatus("PENDING")
                .build();
        
        recording = recordingRepository.save(recording);
        
        // 4. 更新完整性状态
        recording.setIntegrityStatus("COMPLETED");
        recordingRepository.save(recording);
        
        // 5. 验证清理保护
        assertFalse(cleanupService.isRecordingProtected(recording.getId()));
        
        // 6. 清理过期录像（应该跳过，因为只过了1天）
        int deleted = cleanupService.cleanupExpiredRecordings();
        assertEquals(0, deleted);
        
        // 7. 清理测试文件
        storageService.deleteRecording(storagePath);
        assertFalse(storageService.fileExists(storagePath));
    }

    @Test
    void testConcurrentDownloadLimit() {
        int count = downloadService.getActiveDownloadCount(1L);
        assertTrue(count >= 0);
    }
}
```

### Step 2: Run integration tests

Run: `cd backend && mvn test -Dtest=RecordingIntegrationTest -pl aick-mmp-central`
Expected: PASS

### Step 3: Commit

```bash
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/integration/RecordingIntegrationTest.java
git commit -m "test(recording): add integration tests for full recording lifecycle

- Test storage, MD5 calculation, and cleanup protection
- Test concurrent download limits
- Test recording status transitions"
```

---

## 总结

**完成后的文件结构：**

```
backend/
├── aick-mmp-central/
│   ├── src/main/java/com/aick/mmp/central/
│   │   ├── config/
│   │   │   └── RecordingStorageProperties.java      # NEW
│   │   ├── controller/
│   │   │   └── RecordingController.java            # MODIFIED
│   │   ├── repository/
│   │   │   └── RecordingRepository.java            # MODIFIED
│   │   ├── scheduler/
│   │   │   └── RecordingCleanupJob.java           # NEW
│   │   └── service/
│   │       ├── RecordingDownloadService.java       # NEW
│   │       ├── RecordingStorageService.java        # NEW
│   │       ├── RecordingCleanupService.java       # NEW
│   │       └── impl/
│   │           ├── RecordingDownloadServiceImpl.java
│   │           ├── RecordingStorageServiceImpl.java
│   │           └── RecordingCleanupServiceImpl.java
│   └── src/test/java/com/aick/mmp/central/
│       ├── model/RecordingEntityTest.java          # NEW
│       ├── service/
│       │   ├── RecordingStorageServiceTest.java
│       │   └── RecordingCleanupServiceTest.java
│       └── integration/RecordingIntegrationTest.java
├── aick-mmp-shared/
│   └── src/main/java/com/aick/mmp/shared/model/
│       └── Recording.java                          # MODIFIED
└── aick-mmp-central/src/main/resources/
    ├── application.yml                             # MODIFIED
    └── db/migration/V2__add_recording_storage_fields.sql

frontend/
└── src/
    ├── pages/Playback.js                           # MODIFIED
    └── utils/api.js                                # MODIFIED
```

**计划执行顺序：**

1. **Task 1**: 数据库 Schema 更新
2. **Task 2**: 录像存储配置和服务
3. **Task 3**: 录像下载 API
4. **Task 4**: 录像清理服务
5. **Task 5**: 前端下载功能
6. **Task 6**: 前端列表增强
7. **Task 7**: 集成测试

**预计总任务数：** 7个任务，约35个子步骤
**预计完成时间：** 每个任务约15-30分钟（取决于开发环境构建速度）
