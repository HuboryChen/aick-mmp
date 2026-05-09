package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RecordingDTO;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.impl.RecordingServiceImpl;
import com.aick.mmp.shared.model.Recording;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecordingService - Soft Delete functionality
 */
@ExtendWith(MockitoExtension.class)
class RecordingServiceTest {

    @Mock
    private RecordingRepository recordingRepository;

    @InjectMocks
    private RecordingServiceImpl recordingService;

    private Recording testRecording;
    private RecordingDTO testRecordingDTO;

    @BeforeEach
    void setUp() {
        testRecording = Recording.builder()
                .id(1L)
                .name("Test Recording")
                .cameraId(100L)
                .filePath("/storage/recordings/test.mp4")
                .fileSize(1024L * 1024L * 100L) // 100MB
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .duration(3600)
                .status("completed")
                .format("mp4")
                .resolution("1920x1080")
                .isDeleted(false)
                .build();

        testRecordingDTO = RecordingDTO.builder()
                .id(1L)
                .cameraId(100L)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .duration(3600L)
                .size(1024L * 1024L * 100L)
                .quality("1920x1080")
                .storagePath("/storage/recordings/test.mp4")
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("Soft Delete Tests")
    class SoftDeleteTests {

        @Test
        @DisplayName("Should soft delete recording successfully")
        void testSoftDeleteSuccess() {
            when(recordingRepository.softDelete(eq(1L), any(LocalDateTime.class))).thenReturn(1);

            assertDoesNotThrow(() -> recordingService.softDelete(1L));

            verify(recordingRepository).softDelete(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw exception when recording not found for soft delete")
        void testSoftDeleteNotFound() {
            when(recordingRepository.softDelete(eq(999L), any(LocalDateTime.class))).thenReturn(0);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                recordingService.softDelete(999L);
            });

            assertTrue(exception.getMessage().contains("Recording not found for soft delete"));
            assertTrue(exception.getMessage().contains("999"));
            verify(recordingRepository).softDelete(eq(999L), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Restore Tests")
    class RestoreTests {

        @Test
        @DisplayName("Should restore deleted recording successfully")
        void testRestoreSuccess() {
            when(recordingRepository.restore(eq(1L))).thenReturn(1);

            assertDoesNotThrow(() -> recordingService.restore(1L));

            verify(recordingRepository).restore(eq(1L));
        }

        @Test
        @DisplayName("Should throw exception when recording not found for restore")
        void testRestoreNotFound() {
            when(recordingRepository.restore(eq(999L))).thenReturn(0);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                recordingService.restore(999L);
            });

            assertTrue(exception.getMessage().contains("Recording not found for restore"));
            assertTrue(exception.getMessage().contains("999"));
            verify(recordingRepository).restore(eq(999L));
        }
    }

    @Nested
    @DisplayName("Get Recording By ID Tests")
    class GetRecordingByIdTests {

        @Test
        @DisplayName("Should return recording by ID")
        void testGetRecordingById() {
            when(recordingRepository.findById(1L)).thenReturn(Optional.of(testRecording));

            RecordingDTO result = recordingService.getRecordingById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(100L, result.getCameraId());
            verify(recordingRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when recording not found")
        void testGetRecordingByIdNotFound() {
            when(recordingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                recordingService.getRecordingById(999L);
            });
        }

        @Test
        @DisplayName("Should return recording with includeDeleted flag")
        void testGetRecordingByIdWithIncludeDeleted() {
            testRecording.setIsDeleted(true);
            testRecording.setDeletedAt(LocalDateTime.now());
            when(recordingRepository.findById(1L)).thenReturn(Optional.of(testRecording));

            RecordingDTO result = recordingService.getRecordingById(1L, true);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(recordingRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get Recordings Tests")
    class GetRecordingsTests {

        @Test
        @DisplayName("Should return paginated recordings")
        void testGetRecordings() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Recording> recordingPage = new PageImpl<>(List.of(testRecording));

            when(recordingRepository.findAll(pageable)).thenReturn(recordingPage);

            Page<RecordingDTO> result = recordingService.getRecordings(null, null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(1L, result.getContent().get(0).getId());
            verify(recordingRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return recordings with includeDeleted flag")
        void testGetRecordingsWithIncludeDeleted() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Recording> recordingPage = new PageImpl<>(List.of(testRecording));

            when(recordingRepository.findAll(pageable)).thenReturn(recordingPage);

            Page<RecordingDTO> result = recordingService.getRecordings(null, null, null, null, pageable, true);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(recordingRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Get Recordings By Camera ID Tests")
    class GetRecordingsByCameraIdTests {

        @Test
        @DisplayName("Should return recordings by camera ID")
        void testGetRecordingsByCameraId() {
            when(recordingRepository.findByCameraId(100L)).thenReturn(List.of(testRecording));

            List<RecordingDTO> result = recordingService.getRecordingsByCameraId(100L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(100L, result.get(0).getCameraId());
            verify(recordingRepository).findByCameraId(100L);
        }

        @Test
        @DisplayName("Should return empty list when no recordings found")
        void testGetRecordingsByCameraIdEmpty() {
            when(recordingRepository.findByCameraId(999L)).thenReturn(List.of());

            List<RecordingDTO> result = recordingService.getRecordingsByCameraId(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(recordingRepository).findByCameraId(999L);
        }
    }

    @Nested
    @DisplayName("Orphaned Recordings Tests")
    class OrphanedRecordingsTests {

        @Test
        @DisplayName("Should return orphaned recordings")
        void testGetOrphanedRecordings() {
            testRecording.setOrphanedAt(LocalDateTime.now());
            testRecording.setOrphanedBy(999L);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Recording> orphanedPage = new PageImpl<>(List.of(testRecording));

            when(recordingRepository.findOrphanedRecordings(pageable)).thenReturn(orphanedPage);

            Page<RecordingDTO> result = recordingService.getOrphanedRecordings(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertNotNull(result.getContent().get(0).getOrphanedAt());
            verify(recordingRepository).findOrphanedRecordings(pageable);
        }

        @Test
        @DisplayName("Should return orphaned recordings count")
        void testCountOrphanedRecordings() {
            when(recordingRepository.countOrphanedRecordings()).thenReturn(5L);

            long result = recordingService.countOrphanedRecordings();

            assertEquals(5L, result);
            verify(recordingRepository).countOrphanedRecordings();
        }
    }

    @Nested
    @DisplayName("Deleted Recordings Tests")
    class DeletedRecordingsTests {

        @Test
        @DisplayName("Should return deleted recordings")
        void testGetDeletedRecordings() {
            testRecording.setIsDeleted(true);
            testRecording.setDeletedAt(LocalDateTime.now());
            Pageable pageable = PageRequest.of(0, 10);
            Page<Recording> deletedPage = new PageImpl<>(List.of(testRecording));

            when(recordingRepository.findDeletedRecordings(pageable)).thenReturn(deletedPage);

            Page<RecordingDTO> result = recordingService.getDeletedRecordings(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertTrue(result.getContent().get(0).getIsDeleted());
            verify(recordingRepository).findDeletedRecordings(pageable);
        }
    }

    @Nested
    @DisplayName("Cleanup Orphaned Recordings Tests")
    class CleanupOrphanedRecordingsTests {

        @Test
        @DisplayName("Should cleanup orphaned recordings older than specified days")
        void testCleanupOrphanedRecordings() {
            Recording orphanedRecording = Recording.builder()
                    .id(2L)
                    .cameraId(999L)
                    .orphanedAt(LocalDateTime.now().minusDays(60))
                    .build();

            Page<Recording> orphanedPage = new PageImpl<>(List.of(orphanedRecording));
            when(recordingRepository.findOrphanedRecordingsForCleanup(any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(orphanedPage);
            doNothing().when(recordingRepository).delete(any(Recording.class));

            int result = recordingService.cleanupOrphanedRecordings(30);

            assertEquals(1, result);
            verify(recordingRepository).delete(any(Recording.class));
        }

        @Test
        @DisplayName("Should return zero when no orphaned recordings to cleanup")
        void testCleanupOrphanedRecordingsNoRecords() {
            Page<Recording> emptyPage = new PageImpl<>(List.of());
            when(recordingRepository.findOrphanedRecordingsForCleanup(any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            int result = recordingService.cleanupOrphanedRecordings(30);

            assertEquals(0, result);
            verify(recordingRepository, never()).delete(any(Recording.class));
        }
    }

    @Nested
    @DisplayName("Get Recording URL Tests")
    class GetRecordingUrlTests {

        @Test
        @DisplayName("Should return recording stream URL")
        void testGetRecordingUrl() {
            String result = recordingService.getRecordingUrl(1L);

            assertEquals("/api/recordings/1/stream", result);
        }
    }

    @Nested
    @DisplayName("Get Total Recording Size Tests")
    class GetTotalRecordingSizeTests {

        @Test
        @DisplayName("Should return total recording size")
        void testGetTotalRecordingSize() {
            // Current implementation returns 0L (TODO)
            long result = recordingService.getTotalRecordingSize();

            assertEquals(0L, result);
        }
    }
}
