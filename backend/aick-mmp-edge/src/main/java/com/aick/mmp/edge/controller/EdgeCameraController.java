package com.aick.mmp.edge.controller;

import com.aick.mmp.edge.dto.EdgeCameraDTO;
import com.aick.mmp.edge.dto.EdgeCameraStatusDTO;
import com.aick.mmp.edge.service.EdgeCameraService;
import com.aick.mmp.shared.model.Camera;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/edge/cameras")
@Tag(name = "Edge Camera Management", description = "Camera management operations on edge nodes")
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeCameraController {

    private final EdgeCameraService edgeCameraService;

    @GetMapping
    @Operation(summary = "Get all cameras on this edge node")
    public ResponseEntity<List<EdgeCameraDTO>> getAllCameras() {
        try {
            List<EdgeCameraDTO> cameras = edgeCameraService.getAllCameras();
            return ResponseEntity.ok(cameras);
        } catch (Exception e) {
            log.error("Error getting all cameras", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{cameraId}")
    @Operation(summary = "Get camera by ID")
    public ResponseEntity<EdgeCameraDTO> getCameraById(@PathVariable Long cameraId) {
        try {
            Optional<EdgeCameraDTO> camera = edgeCameraService.getCameraById(cameraId);
            return camera.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting camera by ID: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(summary = "Add a new camera to this edge node")
    public ResponseEntity<EdgeCameraDTO> addCamera(@RequestBody EdgeCameraDTO cameraDTO) {
        try {
            EdgeCameraDTO createdCamera = edgeCameraService.addCamera(cameraDTO);
            return ResponseEntity.ok(createdCamera);
        } catch (Exception e) {
            log.error("Error adding camera", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{cameraId}")
    @Operation(summary = "Update camera configuration")
    public ResponseEntity<EdgeCameraDTO> updateCamera(
            @PathVariable Long cameraId, 
            @RequestBody EdgeCameraDTO cameraDTO) {
        try {
            EdgeCameraDTO updatedCamera = edgeCameraService.updateCamera(cameraId, cameraDTO);
            return ResponseEntity.ok(updatedCamera);
        } catch (Exception e) {
            log.error("Error updating camera: {}", cameraId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{cameraId}")
    @Operation(summary = "Remove camera from this edge node")
    public ResponseEntity<Void> removeCamera(@PathVariable Long cameraId) {
        try {
            edgeCameraService.removeCamera(cameraId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error removing camera: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{cameraId}/test-connection")
    @Operation(summary = "Test camera connection")
    public ResponseEntity<Boolean> testCameraConnection(@PathVariable Long cameraId) {
        try {
            boolean connected = edgeCameraService.testCameraConnection(cameraId);
            return ResponseEntity.ok(connected);
        } catch (Exception e) {
            log.error("Error testing camera connection: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{cameraId}/status")
    @Operation(summary = "Get camera status with metrics")
    public ResponseEntity<EdgeCameraStatusDTO> getCameraStatus(@PathVariable Long cameraId) {
        try {
            EdgeCameraStatusDTO status = edgeCameraService.getCameraStatus(cameraId);
            return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting camera status: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{cameraId}/status")
    @Operation(summary = "Update camera status")
    public ResponseEntity<Void> updateCameraStatus(
            @PathVariable Long cameraId,
            @RequestParam Camera.CameraStatus status,
            @RequestParam(required = false) String errorMessage) {
        try {
            edgeCameraService.updateCameraStatus(cameraId, status, errorMessage);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating camera status: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get cameras by status")
    public ResponseEntity<List<EdgeCameraDTO>> getCamerasByStatus(@PathVariable Camera.CameraStatus status) {
        try {
            List<EdgeCameraDTO> cameras = edgeCameraService.getCamerasByStatus(status);
            return ResponseEntity.ok(cameras);
        } catch (Exception e) {
            log.error("Error getting cameras by status: {}", status, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/count/online")
    @Operation(summary = "Get online cameras count")
    public ResponseEntity<Long> getOnlineCamerasCount() {
        try {
            long count = edgeCameraService.getOnlineCamerasCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting online cameras count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statuses")
    @Operation(summary = "Get all camera statuses for reporting")
    public ResponseEntity<List<EdgeCameraStatusDTO>> getAllCameraStatuses() {
        try {
            List<EdgeCameraStatusDTO> statuses = edgeCameraService.getAllCameraStatuses();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("Error getting all camera statuses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialize cameras on edge node")
    public ResponseEntity<Void> initializeCameras() {
        try {
            edgeCameraService.initializeCameras();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error initializing cameras", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/shutdown")
    @Operation(summary = "Shutdown all camera connections")
    public ResponseEntity<Void> shutdownCameras() {
        try {
            edgeCameraService.shutdownCameras();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error shutting down cameras", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reconnect-failed")
    @Operation(summary = "Reconnect failed cameras")
    public ResponseEntity<Void> reconnectFailedCameras() {
        try {
            edgeCameraService.reconnectFailedCameras();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error reconnecting failed cameras", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{cameraId}/stream-url")
    @Operation(summary = "Get camera stream URL")
    public ResponseEntity<String> getCameraStreamUrl(@PathVariable Long cameraId) {
        try {
            String streamUrl = edgeCameraService.getCameraStreamUrl(cameraId);
            return streamUrl != null ? ResponseEntity.ok(streamUrl) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting camera stream URL: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{cameraId}/credentials")
    @Operation(summary = "Update camera credentials")
    public ResponseEntity<Void> updateCameraCredentials(
            @PathVariable Long cameraId,
            @RequestParam String username,
            @RequestParam String password) {
        try {
            edgeCameraService.updateCameraCredentials(cameraId, username, password);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating camera credentials: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check for camera service")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Edge camera service is healthy");
    }
}