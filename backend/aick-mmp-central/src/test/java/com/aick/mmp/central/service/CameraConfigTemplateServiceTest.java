package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraConfigTemplateDTO;
import com.aick.mmp.central.dto.CreateTemplateRequestDTO;
import com.aick.mmp.central.dto.UpdateTemplateRequestDTO;
import com.aick.mmp.central.repository.CameraConfigTemplateRepository;
import com.aick.mmp.central.service.impl.CameraConfigTemplateServiceImpl;
import com.aick.mmp.shared.model.CameraConfigTemplate;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CameraConfigTemplateService Tests")
class CameraConfigTemplateServiceTest {

    @Mock
    private CameraConfigTemplateRepository repository;

    private CameraConfigTemplateServiceImpl service;

    private CameraConfigTemplate hikTemplate;

    @BeforeEach
    void setUp() {
        service = new CameraConfigTemplateServiceImpl(repository);

        hikTemplate = CameraConfigTemplate.builder()
                .id(1L)
                .brand("海康威视")
                .model("DS-2CD2T45D-I5")
                .protocol("RTSP")
                .defaultPort(554)
                .urlPathTemplate("rtsp://{username}:{password}@{ip}:{port}/Streaming/Channels/{channel}01")
                .presetParameters("{\"channel\":\"1\"}")
                .isPreset(true)
                .usageCount(5)
                .lastUsedAt(LocalDateTime.now().minusDays(1))
                .isDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getTemplates")
    class GetTemplatesTests {

        @Test
        @DisplayName("分页查询所有未删除模板")
        void findAllActiveTemplates() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByIsDeletedFalse(pageable))
                    .thenReturn(new PageImpl<>(List.of(hikTemplate), pageable, 1));

            Page<CameraConfigTemplateDTO> result = service.getTemplates(pageable, null, null);

            assertEquals(1, result.getTotalElements());
            assertEquals("海康威视", result.getContent().get(0).getBrand());
            verify(repository).findByIsDeletedFalse(pageable);
        }

        @Test
        @DisplayName("按品牌过滤模板")
        void filterByBrand() {
            when(repository.findByBrandAndIsDeletedFalse("海康威视"))
                    .thenReturn(List.of(hikTemplate));

            Page<CameraConfigTemplateDTO> result = service.getTemplates(
                    PageRequest.of(0, 10), "海康威视", null);

            assertEquals(1, result.getTotalElements());
            verify(repository).findByBrandAndIsDeletedFalse("海康威视");
        }

        @Test
        @DisplayName("品牌不存在时返回空列表")
        void brandNotFound_returnsEmpty() {
            when(repository.findByBrandAndIsDeletedFalse("Unknown"))
                    .thenReturn(List.of());

            Page<CameraConfigTemplateDTO> result = service.getTemplates(
                    PageRequest.of(0, 10), "Unknown", null);

            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("createTemplate")
    class CreateTemplateTests {

        @Test
        @DisplayName("创建普通模板成功")
        void createCustomTemplate() {
            CreateTemplateRequestDTO request = CreateTemplateRequestDTO.builder()
                    .brand("测试品牌")
                    .model("Test-Model-1")
                    .protocol("RTSP")
                    .defaultPort(554)
                    .urlPathTemplate("rtsp://{username}:{password}@{ip}:{port}/test")
                    .build();

            when(repository.save(any(CameraConfigTemplate.class)))
                    .thenAnswer(invocation -> {
                        CameraConfigTemplate t = invocation.getArgument(0);
                        return CameraConfigTemplate.builder()
                                .id(99L)
                                .brand(t.getBrand())
                                .model(t.getModel())
                                .protocol(t.getProtocol())
                                .defaultPort(t.getDefaultPort())
                                .urlPathTemplate(t.getUrlPathTemplate())
                                .isPreset(false)
                                .usageCount(0)
                                .isDeleted(false)
                                .build();
                    });

            CameraConfigTemplateDTO result = service.createTemplate(request);

            assertEquals("测试品牌", result.getBrand());
            assertEquals("Test-Model-1", result.getModel());
            assertFalse(result.getIsPreset());
            assertEquals(0, result.getUsageCount());
            verify(repository).save(any());
        }

        @Test
        @DisplayName("创建模板时设置默认值")
        void createTemplate_setsDefaults() {
            CreateTemplateRequestDTO request = CreateTemplateRequestDTO.builder()
                    .brand("大华")
                    .model("DH-IPC-Test")
                    .protocol("RTSP")
                    .defaultPort(554)
                    .urlPathTemplate("rtsp://{username}:{password}@{ip}:{port}/test")
                    .build();

            when(repository.save(any())).thenAnswer(invocation -> {
                CameraConfigTemplate t = invocation.getArgument(0);
                assertFalse(t.getIsPreset());
                assertEquals(0, t.getUsageCount());
                assertFalse(t.getIsDeleted());
                t.setId(2L);
                return t;
            });

            service.createTemplate(request);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("updateTemplate")
    class UpdateTemplateTests {

        @Test
        @DisplayName("更新模板部分字段")
        void updatePartialFields() {
            when(repository.findById(1L)).thenReturn(Optional.of(hikTemplate));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateTemplateRequestDTO request = UpdateTemplateRequestDTO.builder()
                    .brand("海康威视-更新")
                    .build();

            CameraConfigTemplateDTO result = service.updateTemplate(1L, request);

            assertEquals("海康威视-更新", result.getBrand());
            assertEquals("DS-2CD2T45D-I5", result.getModel());
        }

        @Test
        @DisplayName("更新不存在的模板抛出异常")
        void updateNonExistent_throwsException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.updateTemplate(999L, new UpdateTemplateRequestDTO()));
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class DeleteTemplateTests {

        @Test
        @DisplayName("软删除模板")
        void softDeleteTemplate() {
            when(repository.findById(1L)).thenReturn(Optional.of(hikTemplate));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.deleteTemplate(1L);

            assertTrue(hikTemplate.getIsDeleted());
            assertNotNull(hikTemplate.getDeletedAt());
            verify(repository).save(hikTemplate);
        }
    }

    @Nested
    @DisplayName("generateUrl")
    class GenerateUrlTests {

        @Test
        @DisplayName("生成完整URL - 使用提供的参数")
        void generateUrlWithAllParams() {
            when(repository.findById(1L)).thenReturn(Optional.of(hikTemplate));

            String url = service.generateUrl(1L, Map.of(
                    "ip", "192.168.1.100",
                    "port", "554",
                    "username", "admin",
                    "password", "pass123",
                    "channel", "1"
            ));

            assertEquals(
                    "rtsp://admin:pass123@192.168.1.100:554/Streaming/Channels/101",
                    url);
        }

        @Test
        @DisplayName("端口缺失时使用默认端口")
        void generateUrl_defaultPortWhenMissing() {
            when(repository.findById(1L)).thenReturn(Optional.of(hikTemplate));

            String url = service.generateUrl(1L, Map.of(
                    "ip", "192.168.1.100",
                    "username", "admin",
                    "password", "pass123"
            ));

            assertTrue(url.contains(":554/"));
        }
    }

    @Nested
    @DisplayName("matchTemplate")
    class MatchTemplateTests {

        @Test
        @DisplayName("精确匹配品牌和型号")
        void exactMatch() {
            when(repository.findByBrandAndModelAndIsDeletedFalse("海康威视", "DS-2CD2T45D-I5"))
                    .thenReturn(Optional.of(hikTemplate));

            CameraConfigTemplateDTO result = service.matchTemplate("海康威视", "DS-2CD2T45D-I5");

            assertNotNull(result);
            assertEquals("海康威视", result.getBrand());
        }

        @Test
        @DisplayName("品牌匹配但型号不匹配时返回品牌首个模板")
        void brandMatch_fallbackToFirst() {
            when(repository.findByBrandAndModelAndIsDeletedFalse("海康威视", "Unknown-Model"))
                    .thenReturn(Optional.empty());
            when(repository.findByBrandAndIsDeletedFalse("海康威视"))
                    .thenReturn(List.of(hikTemplate));

            CameraConfigTemplateDTO result = service.matchTemplate("海康威视", "Unknown-Model");

            assertNotNull(result);
            assertEquals("海康威视", result.getBrand());
        }

        @Test
        @DisplayName("无匹配模板返回null")
        void noMatch_returnsNull() {
            when(repository.findByBrandAndModelAndIsDeletedFalse("Unknown", "Unknown"))
                    .thenReturn(Optional.empty());
            when(repository.findByBrandAndIsDeletedFalse("Unknown"))
                    .thenReturn(List.of());

            assertNull(service.matchTemplate("Unknown", "Unknown"));
        }
    }

    @Nested
    @DisplayName("incrementUsage")
    class IncrementUsageTests {

        @Test
        @DisplayName("增加模板使用计数")
        void incrementUsageCount() {
            when(repository.findById(1L)).thenReturn(Optional.of(hikTemplate));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.incrementUsage(1L);

            assertEquals(6, hikTemplate.getUsageCount());
            assertNotNull(hikTemplate.getLastUsedAt());
        }
    }

    @Nested
    @DisplayName("importTemplates")
    class ImportTemplatesTests {

        @Test
        @DisplayName("批量导入模板")
        void batchImport() {
            CreateTemplateRequestDTO req1 = CreateTemplateRequestDTO.builder()
                    .brand("BrandA").model("ModelA").protocol("RTSP")
                    .defaultPort(554).urlPathTemplate("rtsp://{ip}").build();
            CreateTemplateRequestDTO req2 = CreateTemplateRequestDTO.builder()
                    .brand("BrandB").model("ModelB").protocol("HTTP")
                    .defaultPort(80).urlPathTemplate("http://{ip}").build();

            when(repository.saveAll(anyList())).thenAnswer(invocation -> {
                List<CameraConfigTemplate> list = invocation.getArgument(0);
                list.forEach(t -> t.setId(1L));
                return list;
            });

            List<CameraConfigTemplateDTO> result = service.importTemplates(List.of(req1, req2));

            assertEquals(2, result.size());
            assertEquals("BrandA", result.get(0).getBrand());
            assertEquals("BrandB", result.get(1).getBrand());
        }
    }

    @Nested
    @DisplayName("getBrands")
    class GetBrandsTests {

        @Test
        @DisplayName("获取所有品牌列表")
        void getAllBrands() {
            when(repository.findDistinctBrandByIsDeletedFalse())
                    .thenReturn(List.of("海康威视", "大华", "宇视"));

            List<String> brands = service.getBrands();

            assertEquals(3, brands.size());
            assertTrue(brands.contains("海康威视"));
        }
    }
}
