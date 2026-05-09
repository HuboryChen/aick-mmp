package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.ConnectivityResultDTO;
import com.aick.mmp.central.dto.DeviceIdentifyDTO;
import com.aick.mmp.central.dto.DiscoveryTaskDTO;
import com.aick.mmp.central.dto.ScanProgressDTO;
import com.aick.mmp.central.service.CameraDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/camera-discovery")
@RequiredArgsConstructor
public class CameraDiscoveryController {

    private final CameraDiscoveryService discoveryService;

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Long>> startScan(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String networkSegment = request.get("networkSegment");
        if (networkSegment == null || networkSegment.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", -1L));
        }

        Long userId = getUserId(authentication);
        Long taskId = discoveryService.startScan(networkSegment, userId);
        return new ResponseEntity<>(Map.of("taskId", taskId), HttpStatus.CREATED);
    }

    @GetMapping("/scan/{taskId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ScanProgressDTO> getScanProgress(@PathVariable Long taskId) {
        return ResponseEntity.ok(discoveryService.getScanProgress(taskId));
    }

    @DeleteMapping("/scan/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> cancelScan(@PathVariable Long taskId) {
        discoveryService.cancelScan(taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-connectivity")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ConnectivityResultDTO> testConnectivity(
            @RequestBody Map<String, Object> request) {
        String ip = (String) request.get("ip");
        Integer port = request.get("port") instanceof Integer
                ? (Integer) request.get("port")
                : 554;
        return ResponseEntity.ok(discoveryService.testConnectivity(ip, port));
    }

    @PostMapping("/identify")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<DeviceIdentifyDTO> identifyDevice(
            @RequestBody Map<String, Object> request) {
        String ip = (String) request.get("ip");
        Integer port = request.get("port") instanceof Integer
                ? (Integer) request.get("port")
                : 554;
        return ResponseEntity.ok(discoveryService.identifyDevice(ip, port));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<DiscoveryTaskDTO>> getScanHistory(
            Pageable pageable,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(discoveryService.getScanHistory(pageable, userId));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.aick.mmp.central.security.CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return 0L;
    }
}
