package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.EdgeFailoverProperties;
import com.aick.mmp.central.repository.CameraFailoverEventRepository;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * EdgeNodeFailoverServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EdgeNodeFailoverServiceImplTest {

    @Mock
    private CameraRepository cameraRepository;

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @Mock
    private CameraFailoverEventRepository failoverEventRepository;

    @Mock
    private CameraServiceImpl cameraService; // 虽然当前未直接使用,保留注入兼容性

    @InjectMocks
    private EdgeNodeFailoverServiceImpl failoverService;

    private EdgeFailoverProperties failoverProperties;
    private EdgeNode sourceNode;
    private List<Camera> cameras;
    private EdgeNode targetNode1;
    private EdgeNode targetNode2;

    @BeforeEach
    void setUp() {
        // 初始化配置属性
        failoverProperties = new EdgeFailoverProperties();
        failoverProperties.setEnabled(true);
        failoverProperties.setMode(EdgeFailoverProperties.FailoverMode.SYNC); // 测试用同步模式
        failoverProperties.setDelaySeconds(0);
        failoverProperties.setMaxConcurrentTasks(3);
        failoverProperties.setBatchSize(20);
        failoverProperties.setBatchDelayMs(0L);
        failoverProperties.setRegionBonus(0.3);

        // 使用反射设置 final 字段 (因为 @InjectMocks 无法自动注入 @Value 配置类)
        try {
            var propsField = EdgeNodeFailoverServiceImpl.class.getDeclaredField("failoverProperties");
            propsField.setAccessible(true);
            propsField.set(failoverService, failoverProperties);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject properties", e);
        }

        // 创建源节点（离线节点）
        sourceNode = EdgeNode.builder()
                .id(100L).name("离线节点").location("华北区")
                .status(EdgeNode.NodeStatus.OFFLINE)
                .maxCameraSupport(50).currentCameraCount(10)
                .cpuUsage(0.0).memoryUsage(0.0).storageUsage(30.0)
                .build();

        // 创建目标节点1（同区域，健康）
        targetNode1 = EdgeNode.builder()
                .id(200L).name("目标节点-同区域").location("华北区")
                .status(EdgeNode.NodeStatus.ONLINE)
                .maxCameraSupport(50).currentCameraCount(20)
                .cpuUsage(30.0).memoryUsage(40.0).storageUsage(40.0)
                .build();

        // 创建目标节点2（不同区域，健康）
        targetNode2 = EdgeNode.builder()
                .id(300L).name("目标节点-跨区域").location("华南区")
                .status(EdgeNode.NodeStatus.ONLINE)
                .maxCameraSupport(50).currentCameraCount(15)
                .cpuUsage(25.0).memoryUsage(35.0).storageUsage(35.0)
                .build();

        // 创建源节点上的摄像头
        cameras = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            cameras.add(Camera.builder()
                    .id((long) i).name("摄像头-" + i).location("北京")
                    .edgeNodeId(sourceNode.getId())
                    .protocol(Camera.Protocol.RTSP)
                    .connectionUrl("rtsp://cam" + i + ".local")
                    .status(Camera.CameraStatus.ONLINE)
                    .enabled(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    // ==================== 场景1: 成功分配到同区域健康节点 ====================

    @Test
    @DisplayName("场景: 同区域节点优先 - 应选择同区域的节点作为目标")
    void testSelectTargetNodeForFailover_preferSameRegion() {
        // Given: 存在同区域和跨区域的在线节点
        when(edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE))
                .thenReturn(Arrays.asList(targetNode1, targetNode2));
        when(cameraRepository.countByEdgeNodeId(anyLong())).thenAnswer(invocation -> {
            Long nodeId = invocation.getArgument(0);
            if (nodeId.equals(targetNode1.getId())) return 20L;
            if (nodeId.equals(targetNode2.getId())) return 15L;
            return 0L;
        });

        // When
        Camera testCamera = cameras.get(0);
        Long selectedTarget = failoverService.selectTargetNodeForFailover(testCamera, sourceNode);

        // Then: 应该选择同区域的目标节点1（因为有region bonus加成）
        assertNotNull(selectedTarget);
        assertEquals(targetNode1.getId(), selectedTarget,
                "应优先选择与源节点同区域的目标节点");
    }

    // ==================== 场景2: 无可用节点时进入待分配池 ====================

    @Test
    @DisplayName("场景: 所有节点满载或离线 - 应返回null表示无目标")
    void testSelectTargetNodeForFailover_noAvailableNodes() {
        // Given: 没有在线节点
        when(edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE))
                .thenReturn(Collections.emptyList());

        // When
        Long selectedTarget = failoverService.selectTargetNodeForFailover(cameras.get(0), sourceNode);

        // Then: 返回 null
        assertNull(selectedTarget, "无可用节点时应返回null");
    }

    // ==================== 场景3: 避免重复迁移 ====================

    @Test
    @DisplayName("场景: 已被迁移的摄像头应被跳过")
    void testSkipAlreadyMigratedCameras() {
        // Given: 其中一个摄像头已经被迁移到其他节点
        cameras.get(0).setEdgeNodeId(999L); // 已经不在源节点上
        when(cameraRepository.findByEdgeNodeIdAndStatus(eq(sourceNode.getId()), eq(Camera.CameraStatus.ONLINE)))
                .thenReturn(cameras);
        when(failoverEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(edgeNodeRepository.findById(sourceNode.getId())).thenReturn(Optional.of(sourceNode));

        // When
        Long eventId = failoverService.doExecuteFailover(sourceNode.getId(),
                CameraFailoverEvent.FailoverTriggerType.AUTO, sourceNode);

        // Then: 只有4个摄像头参与迁移（第1个被跳过）
        assertNotNull(eventId);
        verify(failoverEventRepository).save(argThat(event ->
                event.getTotalCount() == 4)); // 排除已迁移的1个
    }

    // ==================== 场景4: 故障转移功能被禁用 ====================

    @Test
    @DisplayName("场景: 功能禁用时不触发故障转移")
    void testDisabledFeature_doesNotTrigger() {
        // Given: 禁用故障转移
        failoverProperties.setEnabled(false);

        // When
        Long result = failoverService.triggerFailover(sourceNode.getId(), CameraFailoverEvent.FailoverTriggerType.AUTO);

        // Then: 返回 null 且不执行任何数据库操作
        assertNull(result, "禁用状态下应返回null");
        verifyNoInteractions(cameraRepository);
        verifyNoInteractions(failoverEventRepository);
    }

    // ==================== 场景5: 待分配池处理 ====================

    @Test
    @DisplayName("场景: 处理待分配池中的摄像头")
    void testProcessPendingAllocationPool() {
        // Given: 有待分配的摄像头
        List<Camera> pendingCameras = new ArrayList<>();
        pendingCameras.add(Camera.builder().id(500L).name("待分配-1")
                .edgeNodeId(null).status(Camera.CameraStatus.PENDING_ALLOCATION).build());
        pendingCameras.add(Camera.builder().id(501L).name("待分配-2")
                .edgeNodeId(null).status(Camera.CameraStatus.PENDING_ALLOCATION).build());

        Page<Camera> pendingPage = new PageImpl<>(pendingCameras);
        when(cameraRepository.findByStatus(eq(Camera.CameraStatus.PENDING_ALLOCATION), any(Pageable.class)))
                .thenReturn(pendingPage);
        when(edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE))
                .thenReturn(Collections.singletonList(
                        EdgeNode.builder().id(400L).name("新上线节点")
                                .maxCameraSupport(100).currentCameraCount(10)
                                .status(EdgeNode.NodeStatus.ONLINE).build()
                ));
        when(cameraRepository.countByEdgeNodeId(anyLong())).thenReturn(10L);
        when(cameraRepository.save(any(Camera.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        int allocatedCount = failoverService.processPendingAllocationPool();

        // Then: 应成功分配2个摄像头
        assertEquals(2, allocatedCount, "应成功分配所有待分配摄像头");
        verify(cameraRepository, atLeast(2)).save(any(Camera.class));
    }
}
