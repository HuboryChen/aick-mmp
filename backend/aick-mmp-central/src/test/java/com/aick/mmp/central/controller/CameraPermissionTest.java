package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.BatchOperationDTO;
import com.aick.mmp.central.dto.BatchOperationDTO.BatchOperationType;
import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.RecordingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 单元测试: 验证 CameraController API 权限配置
 * 对应任务 5.1 - 修改 CameraController.search 的权限配置
 * 对应任务 5.2 - 添加 ADMIN、OPERATOR、VIEWER 角色支持
 * 对应任务 5.3 - 统一批量操作返回值格式
 * 对应任务 5.4 - 添加权限修改集成测试
 * 
 * 由于集成测试存在Jackson Scala依赖问题，使用反射方式验证权限注解
 */
@ExtendWith(MockitoExtension.class)
class CameraPermissionTest {

    @Mock
    private CameraService cameraService;

    @Mock
    private RecordingService recordingService;

    @Mock
    private RecordingRepository recordingRepository;

    private CameraController cameraController;
    private CameraDTO testCamera;

    @BeforeEach
    void setUp() {
        cameraController = new CameraController(cameraService, recordingService, recordingRepository);
        
        testCamera = new CameraDTO();
        testCamera.setId(1L);
        testCamera.setName("测试摄像头");
        testCamera.setEnabled(true);
    }

    @Nested
    @DisplayName("批量操作权限验证")
    class BatchOperationPermissionTests {

        @Test
        @DisplayName("batchOperation方法必须有ADMIN角色权限")
        void testBatchOperation_hasAdminRole() throws NoSuchMethodException {
            Method method = CameraController.class.getMethod(
                "batchCameraOperation", 
                BatchOperationDTO.class
            );
            
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("ADMIN");
        }

        @Test
        @DisplayName("batchUpdateEdgeNode方法必须有ADMIN角色权限")
        void testBatchUpdateEdgeNode_hasAdminRole() throws NoSuchMethodException {
            Method method = CameraController.class.getMethod(
                "batchUpdateEdgeNode", 
                Map.class
            );
            
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("ADMIN");
        }

        @Test
        @DisplayName("batchOperation方法调用batchDeleteCameras")
        void testBatchOperation_callsBatchDelete() {
            BatchOperationDTO dto = new BatchOperationDTO();
            dto.setOperation(BatchOperationType.DELETE);
            dto.setCameraIds(List.of(1L));

            cameraController.batchCameraOperation(dto);

            verify(cameraService).batchDeleteCameras(List.of(1L));
        }

        @Test
        @DisplayName("batchOperation方法返回正确格式")
        void testBatchOperation_returnsCorrectFormat() {
            BatchOperationDTO dto = new BatchOperationDTO();
            dto.setOperation(BatchOperationType.DELETE);
            dto.setCameraIds(List.of(1L));

            ResponseEntity<Map<String, Object>> response = cameraController.batchCameraOperation(dto);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsKeys("success", "processedCount", "failedCount", "processedIds", "failedIds");
            assertThat(response.getBody().get("processedCount")).isEqualTo(1);
            assertThat(response.getBody().get("failedCount")).isEqualTo(0);
        }

        @Test
        @DisplayName("batchUpdateEdgeNode方法调用正确服务方法")
        void testBatchUpdateEdgeNode_callsCorrectService() {
            Map<String, Object> request = Map.of(
                "cameraIds", List.of(1L),
                "edgeNodeId", 2L
            );

            cameraController.batchUpdateEdgeNode(request);

            verify(cameraService).batchUpdateEdgeNode(List.of(1L), 2L);
        }

        @Test
        @DisplayName("batchUpdateEdgeNode方法返回正确格式")
        void testBatchUpdateEdgeNode_returnsCorrectFormat() {
            Map<String, Object> request = Map.of(
                "cameraIds", List.of(1L),
                "edgeNodeId", 2L
            );

            ResponseEntity<Map<String, Object>> response = cameraController.batchUpdateEdgeNode(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsKeys("success", "processedCount", "failedCount");
        }
    }

    @Nested
    @DisplayName("单个操作权限验证")
    class SingleOperationPermissionTests {

        @Test
        @DisplayName("deleteCamera方法必须有ADMIN角色权限")
        void testDeleteCamera_hasAdminRole() throws NoSuchMethodException {
            Method method = CameraController.class.getMethod("deleteCamera", Long.class);
            
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("ADMIN");
        }

        @Test
        @DisplayName("deleteCamera方法调用正确服务方法")
        void testDeleteCamera_callsCorrectService() {
            cameraController.deleteCamera(1L);

            verify(cameraService).deleteCamera(1L);
        }

        @Test
        @DisplayName("updateCamera方法调用正确服务方法")
        void testUpdateCamera_callsCorrectService() {
            CameraDTO dto = new CameraDTO();
            dto.setName("更新后的摄像头");

            cameraController.updateCamera(1L, dto);

            verify(cameraService).updateCamera(eq(1L), any(CameraDTO.class));
        }

        @Test
        @DisplayName("updateCameraStatus方法调用正确服务方法")
        void testUpdateCameraStatus_callsCorrectService() {
            CameraStatusUpdateDTO dto = new CameraStatusUpdateDTO();
            dto.setStatus("ONLINE");

            cameraController.updateCameraStatus(1L, dto);

            verify(cameraService).updateCameraStatus(eq(1L), any(CameraStatusUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("查询操作权限验证")
    class QueryPermissionTests {

        @Test
        @DisplayName("getCameraById方法允许ADMIN和OPERATOR访问")
        void testGetCameraById_annotation() throws NoSuchMethodException {
            Method method = CameraController.class.getMethod("getCameraById", Long.class);
            
            // getCameraById允许ADMIN和OPERATOR访问
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("ADMIN");
            assertThat(preAuthorize.value()).contains("OPERATOR");
        }

        @Test
        @DisplayName("getCameraById方法调用正确服务方法")
        void testGetCameraById_callsCorrectService() {
            when(cameraService.getCameraById(1L)).thenReturn(testCamera);

            cameraController.getCameraById(1L);

            verify(cameraService).getCameraById(1L);
        }

        @Test
        @DisplayName("getDeletedCameras方法调用正确服务方法")
        void testGetDeletedCameras_callsCorrectService() {
            when(cameraService.getDeletedCameras()).thenReturn(List.of());

            cameraController.getDeletedCameras();

            verify(cameraService).getDeletedCameras();
        }
    }

    @Nested
    @DisplayName("其他操作验证")
    class OtherOperationTests {

        @Test
        @DisplayName("createCamera方法存在且可调用")
        void testCreateCamera_callsCorrectService() {
            CameraDTO dto = new CameraDTO();
            dto.setName("新摄像头");

            cameraController.createCamera(dto);

            verify(cameraService).createCamera(any(CameraDTO.class));
        }

        @Test
        @DisplayName("restoreCamera方法存在且可调用")
        void testRestoreCamera_callsCorrectService() {
            when(cameraService.restoreCamera(1L)).thenReturn(testCamera);

            cameraController.restoreCamera(1L);

            verify(cameraService).restoreCamera(1L);
        }

        @Test
        @DisplayName("getDeletedCameras方法存在且可调用")
        void testGetDeletedCameras_callsCorrectService() {
            when(cameraService.getDeletedCameras()).thenReturn(List.of());

            cameraController.getDeletedCameras();

            verify(cameraService).getDeletedCameras();
        }
    }
}
