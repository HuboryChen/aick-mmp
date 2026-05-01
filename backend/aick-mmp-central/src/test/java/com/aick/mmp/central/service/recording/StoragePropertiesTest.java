package com.aick.mmp.central.service.recording;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoragePropertiesTest {

    private StorageProperties storageProperties;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
    }

    @Test
    void testDefaultValues() {
        assertEquals("/data/recordings", storageProperties.getLocalPath());
        assertEquals(30, storageProperties.getRetentionDays());
        assertEquals(0.8, storageProperties.getDiskUsageThreshold());
        assertEquals(0.7, storageProperties.getCleanupRetentionRatio());
    }

    @Test
    void testGetStoragePath() {
        String path = storageProperties.getStoragePath("2026-05-01", 1L);
        assertTrue(path.contains("2026-05-01"));
        assertTrue(path.contains("cam_1.mp4"));
    }

    @Test
    void testGetStoragePathWithDifferentCamera() {
        String path = storageProperties.getStoragePath("2026-05-01", 100L);
        assertTrue(path.contains("2026-05-01"));
        assertTrue(path.contains("cam_100.mp4"));
    }

    @Test
    void testGetCleanupTargetBytes() {
        long totalBytes = 1000L * 1024 * 1024 * 1024; // 1TB
        long targetBytes = storageProperties.getCleanupTargetBytes(totalBytes);
        assertEquals(700L * 1024 * 1024 * 1024, targetBytes); // 70% of 1TB
    }

    @Test
    void testDownloadConfig() {
        assertEquals(3, storageProperties.getMaxDownloadConcurrency());
        assertEquals(10, storageProperties.getMaxDownloadBandwidthMB());
    }

    @Test
    void testSetters() {
        storageProperties.setLocalPath("/custom/path");
        storageProperties.setRetentionDays(7);
        storageProperties.setDiskUsageThreshold(0.9);

        assertEquals("/custom/path", storageProperties.getLocalPath());
        assertEquals(7, storageProperties.getRetentionDays());
        assertEquals(0.9, storageProperties.getDiskUsageThreshold());
    }
}
