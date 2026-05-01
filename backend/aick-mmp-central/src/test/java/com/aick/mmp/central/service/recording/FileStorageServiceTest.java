package com.aick.mmp.central.service.recording;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private StorageProperties storageProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setLocalPath(tempDir.toString());
        fileStorageService = new FileStorageService(storageProperties);
    }

    @Test
    void testGetRelativePath() {
        String path = fileStorageService.getRelativePath("2026-05-01", 1L);
        assertTrue(path.contains("2026-05-01"));
        assertTrue(path.contains("cam_1.mp4"));
    }

    @Test
    void testFileExists() throws IOException {
        // 创建测试文件
        Path testDir = tempDir.resolve("2026-05-01");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve("cam_1.mp4");
        Files.writeString(testFile, "test content");

        assertTrue(fileStorageService.fileExists("2026-05-01/cam_1.mp4"));
        assertFalse(fileStorageService.fileExists("2026-05-01/cam_999.mp4"));
    }

    @Test
    void testGetFileSize() throws IOException {
        // 创建测试文件
        Path testDir = tempDir.resolve("2026-05-01");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve("cam_1.mp4");
        String content = "test content with some bytes";
        Files.writeString(testFile, content);

        Optional<Long> size = fileStorageService.getFileSize("2026-05-01/cam_1.mp4");
        assertTrue(size.isPresent());
        assertEquals((long) content.getBytes().length, size.get());
    }

    @Test
    void testCalculateMd5() throws IOException {
        // 创建测试文件
        Path testDir = tempDir.resolve("2026-05-01");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve("cam_1.mp4");
        Files.writeString(testFile, "test content");

        Optional<String> md5 = fileStorageService.calculateMd5("2026-05-01/cam_1.mp4");
        assertTrue(md5.isPresent());
        // MD5 of "test content" is known
        assertEquals(32, md5.get().length()); // MD5 is 32 hex chars
    }

    @Test
    void testVerifyIntegrity() throws IOException {
        // 创建测试文件
        Path testDir = tempDir.resolve("2026-05-01");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve("cam_1.mp4");
        Files.writeString(testFile, "test content");

        // 计算MD5
        Optional<String> md5 = fileStorageService.calculateMd5("2026-05-01/cam_1.mp4");
        assertTrue(md5.isPresent());

        // 验证正确MD5
        assertTrue(fileStorageService.verifyIntegrity("2026-05-01/cam_1.mp4", md5.get()));

        // 验证错误MD5
        assertFalse(fileStorageService.verifyIntegrity("2026-05-01/cam_1.mp4", "wrong_md5"));
    }

    @Test
    void testDeleteFile() throws IOException {
        // 创建测试文件
        Path testDir = tempDir.resolve("2026-05-01");
        Files.createDirectories(testDir);
        Path testFile = testDir.resolve("cam_1.mp4");
        Files.writeString(testFile, "test content");

        assertTrue(fileStorageService.fileExists("2026-05-01/cam_1.mp4"));

        // 删除文件
        assertTrue(fileStorageService.deleteFile("2026-05-01/cam_1.mp4"));
        assertFalse(fileStorageService.fileExists("2026-05-01/cam_1.mp4"));
    }

    @Test
    void testGetDatePath() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        String path = fileStorageService.getDatePath(date);
        assertEquals("2026-05-01", path);
    }

    @Test
    void testEnsureDirectoryExists() throws IOException {
        String relativePath = "2026-05-01/cam_1.mp4";
        fileStorageService.ensureDirectoryExists(relativePath);

        Path expectedDir = tempDir.resolve("2026-05-01");
        assertTrue(Files.exists(expectedDir));
    }
}
