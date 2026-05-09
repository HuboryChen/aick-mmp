package com.aick.mmp.central.service;

import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.impl.CameraServiceImpl;
import com.aick.mmp.central.service.NodeWeightCalculator;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 单元测试: 验证 CameraService.deleteCamera() 的级联录像处理逻辑
 * 对应任务 3.3 - 摄像头删除级联录像处理
 */
@ExtendWith(MockitoExtension.class)
class CameraCascadeDeleteTest {

    @Mock
    private CameraRepository cameraRepository;

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @Mock
    private RecordingRepository recordingRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private CameraServiceImpl cameraService;

    private Camera testCamera;
    private EdgeNode testEdgeNode;

    @BeforeEach
    void setUp() {
        testCamera = Camera.builder()
                .id(1L)
                .name("Test Camera")
                .status(Camera.CameraStatus.ONLINE)
                .connectionUrl("rtsp://test.com/stream")
                .edgeNodeId(100L)
                .regionId(1L)
                .build();

        testEdgeNode = EdgeNode.builder()
                .id(100L)
                .name("Test Edge Node")
                .status(EdgeNode.NodeStatus.ONLINE)
                .currentCameraCount(5)
                .build();
    }

    @Nested
    @DisplayName("deleteCamera 级联处理测试")
    class DeleteCameraCascadeTests {

        @Test
        @DisplayName("删除摄像头时应调用 recordingRepository.markOrphanedByCameraId")
        void testDeleteCameraCallsMarkOrphaned() {
            // Given
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);
            when(recordingRepository.markOrphanedByCameraId(eq(1L), any(LocalDateTime.class), eq(1L))).thenReturn(3);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When
            cameraService.deleteCamera(1L);

            // Then
            verify(recordingRepository).markOrphanedByCameraId(eq(1L), any(LocalDateTime.class), eq(1L));
        }

        @Test
        @DisplayName("删除摄像头时应减少边缘节点的摄像头计数")
        void testDeleteCameraDecrementsEdgeNodeCount() {
            // Given
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(LocalDateTime.class), anyLong())).thenReturn(0);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When
            cameraService.deleteCamera(1L);

            // Then
            verify(edgeNodeRepository).findById(100L);
            verify(edgeNodeRepository).save(argThat(node -> {
                EdgeNode savedNode = (EdgeNode) node;
                return savedNode.getCurrentCameraCount() == 4; // 5 - 1 = 4
            }));
        }

        @Test
        @DisplayName("删除无关联边缘节点的摄像头时不应调用 edgeNodeRepository")
        void testDeleteCameraWithNoEdgeNode() {
            // Given
            testCamera.setEdgeNodeId(null);
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(LocalDateTime.class), anyLong())).thenReturn(2);

            // When
            cameraService.deleteCamera(1L);

            // Then
            verify(edgeNodeRepository, never()).findById(anyLong());
            verify(edgeNodeRepository, never()).save(any(EdgeNode.class));
        }

        @Test
        @DisplayName("删除摄像头时应设置 deletedAt 时间戳")
        void testDeleteCameraSetsDeletedAt() {
            // Given
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(invocation -> {
                Camera savedCamera = invocation.getArgument(0);
                assertNotNull(savedCamera.getDeletedAt(), "deletedAt should be set");
                return savedCamera;
            });
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(LocalDateTime.class), anyLong())).thenReturn(0);

            // When
            cameraService.deleteCamera(1L);

            // Then
            verify(cameraRepository).save(argThat(camera -> 
                camera.getDeletedAt() != null && camera.getUpdatedAt() != null
            ));
        }

        @Test
        @DisplayName("删除不存在的摄像头应抛出 ResourceNotFoundException")
        void testDeleteCameraNotFound() {
            // Given
            when(cameraRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                cameraService.deleteCamera(999L);
            });

            // Verify recordingRepository 未被调用
            verify(recordingRepository, never()).markOrphanedByCameraId(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("删除摄像头时边缘节点计数不应为负数")
        void testDeleteCameraEdgeNodeCountNotNegative() {
            // Given
            testEdgeNode.setCurrentCameraCount(0); // 已经是0
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(LocalDateTime.class), anyLong())).thenReturn(0);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When
            cameraService.deleteCamera(1L);

            // Then
            verify(edgeNodeRepository).save(argThat(node -> {
                EdgeNode savedNode = (EdgeNode) node;
                return savedNode.getCurrentCameraCount() >= 0;
            }));
        }

        @Test
        @DisplayName("边缘节点计数为 null 时删除摄像头应正常处理")
        void testDeleteCameraWithNullEdgeNodeCount() {
            // Given
            testEdgeNode.setCurrentCameraCount(null);
            when(cameraRepository.findById(1L)).thenReturn(Optional.of(testCamera));
            when(cameraRepository.save(any(Camera.class))).thenReturn(testCamera);
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(LocalDateTime.class), anyLong())).thenReturn(0);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When & Then
            assertDoesNotThrow(() -> cameraService.deleteCamera(1L));

            verify(edgeNodeRepository).save(argThat(node -> 
                ((EdgeNode) node).getCurrentCameraCount() == 0
            ));
        }
    }

    @Nested
    @DisplayName("batchDeleteCameras 级联处理测试")
    class BatchDeleteCamerasCascadeTests {

        @Test
        @DisplayName("批量删除应级联处理每个摄像头")
        void testBatchDeleteCamerasCascades() {
            // Given
            Camera camera1 = Camera.builder().id(1L).edgeNodeId(100L).build();
            Camera camera2 = Camera.builder().id(2L).edgeNodeId(100L).build();

            when(cameraRepository.findById(1L)).thenReturn(Optional.of(camera1));
            when(cameraRepository.findById(2L)).thenReturn(Optional.of(camera2));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(i -> i.getArgument(0));
            when(recordingRepository.markOrphanedByCameraId(anyLong(), any(), anyLong())).thenReturn(1);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When
            cameraService.batchDeleteCameras(java.util.List.of(1L, 2L));

            // Then - 录像孤立处理应被调用2次
            verify(recordingRepository, times(2)).markOrphanedByCameraId(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("批量删除中单个失败不应影响其他摄像头")
        void testBatchDeleteContinuesOnSingleFailure() {
            // Given
            Camera camera1 = Camera.builder().id(1L).edgeNodeId(100L).build();
            Camera camera2 = Camera.builder().id(2L).edgeNodeId(100L).build();

            when(cameraRepository.findById(1L)).thenReturn(Optional.empty()); // 第一个失败
            when(cameraRepository.findById(2L)).thenReturn(Optional.of(camera2));
            when(cameraRepository.save(any(Camera.class))).thenAnswer(i -> i.getArgument(0));
            when(recordingRepository.markOrphanedByCameraId(eq(2L), any(), anyLong())).thenReturn(1);
            when(edgeNodeRepository.findById(100L)).thenReturn(Optional.of(testEdgeNode));
            when(edgeNodeRepository.save(any(EdgeNode.class))).thenReturn(testEdgeNode);

            // When
            assertDoesNotThrow(() -> cameraService.batchDeleteCameras(java.util.List.of(1L, 2L)));

            // Then - 第二个摄像头仍应被处理
            verify(recordingRepository).markOrphanedByCameraId(eq(2L), any(), anyLong());
        }
    }
}
