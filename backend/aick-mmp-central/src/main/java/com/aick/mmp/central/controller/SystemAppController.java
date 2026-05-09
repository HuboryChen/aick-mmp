package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CreateSystemAppRequestDTO;
import com.aick.mmp.central.dto.SystemAppCredentialsResponseDTO;
import com.aick.mmp.central.dto.SystemAppDTO;
import com.aick.mmp.central.dto.UpdateSystemAppRequestDTO;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.UserRepository;
import com.aick.mmp.central.service.SystemAppService;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing system applications.
 */
@RestController
@RequestMapping("/system-apps")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Apps", description = "System application management with integrated credentials")
public class SystemAppController {
    
    private final SystemAppService systemAppService;
    private final EdgeNodeRepository edgeNodeRepository;
    private final UserRepository userRepository;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new system application with credentials")
    public ResponseEntity<SystemAppCredentialsResponseDTO> createSystemApp(@Valid @RequestBody CreateSystemAppRequestDTO request) {
        Long createdBy = getCurrentUserId();
        
        SystemAppCredentialsResponseDTO credentials = systemAppService.createSystemAppWithCredentials(
                request.getName(),
                request.getDescription(),
                request.getPermissions(),
                request.getOwnerType() != null ? request.getOwnerType() : "SYSTEM",
                request.getOwnerId(),
                createdBy
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(credentials);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List system applications")
    public ResponseEntity<Page<SystemAppDTO>> listSystemApps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String status) {
        
        Page<SystemApp> appPage = systemAppService.listSystemApps(page, size, ownerType, status);
        
        Page<SystemAppDTO> dtoPage = appPage.map(this::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system application details")
    public ResponseEntity<SystemAppDTO> getSystemApp(@PathVariable Long id) {
        return systemAppService.getSystemApp(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update system application")
    public ResponseEntity<SystemAppDTO> updateSystemApp(
            @PathVariable Long id,
            @RequestBody UpdateSystemAppRequestDTO request) {
        
        SystemApp app = systemAppService.updateSystemApp(
                id,
                request.getName(),
                request.getDescription(),
                request.getPermissions(),
                request.getStatus()
        );
        
        return ResponseEntity.ok(toDTO(app));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete system application")
    public ResponseEntity<Void> deleteSystemApp(@PathVariable Long id) {
        try {
            systemAppService.deleteSystemApp(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete system app: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch delete system applications")
    public ResponseEntity<?> batchDeleteSystemApps(@RequestBody List<Long> ids) {
        try {
            log.info("Batch delete system apps: {}", ids);
            for (Long id : ids) {
                systemAppService.deleteSystemApp(id);
            }
            return ResponseEntity.ok(Map.of("message", "批量删除成功", "count", ids.size()));
        } catch (RuntimeException e) {
            log.error("Failed to batch delete system apps: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    // ==================== Credential Management ====================
    
    @GetMapping("/{id}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get credentials for a system app (decrypted secret)")
    public ResponseEntity<?> getCredentials(@PathVariable Long id) {
        return systemAppService.getDecryptedAppSecret(id)
                .map(secret -> {
                    SystemApp app = systemAppService.getSystemApp(id).orElse(null);
                    if (app != null) {
                        return ResponseEntity.ok(Map.of(
                                "appKey", app.getAppKey(),
                                "appSecret", secret,
                                "hasCredentials", app.hasCredentials()
                        ));
                    }
                    return ResponseEntity.notFound().<Map<String, Object>>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/credentials/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate credentials for a system app")
    public ResponseEntity<SystemAppCredentialsResponseDTO> regenerateCredentials(@PathVariable Long id) {
        try {
            SystemAppCredentialsResponseDTO credentials = systemAppService.regenerateCredentials(id);
            return ResponseEntity.ok(credentials);
        } catch (RuntimeException e) {
            log.error("Failed to regenerate credentials: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // ==================== Helper Methods ====================
    
    private SystemAppDTO toDTO(SystemApp app) {
        return SystemAppDTO.builder()
                .id(app.getId())
                .appKey(app.getAppKey())
                .name(app.getName())
                .description(app.getDescription())
                .ownerType(app.getOwnerType())
                .ownerId(app.getOwnerId())
                .status(app.getStatus().name())
                .permissions(app.getPermissions())
                .createdBy(app.getCreatedBy())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .hasCredentials(app.hasCredentials())
                .edgeNodeCount(countEdgeNodesForApp(app.getId()))
                .build();
    }
    
    private int countEdgeNodesForApp(Long appId) {
        return (int) edgeNodeRepository.findAll().stream()
                .filter(node -> node.getSystemApp() != null && node.getSystemApp().getId().equals(appId))
                .count();
    }
    
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}
