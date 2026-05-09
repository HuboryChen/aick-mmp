package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.repository.UserRepository;
import com.aick.mmp.central.service.ApiKeyService;
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
import java.util.List;

/**
 * Controller for managing API keys (USER type only).
 * System-level credentials are managed via SystemAppController.
 */
@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API Keys", description = "API key management for AK/SK authentication (USER type)")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    
    // ==================== User-level API Keys ====================
    
    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create API key for current user")
    public ResponseEntity<ApiKeyCreatedResponseDTO> createApiKeyForUser(
            @Valid @RequestBody CreateApiKeyRequestDTO request) {
        
        Long userId = getCurrentUserId();
        
        ApiKeyCreatedResponseDTO response = apiKeyService.createApiKeyForUser(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List API keys for current user")
    public ResponseEntity<List<ApiKeyDTO>> listUserApiKeys() {
        Long userId = getCurrentUserId();
        
        List<ApiKeyDTO> keys = apiKeyService.listApiKeysByUser(userId);
        
        return ResponseEntity.ok(keys);
    }
    
    @PutMapping("/me/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update API key status for current user")
    public ResponseEntity<Void> updateUserApiKeyStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApiKeyStatusRequestDTO request) {
        
        Long userId = getCurrentUserId();
        
        // Verify ownership
        apiKeyService.getApiKeyById(id).ifPresent(key -> {
            if (!userId.equals(key.getUserId())) {
                throw new RuntimeException("API key does not belong to user");
            }
        });
        
        apiKeyService.updateKeyStatus(id, request.getStatus());
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/me/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete API key for current user")
    public ResponseEntity<Void> deleteUserApiKey(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        
        apiKeyService.deleteApiKeyForUser(id, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Helper Methods ====================
    
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
