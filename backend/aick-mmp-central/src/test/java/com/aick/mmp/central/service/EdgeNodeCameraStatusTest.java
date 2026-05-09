package com.aick.mmp.central.service;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.impl.EdgeNodeServiceImpl;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EdgeNodeService - Camera Status Synchronization
 */
@ExtendWith(MockitoExtension.class)
class EdgeNodeCameraStatusTest {

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @Mock
    private CameraRepository cameraRepository;

    @Mock
    private NetworkMonitorService networkMonitorService;

    @InjectMocks
    private EdgeNodeServiceImpl edgeNodeService;

    private EdgeNode testEdgeNode;
    private Camera testCamera;
    private List<Map<String, Object>> cameraStatuses;

    @BeforeEach
    void setUp() {
        testEdgeNode = EdgeNode.builder()
                .id(1L)
                .name("test-edge-node")
                .uuid("edge-uuid-123")
                .location("Test Location")
                .status(EdgeNode.EdgeNodeStatus.ONLINE)
                .registeredAt(LocalDateTime.now().minusDays(10))
                .lastHeartbeat(LocalDateTime.now().minusMinutes(5))
                .build();

        testCamera = Camera.builder()
                .id(100L)
                .name("Test Camera")
                .edgeNodeId(1L)
                .status(Camera.CameraStatus.ONLINE)
                .rtspUrl("rtsp://localhost:554/stream")
                .currentBitrate(2048)
                .currentFps(30.0)
                .build();

        cameraStatuses = new ArrayList<>();
        Map<String, Object> status1 = new HashMap<>();
        status1.put("cameraId", 100L);
        status1.put("status", "ONLINE");
        status1.put("currentBitrate", 2048);
        status1.put("currentFps", 30.0);
        status1.put("errorCode", null);
        status1.put("errorMessage", null);
        cameraStatuses.add(status1);
    }

    @Nested
    @DisplayName("Camera Status Processing Tests")
    class CameraStatusProcessingTests {

        @Test
        @DisplayName("Should process camera status reports successfully")
        void testProcessCameraStatusReportsSuccess() {
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals(1, result.get("processedCount"));
            assertEquals(1, result.get("totalCount"));
            assertEquals(0, result.get("errorCount"));

            verify(cameraRepository).save(any(Camera.class));
        }

        @Test
        @DisplayName("Should find edge node by UUID when name not found")
        void testProcessCameraStatusReportsFindByUuid() {
            when(edgeNodeRepository.findByName("edge-uuid-123")).thenReturn(Optional.empty());
            when(edgeNodeRepository.findByUuid("edge-uuid-123")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("edge-uuid-123", cameraStatuses);

            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals(1, result.get("processedCount"));

            verify(edgeNodeRepository).findByName("edge-uuid-123");
            verify(edgeNodeRepository).findByUuid("edge-uuid-123");
        }

        @Test
        @DisplayName("Should return error when edge node not found")
        void testProcessCameraStatusReportsEdgeNodeNotFound() {
            when(edgeNodeRepository.findByName("non-existent")).thenReturn(Optional.empty());
            when(edgeNodeRepository.findByUuid("non-existent")).thenReturn(Optional.empty());

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("non-existent", cameraStatuses);

            assertNotNull(result);
            assertEquals(false, result.get("success"));
            assertEquals("Edge node not found", result.get("error"));

            verify(cameraRepository, never()).save(any(Camera.class));
        }

        @Test
        @DisplayName("Should skip camera when camera not found")
        void testProcessCameraStatusReportsCameraNotFound() {
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.empty());

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals(0, result.get("processedCount"));
            assertEquals(1, result.get("errorCount"));

            verify(cameraRepository, never()).save(any(Camera.class));
        }

        @Test
        @DisplayName("Should skip camera when it does not belong to edge node")
        void testProcessCameraStatusReportsCameraNotBelongToNode() {
            testCamera.setEdgeNodeId(999L); // Different edge node
            
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals(0, result.get("processedCount"));

            verify(cameraRepository, never()).save(any(Camera.class));
        }

        @Test
        @DisplayName("Should update camera status from report")
        void testProcessCameraStatusReportsUpdateStatus() {
            testCamera.setStatus(Camera.CameraStatus.OFFLINE);
            
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));
            assertEquals(1, result.get("processedCount"));

            verify(cameraRepository).save(argThat(camera -> 
                camera.getStatus() == Camera.CameraStatus.ONLINE
            ));
        }

        @Test
        @DisplayName("Should update camera bitrate from report")
        void testProcessCameraStatusReportsUpdateBitrate() {
            testCamera.setCurrentBitrate(1024);
            
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));

            verify(cameraRepository).save(argThat(camera -> 
                camera.getCurrentBitrate() == 2048
            ));
        }

        @Test
        @DisplayName("Should update camera error info from report")
        void testProcessCameraStatusReportsUpdateErrorInfo() {
            cameraStatuses.get(0).put("errorCode", "CONNECTION_FAILED");
            cameraStatuses.get(0).put("errorMessage", "Camera connection timeout");
            
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));

            verify(cameraRepository).save(argThat(camera -> 
                "CONNECTION_FAILED".equals(camera.getLastErrorCode()) &&
                "Camera connection timeout".equals(camera.getLastErrorMessage())
            ));
        }

        @Test
        @DisplayName("Should update camera lastHeartbeatTime")
        void testProcessCameraStatusReportsUpdateLastHeartbeatTime() {
            LocalDateTime beforeUpdate = LocalDateTime.now().minusHours(1);
            testCamera.setLastHeartbeatTime(beforeUpdate);
            
            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));

            verify(cameraRepository).save(argThat(camera -> 
                camera.getLastHeartbeatTime() != null &&
                camera.getLastHeartbeatTime().isAfter(beforeUpdate)
            ));
        }

        @Test
        @DisplayName("Should handle multiple cameras in status report")
        void testProcessCameraStatusReportsMultipleCameras() {
            Camera testCamera2 = Camera.builder()
                    .id(101L)
                    .name("Test Camera 2")
                    .edgeNodeId(1L)
                    .status(Camera.CameraStatus.ONLINE)
                    .build();

            Map<String, Object> status2 = new HashMap<>();
            status2.put("cameraId", 101L);
            status2.put("status", "ONLINE");
            status2.put("currentBitrate", 4096);
            status2.put("currentFps", 60.0);
            cameraStatuses.add(status2);

            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));
            when(cameraRepository.findById(100L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.findById(101L)).thenReturn(Optional.of(testCamera2));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));
            assertEquals(2, result.get("processedCount"));
            assertEquals(2, result.get("totalCount"));

            verify(cameraRepository, times(2)).save(any(Camera.class));
        }

        @Test
        @DisplayName("Should handle invalid camera ID format gracefully")
        void testProcessCameraStatusReportsInvalidCameraId() {
            Map<String, Object> invalidStatus = new HashMap<>();
            invalidStatus.put("cameraId", "invalid");
            cameraStatuses.add(invalidStatus);

            when(edgeNodeRepository.findByName("test-edge-node")).thenReturn(Optional.of(testEdgeNode));

            Map<String, Object> result = edgeNodeService.processCameraStatusReports("test-edge-node", cameraStatuses);

            assertEquals(true, result.get("success"));
            assertEquals(1, result.get("processedCount"));
            assertEquals(1, result.get("errorCount"));

            List<String> errors = (List<String>) result.get("errors");
            assertNotNull(errors);
            assertTrue(errors.size() > 0);
        }
    }
}
