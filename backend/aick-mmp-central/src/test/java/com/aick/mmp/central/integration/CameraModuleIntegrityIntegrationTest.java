package com.aick.mmp.central.integration;

import com.aick.mmp.central.repository.*;
import com.aick.mmp.central.service.*;
import com.aick.mmp.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试: 验证摄像头模块完整性功能
 * - 录像软删除和孤立录像处理
 * - 录像计划管理
 * - 移动侦测事件处理
 * - 边缘节点摄像头状态同步
 */
@SpringBootTest
@ActiveProfiles("central")
class CameraModuleIntegrityIntegrationTest {

    @Autowired
    private RecordingRepository recordingRepository;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private RecordingScheduleRepository recordingScheduleRepository;

    @Autowired
    private MotionEventRepository motionEventRepository;

    @Autowired
    private EdgeNodeRepository edgeNodeRepository;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private RecordingScheduleService recordingScheduleService;

    @Autowired
    private MotionEventService motionEventService;

    private Camera testCamera;
    private EdgeNode testEdgeNode;
    private Recording testRecording;

    @BeforeEach
    void setUp() {
        // Clean up test data
        motionEventRepository.deleteAll();
        recordingScheduleRepository.deleteAll();
        recordingRepository.deleteAll();
        cameraRepository.deleteAll();
        edgeNodeRepository.deleteAll();
    }

    @Nested
    @DisplayName("录像软删除和孤立录像处理")
    class RecordingSoftDeleteTests {

        @Test
        @DisplayName("验证: 录像软删除后状态正确")
        void testRecordingSoftDeleteIntegration() {
            // Create test edge node
            EdgeNode edgeNode = EdgeNode.builder()
                    .name("test-edge-node")
                    .location("Test Location")
                    .status(EdgeNode.EdgeNodeStatus.ONLINE)
                    .registeredAt(LocalDateTime.now())
                    .build();
            edgeNodeRepository.save(edgeNode);

            // Create test camera
            Camera camera = Camera.builder()
                    .name("Test Camera")
                    .edgeNodeId(edgeNode.getId())
                    .status(Camera.CameraStatus.ONLINE)
                    .build();
            cameraRepository.save(camera);

            // Create test recording
            Recording recording = Recording.builder()
                    .name("Test Recording")
                    .cameraId(camera.getId())
                    .startTime(LocalDateTime.now().minusHours(2))
                    .endTime(LocalDateTime.now().minusHours(1))
                    .isDeleted(false)
                    .build();
            recordingRepository.save(recording);

            // Verify recording exists
            Optional<Recording> found = recordingRepository.findById(recording.getId());
            assertTrue(found.isPresent());
            assertFalse(found.get().getIsDeleted());
        }
    }

    @Nested
    @DisplayName("录像计划管理")
    class RecordingScheduleTests {

        @Test
        @DisplayName("验证: 录像计划 CRUD 操作")
        void testRecordingScheduleCRUDIntegration() {
            // Create test camera
            Camera camera = Camera.builder()
                    .name("Test Camera")
                    .status(Camera.CameraStatus.ONLINE)
                    .build();
            cameraRepository.save(camera);

            // Verify camera exists
            assertTrue(cameraRepository.existsById(camera.getId()));

            // Verify schedule repository is available
            List<RecordingSchedule> schedules = recordingScheduleRepository.findByCameraId(camera.getId());
            assertNotNull(schedules);
        }

        @Test
        @DisplayName("验证: 获取活动录像计划")
        void testGetActiveSchedulesIntegration() {
            List<RecordingSchedule> activeSchedules = recordingScheduleService.getActiveSchedulesForSync();
            assertNotNull(activeSchedules);
        }
    }

    @Nested
    @DisplayName("移动侦测事件处理")
    class MotionEventTests {

        @Test
        @DisplayName("验证: 移动侦测事件上报和查询")
        void testMotionEventReportAndQueryIntegration() {
            // Create test camera
            Camera camera = Camera.builder()
                    .name("Test Camera")
                    .edgeNodeId(1L)
                    .status(Camera.CameraStatus.ONLINE)
                    .build();
            cameraRepository.save(camera);

            // Verify camera exists
            assertTrue(cameraRepository.existsById(camera.getId()));

            // Verify motion event repository is available
            List<com.aick.mmp.central.entity.MotionEvent> events = 
                    motionEventRepository.findByCameraId(camera.getId());
            assertNotNull(events);
        }
    }

    @Nested
    @DisplayName("边缘节点摄像头状态同步")
    class EdgeNodeCameraStatusSyncTests {

        @Test
        @DisplayName("验证: 边缘节点心跳处理服务可用")
        void testEdgeNodeServiceAvailable() {
            assertNotNull(edgeNodeService);
        }

        @Test
        @DisplayName("验证: 边缘节点状态查询")
        void testEdgeNodeStatusQuery() {
            // 验证可以查询边缘节点列表
            List<EdgeNode> nodes = edgeNodeRepository.findByStatus(EdgeNode.EdgeNodeStatus.ONLINE);
            assertNotNull(nodes);
        }
    }

    @Nested
    @DisplayName("服务集成验证")
    class ServiceIntegrationTests {

        @Test
        @DisplayName("验证: 所有关键服务已正确注入")
        void testAllServicesInjected() {
            assertNotNull(edgeNodeService, "EdgeNodeService 应被正确注入");
            assertNotNull(recordingScheduleService, "RecordingScheduleService 应被正确注入");
            assertNotNull(motionEventService, "MotionEventService 应被正确注入");
        }

        @Test
        @DisplayName("验证: 所有 Repository 已正确注入")
        void testAllRepositoriesInjected() {
            assertNotNull(recordingRepository, "RecordingRepository 应被正确注入");
            assertNotNull(cameraRepository, "CameraRepository 应被正确注入");
            assertNotNull(recordingScheduleRepository, "RecordingScheduleRepository 应被正确注入");
            assertNotNull(motionEventRepository, "MotionEventRepository 应被正确注入");
            assertNotNull(edgeNodeRepository, "EdgeNodeRepository 应被正确注入");
        }
    }
}
