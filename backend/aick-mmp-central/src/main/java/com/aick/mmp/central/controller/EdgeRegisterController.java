package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.EdgeRegisterRequestDTO;
import com.aick.mmp.central.dto.EdgeRegisterResponseDTO;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.SystemAppService;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.SystemAppPermission;
import com.aick.mmp.shared.util.SignatureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for Edge node self-registration and authentication.
 * Uses SystemApp credentials for AK/SK authentication.
 */
@RestController
@RequestMapping("/edge")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Edge Registration", description = "Edge node self-registration with AK/SK authentication")
public class EdgeRegisterController {
    
    private final EdgeNodeRepository edgeNodeRepository;
    private final SystemAppService systemAppService;
    private final SignatureUtil signatureUtil;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new Edge node with AK/SK authentication")
    @Transactional
    public ResponseEntity<EdgeRegisterResponseDTO> register(
            @RequestHeader("X-Access-Key") String accessKey,
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp,
            @Valid @RequestBody EdgeRegisterRequestDTO request) {
        
        // Validate AK/SK authentication using SystemApp credentials
        String validationError = validateSystemAppAuthentication(accessKey, signature, timestamp, "POST", "/api/edge/register");
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(EdgeRegisterResponseDTO.builder()
                            .message(validationError)
                            .build());
        }
        
        // Get the SystemApp by app key
        SystemApp systemApp = systemAppService.getSystemAppByKey(accessKey).orElse(null);
        
        if (systemApp == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(EdgeRegisterResponseDTO.builder()
                            .message("Invalid system app")
                            .build());
        }
        
        // Check if name already exists - if so, update existing node
        EdgeNode existingNode = edgeNodeRepository.findByName(request.getName()).orElse(null);
        if (existingNode != null) {
            // Update existing node
            existingNode.setLocation(request.getLocation());
            existingNode.setIpAddress(request.getIpAddress());
            existingNode.setPort(request.getPort());
            existingNode.setMaxCameraSupport(request.getMaxCameraSupport());
            existingNode.setStatus(EdgeNode.NodeStatus.ONLINE);
            existingNode.setRegisteredAt(LocalDateTime.now());
            existingNode = edgeNodeRepository.save(existingNode);
            
            log.info("Updated existing edge node: {} ({})", existingNode.getName(), existingNode.getUuid());
            
            return ResponseEntity.ok()
                    .body(EdgeRegisterResponseDTO.builder()
                            .id(existingNode.getId())
                            .uuid(existingNode.getUuid())
                            .name(existingNode.getName())
                            .status(existingNode.getStatus().name())
                            .registeredAt(existingNode.getRegisteredAt())
                            .message("Edge node updated successfully")
                            .build());
        }
        
        // Check if IP and port already registered
        if (edgeNodeRepository.existsByIpAddressAndPort(request.getIpAddress(), request.getPort())) {
            return ResponseEntity.badRequest()
                    .body(EdgeRegisterResponseDTO.builder()
                            .message("Edge node with this IP and port already registered")
                            .build());
        }
        
        // Check if app has EDGE_REGISTER permission
        if (!systemApp.hasPermission(SystemAppPermission.EDGE_REGISTER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(EdgeRegisterResponseDTO.builder()
                            .message("System app does not have EDGE_REGISTER permission")
                            .build());
        }
        
        // Check if app is active
        if (systemApp.getStatus() != SystemApp.AppStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(EdgeRegisterResponseDTO.builder()
                            .message("System app is not active")
                            .build());
        }
        
        // Create Edge node
        EdgeNode edgeNode = EdgeNode.builder()
                .uuid(UUID.randomUUID().toString())
                .name(request.getName())
                .location(request.getLocation())
                .ipAddress(request.getIpAddress())
                .port(request.getPort())
                .maxCameraSupport(request.getMaxCameraSupport())
                .currentCameraCount(0)
                .status(EdgeNode.NodeStatus.ONLINE)
                .systemApp(systemApp)
                .registeredAt(LocalDateTime.now())
                .enabled(true)
                .build();
        
        edgeNode = edgeNodeRepository.save(edgeNode);
        
        log.info("Edge node registered: {} ({})", edgeNode.getName(), edgeNode.getUuid());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EdgeRegisterResponseDTO.builder()
                        .id(edgeNode.getId())
                        .uuid(edgeNode.getUuid())
                        .name(edgeNode.getName())
                        .status(edgeNode.getStatus().name())
                        .registeredAt(edgeNode.getRegisteredAt())
                        .message("Edge node registered successfully")
                        .build());
    }
    
    private String validateSystemAppAuthentication(String appKey, String signature, 
                                                  String timestamp, String method, String path) {
        // Validate app key format
        if (appKey == null || !appKey.startsWith("ak_")) {
            return "Invalid app key format";
        }
        
        // Validate timestamp
        if (!signatureUtil.isTimestampValid(timestamp)) {
            return "Timestamp expired or invalid";
        }
        
        // Get SystemApp from database
        SystemApp systemApp = systemAppService.getSystemAppByKey(appKey).orElse(null);
        if (systemApp == null) {
            return "Invalid app key";
        }
        
        // Check if app has credentials
        if (!systemApp.hasCredentials()) {
            return "System app has no credentials configured";
        }
        
        // Check app status
        if (systemApp.getStatus() != SystemApp.AppStatus.ACTIVE) {
            return "System app is not active";
        }
        
        // Get decrypted secret key
        String secretKey = systemAppService.getDecryptedAppSecretByKey(appKey).orElse(null);
        if (secretKey == null) {
            return "Failed to retrieve secret key";
        }
        
        // Verify signature
        String stringToSign = signatureUtil.buildStringToSign(method, path, timestamp);
        if (!signatureUtil.verifySignature(stringToSign, signature, secretKey)) {
            return "Invalid signature";
        }
        
        return null; // Success
    }
}
