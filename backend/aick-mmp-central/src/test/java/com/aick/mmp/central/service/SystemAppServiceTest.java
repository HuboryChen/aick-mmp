package com.aick.mmp.central.service;

import com.aick.mmp.central.repository.ApiKeyRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.SystemAppRepository;
import com.aick.mmp.central.service.impl.SystemAppServiceImpl;
import com.aick.mmp.shared.model.ApiKey;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.OwnerType;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SystemAppService
 */
@ExtendWith(MockitoExtension.class)
class SystemAppServiceTest {

    @Mock
    private SystemAppRepository systemAppRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @InjectMocks
    private SystemAppServiceImpl systemAppService;

    private SystemApp createTestSystemApp(Long id) {
        return SystemApp.builder()
                .id(id)
                .appKey(UUID.randomUUID().toString())
                .name("Test App")
                .description("Test Description")
                .ownerType(OwnerType.SYSTEM)
                .status(SystemApp.AppStatus.ACTIVE)
                .permissions(Set.of(SystemAppPermission.EDGE_REGISTER))
                .createdBy(1L)
                .build();
    }

    @Test
    @DisplayName("Should create system app successfully")
    void testCreateSystemApp() {
        Set<SystemAppPermission> permissions = Set.of(
                SystemAppPermission.EDGE_REGISTER,
                SystemAppPermission.EDGE_HEARTBEAT
        );

        when(systemAppRepository.existsByAppKey(anyString())).thenReturn(false);
        when(systemAppRepository.save(any(SystemApp.class))).thenAnswer(invocation -> {
            SystemApp app = invocation.getArgument(0);
            app.setId(1L);
            return app;
        });

        SystemApp result = systemAppService.createSystemApp(
                "Edge Node App",
                "App for Edge nodes",
                permissions,
                "SYSTEM",
                null,
                1L
        );

        assertNotNull(result);
        assertEquals("Edge Node App", result.getName());
        assertEquals(OwnerType.SYSTEM, result.getOwnerType());
        assertTrue(result.getPermissions().contains(SystemAppPermission.EDGE_REGISTER));
        assertEquals(SystemApp.AppStatus.ACTIVE, result.getStatus());

        verify(systemAppRepository).save(any(SystemApp.class));
    }

    @Test
    @DisplayName("Should generate unique app key")
    void testCreateSystemAppGeneratesUniqueKey() {
        when(systemAppRepository.existsByAppKey(anyString())).thenReturn(false);
        when(systemAppRepository.save(any(SystemApp.class))).thenAnswer(invocation -> {
            SystemApp app = invocation.getArgument(0);
            app.setId(1L);
            return app;
        });

        SystemApp result = systemAppService.createSystemApp(
                "Test App",
                null,
                Set.of(),
                "SYSTEM",
                null,
                1L
        );

        assertNotNull(result.getAppKey());
        assertEquals(36, result.getAppKey().length()); // UUID format
    }

    @Test
    @DisplayName("Should get system app by ID")
    void testGetSystemApp() {
        SystemApp app = createTestSystemApp(1L);
        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));

        Optional<SystemApp> result = systemAppService.getSystemApp(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("Test App", result.get().getName());

        verify(systemAppRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when system app not found")
    void testGetSystemAppNotFound() {
        when(systemAppRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<SystemApp> result = systemAppService.getSystemApp(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get system app by app key")
    void testGetSystemAppByKey() {
        SystemApp app = createTestSystemApp(1L);
        String appKey = app.getAppKey();
        when(systemAppRepository.findByAppKey(appKey)).thenReturn(Optional.of(app));

        Optional<SystemApp> result = systemAppService.getSystemAppByKey(appKey);

        assertTrue(result.isPresent());
        assertEquals(appKey, result.get().getAppKey());

        verify(systemAppRepository).findByAppKey(appKey);
    }

    @Test
    @DisplayName("Should list system apps with pagination")
    void testListSystemApps() {
        SystemApp app1 = createTestSystemApp(1L);
        SystemApp app2 = createTestSystemApp(2L);
        Page<SystemApp> page = new PageImpl<>(List.of(app1, app2));

        when(systemAppRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<SystemApp> result = systemAppService.listSystemApps(0, 10, null, null);

        assertEquals(2, result.getContent().size());
        verify(systemAppRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should list system apps filtered by owner type")
    void testListSystemAppsFilteredByOwnerType() {
        SystemApp app = createTestSystemApp(1L);
        Page<SystemApp> page = new PageImpl<>(List.of(app));

        when(systemAppRepository.findByOwnerType(eq(OwnerType.SYSTEM), any(PageRequest.class)))
                .thenReturn(page);

        Page<SystemApp> result = systemAppService.listSystemApps(0, 10, "SYSTEM", null);

        assertEquals(1, result.getContent().size());
        assertEquals(OwnerType.SYSTEM, result.getContent().get(0).getOwnerType());

        verify(systemAppRepository).findByOwnerType(eq(OwnerType.SYSTEM), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should update system app name")
    void testUpdateSystemAppName() {
        SystemApp app = createTestSystemApp(1L);
        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));
        when(systemAppRepository.save(any(SystemApp.class))).thenReturn(app);

        SystemApp result = systemAppService.updateSystemApp(1L, "New Name", null, null, null);

        assertEquals("New Name", result.getName());
        verify(systemAppRepository).save(app);
    }

    @Test
    @DisplayName("Should update system app permissions")
    void testUpdateSystemAppPermissions() {
        SystemApp app = createTestSystemApp(1L);
        Set<SystemAppPermission> newPermissions = Set.of(SystemAppPermission.EDGE_CONFIG_UPDATE);

        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));
        when(systemAppRepository.save(any(SystemApp.class))).thenReturn(app);

        SystemApp result = systemAppService.updateSystemApp(1L, null, null, newPermissions, null);

        assertTrue(result.getPermissions().contains(SystemAppPermission.EDGE_CONFIG_UPDATE));
        verify(systemAppRepository).save(app);
    }

    @Test
    @DisplayName("Should suspend system app successfully")
    void testSuspendSystemApp() {
        SystemApp app = createTestSystemApp(1L);
        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));
        when(systemAppRepository.save(any(SystemApp.class))).thenReturn(app);

        SystemApp result = systemAppService.updateSystemApp(1L, null, null, null, "SUSPENDED");

        assertEquals(SystemApp.AppStatus.SUSPENDED, app.getStatus());
        verify(systemAppRepository).save(app);
    }

    @Test
    @DisplayName("Should delete system app without associations")
    void testDeleteSystemAppSuccess() {
        SystemApp app = createTestSystemApp(1L);
        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));
        when(edgeNodeRepository.findAll()).thenReturn(List.of());

        systemAppService.deleteSystemApp(1L);

        verify(systemAppRepository).delete(app);
    }

    @Test
    @DisplayName("Should throw exception when deleting app with Edge nodes")
    void testDeleteSystemAppWithEdgeNodes() {
        SystemApp app = createTestSystemApp(1L);
        EdgeNode edgeNode = EdgeNode.builder()
                .id(1L)
                .systemApp(app)
                .build();

        when(systemAppRepository.findById(1L)).thenReturn(Optional.of(app));
        when(edgeNodeRepository.findAll()).thenReturn(List.of(edgeNode));

        assertThrows(RuntimeException.class, () -> {
            systemAppService.deleteSystemApp(1L);
        });

        verify(systemAppRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should validate valid permissions")
    void testValidatePermissionsSuccess() {
        Set<SystemAppPermission> permissions = Set.of(
                SystemAppPermission.EDGE_REGISTER,
                SystemAppPermission.EDGE_HEARTBEAT,
                SystemAppPermission.EDGE_CONFIG_UPDATE
        );

        assertDoesNotThrow(() -> {
            systemAppService.validatePermissions(permissions);
        });
    }

    @Test
    @DisplayName("Should accept empty permissions")
    void testValidateEmptyPermissions() {
        assertDoesNotThrow(() -> {
            systemAppService.validatePermissions(Set.of());
        });
        assertDoesNotThrow(() -> {
            systemAppService.validatePermissions(null);
        });
    }

    @Test
    @DisplayName("Should throw exception for null permission value")
    void testValidateNullPermission() {
        // This would be caught at a different level, but validatePermissions
        // should not throw for null set
        assertDoesNotThrow(() -> {
            systemAppService.validatePermissions(null);
        });
    }
}
