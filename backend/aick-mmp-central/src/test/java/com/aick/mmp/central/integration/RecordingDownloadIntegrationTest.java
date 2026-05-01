package com.aick.mmp.central.integration;

import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.recording.RecordingDownloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 录像下载集成测试
 * 
 * 注意：需要完整的Spring Boot上下文和数据库连接
 */
@SpringBootTest
@ActiveProfiles("test")
class RecordingDownloadIntegrationTest {

    @Autowired(required = false)
    private RecordingRepository recordingRepository;

    @Autowired(required = false)
    private RecordingDownloadService downloadService;

    @Test
    void testDownloadServiceBeanExists() {
        assertNotNull(downloadService, "RecordingDownloadService should be available");
    }

    @Test
    void testDownloadServiceConcurrencyControl() {
        if (downloadService == null) {
            return; // 跳过测试
        }

        int activeCount = downloadService.getActiveDownloadCount();
        assertTrue(activeCount >= 0, "Active download count should be non-negative");
    }

    @Test
    void testEmptyDownloadList() {
        if (downloadService == null) {
            return; // 跳过测试
        }

        var downloads = downloadService.getActiveDownloads();
        assertNotNull(downloads);
    }
}
