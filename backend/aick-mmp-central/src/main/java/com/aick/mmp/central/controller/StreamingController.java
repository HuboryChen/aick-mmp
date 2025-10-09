package com.aick.mmp.central.controller;

import com.aick.mmp.central.service.StreamingService;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/streaming")
public class StreamingController {

    private final StreamingService streamingService;
    private final CameraService cameraService;

    @Autowired
    public StreamingController(StreamingService streamingService, CameraService cameraService) {
        this.streamingService = streamingService;
        this.cameraService = cameraService;
    }

    @PostMapping("/{cameraId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<?> startStream(@PathVariable Long cameraId) {
        try {
            String sessionId = cameraService.startCameraStream(cameraId);
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{cameraId}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<?> stopStream(@PathVariable Long cameraId) {
        try {
            cameraService.stopCameraStream(cameraId);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{cameraId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getStreamStatus(@PathVariable Long cameraId) {
        try {
            return ResponseEntity.ok(streamingService.getStreamStatus(cameraId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}