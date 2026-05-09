package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ConnectivityResultDTO;
import com.aick.mmp.central.dto.DeviceIdentifyDTO;
import com.aick.mmp.central.dto.DiscoveryTaskDTO;
import com.aick.mmp.central.dto.ScanProgressDTO;
import com.aick.mmp.central.repository.CameraDiscoveryTaskRepository;
import com.aick.mmp.central.service.impl.CameraDiscoveryServiceImpl;
import com.aick.mmp.shared.model.CameraDiscoveryTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CameraDiscoveryService Tests")
class CameraDiscoveryServiceTest {

    @Mock
    private CameraDiscoveryTaskRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private CameraDiscoveryServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CameraDiscoveryTask runningTask;

    @BeforeEach
    void setUp() {
        service = new CameraDiscoveryServiceImpl(repository, messagingTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "scanBatchSize", 50);
        ReflectionTestUtils.setField(service, "scanTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "commonPortsStr", "554,80,8080,8554");

        runningTask = CameraDiscoveryTask.builder()
                .id(1L)
                .userId(1L)
                .networkSegment("192.168.1.0/24")
                .status("RUNNING")
                .progress(50)
                .totalIps(254)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    @Nested
    @DisplayName("startScan")
    class StartScanTests {

        @Test
        @DisplayName("创建扫描任务并返回ID")
        void startScan_createsTask() {
            when(repository.save(any(CameraDiscoveryTask.class)))
                    .thenAnswer(invocation -> {
                        CameraDiscoveryTask t = invocation.getArgument(0);
                        t.setId(1L);
                        return t;
                    });

            Long taskId = service.startScan("192.168.1.0/24", 1L);

            assertEquals(1L, taskId);
            verify(repository).save(argThat(t ->
                    "PENDING".equals(t.getStatus()) &&
                    "192.168.1.0/24".equals(t.getNetworkSegment()) &&
                    t.getUserId() == 1L));
        }
    }

    @Nested
    @DisplayName("getScanProgress")
    class GetScanProgressTests {

        @Test
        @DisplayName("获取扫描进度")
        void getProgress() {
            when(repository.findById(1L)).thenReturn(Optional.of(runningTask));

            ScanProgressDTO progress = service.getScanProgress(1L);

            assertEquals(1L, progress.getTaskId());
            assertEquals(50, progress.getProgress());
            assertEquals(254, progress.getTotalIps());
        }

        @Test
        @DisplayName("任务不存在时抛出异常")
        void taskNotFound_throwsException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.getScanProgress(999L));
        }
    }

    @Nested
    @DisplayName("cancelScan")
    class CancelScanTests {

        @Test
        @DisplayName("取消运行中的扫描任务")
        void cancelRunningTask() {
            when(repository.findById(1L)).thenReturn(Optional.of(runningTask));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.cancelScan(1L);

            assertEquals("CANCELLED", runningTask.getStatus());
            assertNotNull(runningTask.getCompletedAt());
            verify(messagingTemplate).convertAndSend(anyString(), anyString());
        }

        @Test
        @DisplayName("取消已完成的任务抛出异常")
        void cancelCompletedTask_throwsException() {
            runningTask.setStatus("COMPLETED");
            when(repository.findById(1L)).thenReturn(Optional.of(runningTask));

            assertThrows(IllegalStateException.class,
                    () -> service.cancelScan(1L));
        }
    }

    @Nested
    @DisplayName("testConnectivity")
    class TestConnectivityTests {

        @Test
        @DisplayName("连接不可达IP返回失败")
        void unreachableIp_returnsFailed() {
            ConnectivityResultDTO result = service.testConnectivity("10.255.255.1", 554);

            assertFalse(result.isConnected());
            assertEquals("10.255.255.1", result.getIp());
            assertEquals(554, result.getPort());
        }
    }

    @Nested
    @DisplayName("identifyDevice")
    class IdentifyDeviceTests {

        @Test
        @DisplayName("未知IP返回未识别")
        void unknownDevice_returnsUnknown() {
            DeviceIdentifyDTO result = service.identifyDevice("10.255.255.1", 554);

            assertFalse(result.isIdentified());
            assertEquals("Unknown", result.getBrand());
        }

        @Test
        @DisplayName("HTTP端口设备识别")
        void httpPort_identify() {
            DeviceIdentifyDTO result = service.identifyDevice("10.255.255.1", 80);

            assertFalse(result.isIdentified());
            assertEquals("HTTP", result.getProtocol());
        }
    }

    @Nested
    @DisplayName("getScanHistory")
    class GetScanHistoryTests {

        @Test
        @DisplayName("获取用户扫描历史")
        void getUserScanHistory() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(repository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(runningTask), pageable, 1));

            Page<DiscoveryTaskDTO> result = service.getScanHistory(pageable, 1L);

            assertEquals(1, result.getTotalElements());
            assertEquals("192.168.1.0/24", result.getContent().get(0).getNetworkSegment());
        }

        @Test
        @DisplayName("无扫描历史时返回空")
        void noHistory_returnsEmpty() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(repository.findByUserIdOrderByCreatedAtDesc(2L, pageable))
                    .thenReturn(Page.empty());

            Page<DiscoveryTaskDTO> result = service.getScanHistory(pageable, 2L);

            assertEquals(0, result.getTotalElements());
        }
    }
}
