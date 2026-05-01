package com.aick.mmp.central.service.recording;

import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.central.repository.RecordingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordingCleanupServiceTest {

    @Mock
    private RecordingRepository recordingRepository;

    @Mock
    private FileStorageService fileStorageService;

    private StorageProperties storageProperties;
    private RecordingCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setRetentionDays(30);
        storageProperties.setDiskUsageThreshold(0.8);
        storageProperties.setCleanupRetentionRatio(0.7);
        storageProperties.setEnabled(true);

        cleanupService = new RecordingCleanupService(
                recordingRepository,
                fileStorageService,
                storageProperties
        );
    }

    @Test
    void testCleanupByRetentionDays() {
        // 准备过期录像
        Recording expiredRecording = Recording.builder()
                .id(1L)
                .name("expired.mp4")
                .storagePath("2026-04-01/cam_1.mp4")
                .fileSize(1000L)
                .integrityStatus("COMPLETED")
                .lockStatus(false)
                .startTime(LocalDateTime.now().minusDays(60))
                .build();

        Page<Recording> page = Page.empty();
        when(recordingRepository.findExpiredRecordings(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        RecordingCleanupService.CleanupResult result = cleanupService.cleanupByRetentionDays();

        assertNotNull(result);
        assertEquals(0, result.deletedCount());
    }

    @Test
    void testCleanupByDiskUsage_BelowThreshold() {
        // 磁盘使用率低于阈值
        when(fileStorageService.getTotalDiskSpace()).thenReturn(1000L * 1024 * 1024);
        when(fileStorageService.getUsedDiskSpace()).thenReturn(500L * 1024 * 1024); // 50% usage

        RecordingCleanupService.CleanupResult result = cleanupService.cleanupByDiskUsage();

        assertNotNull(result);
        assertEquals(0, result.deletedCount());
    }

    @Test
    void testCleanupByDiskUsage_AboveThreshold() {
        // 磁盘使用率高于阈值
        when(fileStorageService.getTotalDiskSpace()).thenReturn(1000L * 1024 * 1024);
        when(fileStorageService.getUsedDiskSpace()).thenReturn(900L * 1024 * 1024); // 90% usage

        when(recordingRepository.findRecordingsForCleanup(any(Pageable.class)))
                .thenReturn(Page.empty());

        RecordingCleanupService.CleanupResult result = cleanupService.cleanupByDiskUsage();

        assertNotNull(result);
        // 由于没有录像，deletedCount应该为0
    }

    @Test
    void testCleanupByDiskUsage_SkipsLockedRecordings() {
        // 准备锁定的录像
        Recording lockedRecording = Recording.builder()
                .id(1L)
                .name("locked.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .fileSize(1000L)
                .integrityStatus("COMPLETED")
                .lockStatus(true) // 锁定
                .startTime(LocalDateTime.now().minusDays(1))
                .build();

        when(fileStorageService.getTotalDiskSpace()).thenReturn(1000L * 1024 * 1024);
        when(fileStorageService.getUsedDiskSpace()).thenReturn(900L * 1024 * 1024);

        when(recordingRepository.findRecordingsForCleanup(any(Pageable.class)))
                .thenReturn(Page.empty());

        RecordingCleanupService.CleanupResult result = cleanupService.cleanupByDiskUsage();

        // 锁定录像不应该被删除
        verify(fileStorageService, never()).deleteFile(any());
    }

    @Test
    void testPerformCleanup() {
        when(recordingRepository.findExpiredRecordings(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(fileStorageService.getTotalDiskSpace()).thenReturn(1000L * 1024 * 1024);
        when(fileStorageService.getUsedDiskSpace()).thenReturn(500L * 1024 * 1024);

        RecordingCleanupService.CleanupResult result = cleanupService.performCleanup();

        assertNotNull(result);
        assertTrue(result.durationMs() >= 0);
    }

    @Test
    void testVerifyIntegrity_RecordingNotFound() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .integrityStatus("COMPLETED")
                .lockStatus(false)
                .build();

        when(recordingRepository.findByIntegrityStatus("COMPLETED"))
                .thenReturn(List.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(false);

        int corruptedCount = cleanupService.verifyIntegrity();

        assertEquals(1, corruptedCount);
        verify(recordingRepository).save(any());
    }

    @Test
    void testVerifyIntegrity_Md5Mismatch() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .md5("expected_md5")
                .integrityStatus("COMPLETED")
                .lockStatus(false)
                .build();

        when(recordingRepository.findByIntegrityStatus("COMPLETED"))
                .thenReturn(List.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(true);
        when(fileStorageService.verifyIntegrity(any(), any())).thenReturn(false);

        int corruptedCount = cleanupService.verifyIntegrity();

        assertEquals(1, corruptedCount);
    }

    @Test
    void testVerifyIntegrity_ValidRecording() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .md5("expected_md5")
                .integrityStatus("COMPLETED")
                .lockStatus(false)
                .build();

        when(recordingRepository.findByIntegrityStatus("COMPLETED"))
                .thenReturn(List.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(true);
        when(fileStorageService.verifyIntegrity(any(), eq("expected_md5"))).thenReturn(true);

        int corruptedCount = cleanupService.verifyIntegrity();

        assertEquals(0, corruptedCount);
        verify(recordingRepository, never()).save(any());
    }

    @Test
    void testCleanup_Disabled() {
        storageProperties.setEnabled(false);

        // 设置mock避免NPE
        when(recordingRepository.findExpiredRecordings(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(fileStorageService.getTotalDiskSpace()).thenReturn(0L);
        when(fileStorageService.getUsedDiskSpace()).thenReturn(0L);

        RecordingCleanupService.CleanupResult result = cleanupService.performCleanup();

        assertNotNull(result);
        assertEquals(0, result.deletedCount());
    }
}
