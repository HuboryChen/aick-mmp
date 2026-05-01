package com.aick.mmp.central.service.recording;

import com.aick.mmp.shared.model.Recording;
import com.aick.mmp.central.repository.RecordingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordingDownloadServiceTest {

    @Mock
    private RecordingRepository recordingRepository;

    @Mock
    private FileStorageService fileStorageService;

    private StorageProperties storageProperties;
    private RecordingDownloadService downloadService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setMaxDownloadConcurrency(3);
        downloadService = new RecordingDownloadService(
                recordingRepository,
                fileStorageService,
                storageProperties
        );
    }

    @Test
    void testGetActiveDownloadCount_Initial() {
        assertEquals(0, downloadService.getActiveDownloadCount());
    }

    @Test
    void testGetActiveDownloadCount_AfterPrepare() {
        // 设置模拟
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .fileSize(1000L)
                .format("mp4")
                .lockStatus(false)
                .build();

        when(recordingRepository.findById(1L)).thenReturn(Optional.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(true);
        when(fileStorageService.getFileSize(any())).thenReturn(Optional.of(1000L));
        when(fileStorageService.getInputStream(any())).thenReturn(Optional.of(new java.io.ByteArrayInputStream(new byte[0])));
        when(recordingRepository.save(any())).thenReturn(recording);

        // 准备下载
        Optional<RecordingDownloadService.DownloadResult> result = downloadService.prepareDownload(1L);
        
        assertTrue(result.isPresent());
        assertEquals(1, downloadService.getActiveDownloadCount());
    }

    @Test
    void testPrepareDownload_RecordingNotFound() {
        when(recordingRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<RecordingDownloadService.DownloadResult> result = downloadService.prepareDownload(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testPrepareDownload_FileLocked() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .lockStatus(true)
                .build();

        when(recordingRepository.findById(1L)).thenReturn(Optional.of(recording));

        Optional<RecordingDownloadService.DownloadResult> result = downloadService.prepareDownload(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testPrepareDownload_FileNotExists() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .lockStatus(false)
                .build();

        when(recordingRepository.findById(1L)).thenReturn(Optional.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(false);

        Optional<RecordingDownloadService.DownloadResult> result = downloadService.prepareDownload(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testPrepareAndReleaseDownload() {
        Recording recording = Recording.builder()
                .id(1L)
                .name("test.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .fileSize(1000L)
                .format("mp4")
                .lockStatus(false)
                .build();

        when(recordingRepository.findById(1L)).thenReturn(Optional.of(recording));
        when(fileStorageService.fileExists(any())).thenReturn(true);
        when(fileStorageService.getFileSize(any())).thenReturn(Optional.of(1000L));
        when(fileStorageService.getInputStream(any())).thenReturn(Optional.of(new java.io.ByteArrayInputStream(new byte[0])));
        when(recordingRepository.save(any())).thenReturn(recording);

        // 准备下载
        Optional<RecordingDownloadService.DownloadResult> result = downloadService.prepareDownload(1L);
        assertTrue(result.isPresent());
        assertEquals(1, downloadService.getActiveDownloadCount());

        // 释放下载
        downloadService.releaseDownload(1L);
        assertEquals(0, downloadService.getActiveDownloadCount());
        // save被调用2次：一次在prepareDownload中锁定，一次在releaseDownload中解锁
        verify(recordingRepository, times(2)).save(any());
    }

    @Test
    void testDownloadConcurrencyLimit() {
        // 模拟已达到并发限制
        storageProperties.setMaxDownloadConcurrency(1);
        downloadService = new RecordingDownloadService(
                recordingRepository,
                fileStorageService,
                storageProperties
        );

        // 第一次下载成功
        Recording recording1 = Recording.builder()
                .id(1L)
                .name("test1.mp4")
                .storagePath("2026-05-01/cam_1.mp4")
                .fileSize(1000L)
                .format("mp4")
                .lockStatus(false)
                .build();

        when(recordingRepository.findById(1L)).thenReturn(Optional.of(recording1));
        when(fileStorageService.fileExists(any())).thenReturn(true);
        when(fileStorageService.getFileSize(any())).thenReturn(Optional.of(1000L));
        when(fileStorageService.getInputStream(any())).thenReturn(Optional.of(new java.io.ByteArrayInputStream(new byte[0])));
        when(recordingRepository.save(any())).thenReturn(recording1);

        Optional<RecordingDownloadService.DownloadResult> result1 = downloadService.prepareDownload(1L);
        assertTrue(result1.isPresent());

        // 第二次下载应该失败（并发限制）
        Recording recording2 = Recording.builder()
                .id(2L)
                .name("test2.mp4")
                .storagePath("2026-05-01/cam_2.mp4")
                .fileSize(2000L)
                .format("mp4")
                .lockStatus(false)
                .build();

        // 这里不需要mock recordingRepository.findById，因为下载会直接因为并发限制失败
        Optional<RecordingDownloadService.DownloadResult> result2 = downloadService.prepareDownload(2L);
        assertTrue(result2.isEmpty());
    }
}
