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
                .fileSize(1024L * 1024 * 100)
                .md5("d41d8cd98f00b204e9800998ecf8427e")
                .storagePath("/mnt/storage/recordings/2026-05-01/cam_001.mp4")
                .integrityStatus("PENDING")
                .lockStatus(false)
                .startTime(java.time.LocalDateTime.now())
                .build();
        
        assertNotNull(recording.getFilePath());
        assertNotNull(recording.getFileSize());
        assertNotNull(recording.getMd5());
        assertNotNull(recording.getStoragePath());
        assertNotNull(recording.getIntegrityStatus());
        assertNotNull(recording.getLockStatus());
    }

    @Test
    void testRecordingStatusEnum() {
        Recording recording = Recording.builder().build();
        recording.setStatus("COMPLETED");
        assertEquals("COMPLETED", recording.getStatus());
    }
    
    @Test
    void testRecordingLockStatus() {
        Recording recording = Recording.builder().build();
        recording.setLockStatus(true);
        assertTrue(recording.getLockStatus());
    }
    
    @Test
    void testRecordingIntegrityStatus() {
        Recording recording = Recording.builder().build();
        recording.setIntegrityStatus("COMPLETED");
        assertEquals("COMPLETED", recording.getIntegrityStatus());
    }
}
