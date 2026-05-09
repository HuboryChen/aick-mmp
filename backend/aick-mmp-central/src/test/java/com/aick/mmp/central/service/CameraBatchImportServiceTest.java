package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraImportDTO;
import com.aick.mmp.central.dto.ImportProgressDTO;
import com.aick.mmp.central.dto.ValidationErrorDTO;
import com.aick.mmp.central.repository.CameraBatchImportTaskRepository;
import com.aick.mmp.central.service.impl.CameraBatchImportServiceImpl;
import com.aick.mmp.shared.model.CameraBatchImportTask;
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
@DisplayName("CameraBatchImportService Tests")
class CameraBatchImportServiceTest {

    @Mock
    private CameraBatchImportTaskRepository repository;

    @Mock
    private CameraService cameraService;

    @Mock
    private CameraConfigTemplateService templateService;

    @Mock
    private RegionService regionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private CameraBatchImportServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CameraBatchImportTask completedTask;

    @BeforeEach
    void setUp() {
        service = new CameraBatchImportServiceImpl(
                repository, cameraService, templateService,
                regionService, messagingTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "batchSize", 50);
        ReflectionTestUtils.setField(service, "batchDelayMs", 100);

        completedTask = CameraBatchImportTask.builder()
                .id(1L)
                .userId(1L)
                .fileName("test.xlsx")
                .status("COMPLETED")
                .totalRecords(100)
                .successCount(95)
                .failCount(5)
                .startedAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Nested
    @DisplayName("getImportTemplate")
    class GetImportTemplateTests {

        @Test
        @DisplayName("生成Excel导入模板")
        void generateExcelTemplate() {
            byte[] data = service.getImportTemplate();

            assertNotNull(data);
            assertTrue(data.length > 0, "Template file should not be empty");
        }
    }

    @Nested
    @DisplayName("getImportProgress")
    class GetImportProgressTests {

        @Test
        @DisplayName("获取导入进度")
        void getProgress() {
            when(repository.findById(1L)).thenReturn(Optional.of(completedTask));

            ImportProgressDTO progress = service.getImportProgress(1L);

            assertEquals("COMPLETED", progress.getStatus());
            assertEquals(100, progress.getTotalRecords());
            assertEquals(95, progress.getSuccessCount());
            assertEquals(5, progress.getFailCount());
        }

        @Test
        @DisplayName("任务不存在时抛出异常")
        void taskNotFound_throwsException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.getImportProgress(999L));
        }
    }

    @Nested
    @DisplayName("cancelImport")
    class CancelImportTests {

        @Test
        @DisplayName("取消验证中的导入任务")
        void cancelValidatingTask() {
            CameraBatchImportTask validatingTask = CameraBatchImportTask.builder()
                    .id(2L).status("VALIDATING").build();
            when(repository.findById(2L)).thenReturn(Optional.of(validatingTask));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.cancelImport(2L);

            assertEquals("CANCELLED", validatingTask.getStatus());
        }

        @Test
        @DisplayName("取消已完成的任务抛出异常")
        void cancelCompletedTask_throwsException() {
            CameraBatchImportTask task = CameraBatchImportTask.builder()
                    .id(3L).status("COMPLETED").build();
            when(repository.findById(3L)).thenReturn(Optional.of(task));

            assertThrows(IllegalStateException.class,
                    () -> service.cancelImport(3L));
        }
    }

    @Nested
    @DisplayName("validateImportData")
    class ValidateImportDataTests {

        @Test
        @DisplayName("有效数据通过验证")
        void validData_passesValidation() {
            CameraImportDTO validDto = CameraImportDTO.builder()
                    .cameraName("测试摄像头")
                    .brand("海康威视")
                    .model("DS-2CD2T45D-I5")
                    .ip("192.168.1.100")
                    .regionName("总部")
                    .build();

            List<ValidationErrorDTO> errors = service.validateImportData(List.of(validDto));

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("缺少必填字段报错")
        void missingRequiredFields_returnsErrors() {
            CameraImportDTO invalidDto = CameraImportDTO.builder()
                    .cameraName("")
                    .brand("")
                    .model("")
                    .ip("invalid-ip")
                    .regionName("")
                    .build();

            List<ValidationErrorDTO> errors = service.validateImportData(List.of(invalidDto));

            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.getFieldName().contains("摄像头名称")));
            assertTrue(errors.stream().anyMatch(e -> e.getFieldName().contains("IP地址")));
        }

        @Test
        @DisplayName("IP格式错误报错")
        void invalidIp_returnsError() {
            CameraImportDTO dto = CameraImportDTO.builder()
                    .cameraName("Test")
                    .brand("Brand")
                    .model("Model")
                    .ip("999.999.999.999")
                    .regionName("Region")
                    .build();

            List<ValidationErrorDTO> errors = service.validateImportData(List.of(dto));

            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.getFieldName().equals("IP地址")));
        }

        @Test
        @DisplayName("摄像头名称过短报错")
        void shortName_returnsError() {
            CameraImportDTO dto = CameraImportDTO.builder()
                    .cameraName("A")
                    .brand("Brand")
                    .model("Model")
                    .ip("192.168.1.1")
                    .regionName("Region")
                    .build();

            List<ValidationErrorDTO> errors = service.validateImportData(List.of(dto));

            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("getImportHistory")
    class GetImportHistoryTests {

        @Test
        @DisplayName("获取用户导入历史")
        void getUserImportHistory() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(repository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(completedTask), pageable, 1));

            var result = service.getImportHistory(pageable, 1L);

            assertEquals(1, result.getTotalElements());
            assertEquals("test.xlsx", result.getContent().get(0).getFileName());
        }

        @Test
        @DisplayName("无导入历史时返回空")
        void noHistory_returnsEmpty() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(repository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                    .thenReturn(Page.empty());

            var result = service.getImportHistory(pageable, 1L);

            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("downloadErrorReport")
    class DownloadErrorReportTests {

        @Test
        @DisplayName("无错误的任务无法下载报告")
        void noErrors_throwsException() {
            CameraBatchImportTask noErrorTask = CameraBatchImportTask.builder()
                    .id(1L).status("COMPLETED")
                    .errorDetails(null).build();
            when(repository.findById(1L)).thenReturn(Optional.of(noErrorTask));

            assertThrows(IllegalStateException.class,
                    () -> service.downloadErrorReport(1L));
        }

        @Test
        @DisplayName("有错误时生成报告文件")
        void withErrors_generatesReport() {
            CameraBatchImportTask taskWithErrors = CameraBatchImportTask.builder()
                    .id(2L).status("COMPLETED")
                    .errorDetails("[{\"rowNumber\":1,\"fieldName\":\"IP地址\",\"errorMessage\":\"格式不正确\"}]")
                    .build();
            when(repository.findById(2L)).thenReturn(Optional.of(taskWithErrors));

            byte[] report = service.downloadErrorReport(2L);

            assertNotNull(report);
            assertTrue(report.length > 0);
        }
    }
}
