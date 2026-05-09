package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatisticsDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.impl.CameraServiceImpl;
import com.aick.mmp.central.service.NodeWeightCalculator;
import com.aick.mmp.central.service.StreamingService;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Recording;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.anyLong;

/**
 * Unit tests for CameraService
 */
@ExtendWith(MockitoExtension.class)
class CameraServiceTest {

    @Mock
    private CameraRepository cameraRepository;

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @Mock
    private RecordingRepository recordingRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private StreamingService streamingService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private NodeWeightCalculator nodeWeightCalculator;

    @InjectMocks
    private CameraServiceImpl cameraService;

    private Camera testCamera;
    private CameraDTO testCameraDTO;
    private EdgeNode testEdgeNode;

    @BeforeEach
    void setUp() {
        testCamera = Camera.builder()
                .id(1L)
                .name("Test Camera")
                .status(Camera.CameraStatus.ONLINE)
                .connectionUrl("rtsp://test.com/stream")
                .edgeNodeId(1L)
                .regionId(1L)
                .build();

        testCameraDTO = CameraDTO.builder()
                .id(1L)
                .name("Test Camera")
                .status(Camera.CameraStatus.ONLINE)
                .connectionUrl("rtsp://test.com/stream")
                .edgeNodeId(1L)
                .regionId(1L)
                .build();

        testEdgeNode = EdgeNode.builder()
                .id(1L)
                .name("Test Edge Node")
                .status(EdgeNode.NodeStatus.ONLINE)
                .build();
    }

    @Nested
    @DisplayName("getCameraById Tests")
    class GetCameraByIdTests {

        @Test
        @DisplayName("Should return camera when found")
        void testGetCameraByIdSuccess() {
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(modelMapper.map(testCamera, CameraDTO.class)).thenReturn(testCameraDTO);

            CameraDTO result = cameraService.getCameraById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Camera", result.getName());
            verify(cameraRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when camera not found")
        void testGetCameraByIdNotFound() {
            when(cameraRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cameraService.getCameraById(999L);
            });

            verify(cameraRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getAllCameras Tests")
    class GetAllCamerasTests {

        @Test
        @DisplayName("Should return paginated cameras")
        void testGetAllCameras() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Camera> cameraPage = new PageImpl<>(List.of(testCamera));

            when(cameraRepository.findAll(pageable)).thenReturn(cameraPage);
            when(modelMapper.map(testCamera, CameraDTO.class)).thenReturn(testCameraDTO);

            Page<CameraDTO> result = cameraService.getAllCameras(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Camera", result.getContent().get(0).getName());
            verify(cameraRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getCameraStatisticsSummary Tests")
    class GetCameraStatisticsSummaryTests {

        @Test
        @DisplayName("Should return statistics summary without filters")
        void testGetStatisticsSummaryNoFilters() {
            // Mock status counts - use lenient() for any status
            lenient().when(cameraRepository.countByStatus(any(Camera.CameraStatus.class))).thenReturn(2L);
            when(cameraRepository.countActive()).thenReturn(8L);

            // Mock edge node statistics
            when(edgeNodeRepository.findAll()).thenReturn(List.of(testEdgeNode));
            when(cameraRepository.findByEdgeNodeId(1L)).thenReturn(List.of(testCamera));

            // Mock recording statistics
            when(recordingRepository.count()).thenReturn(100L);
            when(recordingRepository.countOrphanedRecordings()).thenReturn(5L);
            when(recordingRepository.countDeletedRecordings()).thenReturn(10L);

            CameraStatisticsDTO result = cameraService.getCameraStatisticsSummary(null, null, false);

            assertNotNull(result);
            assertEquals(8L, result.getTotal());
            assertNotNull(result.getByStatus());
            assertNotNull(result.getByEdgeNode());
            assertEquals(1, result.getByEdgeNode().size());
            assertNotNull(result.getRecordingStatistics());
            assertEquals(100L, result.getRecordingStatistics().getTotalRecordings());
            assertEquals(5L, result.getRecordingStatistics().getOrphanedRecordings());
            assertEquals(10L, result.getRecordingStatistics().getDeletedRecordings());
            assertNotNull(result.getCachedAt());
        }

        @Test
        @DisplayName("Should filter by regionId when provided")
        void testGetStatisticsSummaryWithRegionId() {
            lenient().when(cameraRepository.countByRegionIdAndStatusAndIsDeletedFalse(anyLong(), any())).thenReturn(3L);
            when(cameraRepository.countByRegionIdAndIsDeletedFalse(1L)).thenReturn(10L);
            when(edgeNodeRepository.findAll()).thenReturn(List.of());

            CameraStatisticsDTO result = cameraService.getCameraStatisticsSummary(1L, null, false);

            assertNotNull(result);
            assertEquals(10L, result.getTotal());
        }

        @Test
        @DisplayName("Should filter by edgeNodeId when provided")
        void testGetStatisticsSummaryWithEdgeNodeId() {
            lenient().when(cameraRepository.countByStatus(any(Camera.CameraStatus.class))).thenReturn(2L);
            when(cameraRepository.countActive()).thenReturn(4L);
            when(edgeNodeRepository.findAll()).thenReturn(List.of(testEdgeNode));
            when(cameraRepository.findByEdgeNodeId(1L)).thenReturn(List.of(testCamera));
            when(recordingRepository.count()).thenReturn(50L);
            when(recordingRepository.countOrphanedRecordings()).thenReturn(2L);
            when(recordingRepository.countDeletedRecordings()).thenReturn(5L);

            CameraStatisticsDTO result = cameraService.getCameraStatisticsSummary(null, 1L, false);

            assertNotNull(result);
            assertEquals(4L, result.getTotal());
            verify(edgeNodeRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getOrphanedRecordingsCount Tests")
    class GetOrphanedRecordingsCountTests {

        @Test
        @DisplayName("Should return orphaned recordings count")
        void testGetOrphanedRecordingsCount() {
            when(recordingRepository.countOrphanedRecordings()).thenReturn(15L);

            long result = cameraService.getOrphanedRecordingsCount();

            assertEquals(15L, result);
            verify(recordingRepository).countOrphanedRecordings();
        }
    }

    @Nested
    @DisplayName("cleanupOrphanedRecordings Tests")
    class CleanupOrphanedRecordingsTests {

        @Test
        @DisplayName("Should cleanup orphaned recordings older than specified days")
        void testCleanupOrphanedRecordings() {
            Recording recording1 = Recording.builder().id(1L).name("Recording 1").build();
            Recording recording2 = Recording.builder().id(2L).name("Recording 2").build();
            List<Recording> orphanedRecordings = Arrays.asList(recording1, recording2);

            when(recordingRepository.findOrphanedRecordingsForCleanup(any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(orphanedRecordings));
            doNothing().when(recordingRepository).delete(any(Recording.class));

            int result = cameraService.cleanupOrphanedRecordings(30);

            assertEquals(2, result);
            verify(recordingRepository, times(2)).delete(any(Recording.class));
        }

        @Test
        @DisplayName("Should return zero when no orphaned recordings found")
        void testCleanupOrphanedRecordingsNoRecords() {
            when(recordingRepository.findOrphanedRecordingsForCleanup(any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            int result = cameraService.cleanupOrphanedRecordings(30);

            assertEquals(0, result);
            verify(recordingRepository, never()).delete(any(Recording.class));
        }
    }

    @Nested
    @DisplayName("getCameraStatistics Tests")
    class GetCameraStatisticsTests {

        @Test
        @DisplayName("Should return camera statistics map")
        void testGetCameraStatistics() {
            testCamera.setCreatedAt(LocalDateTime.now().minusDays(10));
            testCamera.setLastActiveTime(LocalDateTime.now());

            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));

            Map<String, Object> result = cameraService.getCameraStatistics(1L);

            assertNotNull(result);
            assertEquals(1L, result.get("id"));
            assertEquals("Test Camera", result.get("name"));
            assertEquals(Camera.CameraStatus.ONLINE, result.get("status"));
            assertNotNull(result.get("uptimePercentage"));
            verify(cameraRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when camera not found for statistics")
        void testGetCameraStatisticsNotFound() {
            when(cameraRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cameraService.getCameraStatistics(999L);
            });
        }
    }

    @Nested
    @DisplayName("testCameraConnection Tests")
    class TestCameraConnectionTests {

        @Test
        @DisplayName("Should return true when connection URL is valid")
        void testCameraConnectionSuccess() {
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));

            boolean result = cameraService.testCameraConnection(1L);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when connection URL is empty")
        void testCameraConnectionEmptyUrl() {
            testCamera.setConnectionUrl("");
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));

            boolean result = cameraService.testCameraConnection(1L);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should throw exception when camera not found")
        void testCameraConnectionNotFound() {
            when(cameraRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cameraService.testCameraConnection(999L);
            });
        }
    }

    @Nested
    @DisplayName("getCameraCountByStatus Tests")
    class GetCameraCountByStatusTests {

        @Test
        @DisplayName("Should return count of cameras by status")
        void testGetCameraCountByStatus() {
            when(cameraRepository.countByStatus(Camera.CameraStatus.ONLINE)).thenReturn(10L);

            long result = cameraService.getCameraCountByStatus(Camera.CameraStatus.ONLINE);

            assertEquals(10L, result);
            verify(cameraRepository).countByStatus(Camera.CameraStatus.ONLINE);
        }
    }

    @Nested
    @DisplayName("refreshStatisticsCache Tests")
    class RefreshStatisticsCacheTests {

        @Test
        @DisplayName("Should refresh statistics cache successfully")
        void testRefreshStatisticsCache() {
            assertDoesNotThrow(() -> cameraService.refreshStatisticsCache());
        }
    }
}
