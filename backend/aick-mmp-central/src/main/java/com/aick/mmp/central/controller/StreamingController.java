package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.StreamQualityRequest;
import com.aick.mmp.central.dto.WebRtcRequest;
import com.aick.mmp.central.service.StreamingService;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.shared.exception.ResourceNotFoundException;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.StreamSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/streaming")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final StreamingService streamingService;
    private final CameraService cameraService;

    // ==================== 基础流管理 ====================

    @PostMapping("/{cameraId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> startStream(@PathVariable Long cameraId) {
        try {
            StreamSession session = streamingService.startStream(cameraId);
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("cameraId", session.getCameraId());
            response.put("status", session.getStatus());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to start stream for camera {}: {}", cameraId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stream start failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{cameraId}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> stopStream(@PathVariable Long cameraId) {
        try {
            streamingService.stopStream(cameraId);
            return ResponseEntity.ok(Map.of("message", "Stream stopped successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to stop stream for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stream stop failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{cameraId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getStreamStatus(@PathVariable Long cameraId) {
        try {
            Map<String, Object> status = streamingService.getStreamStatus(cameraId);
            return ResponseEntity.ok(status);
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to get stream status for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get stream status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==================== WebRTC 支持 ====================

    @PostMapping("/{cameraId}/webrtc/offer")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> generateWebRtcOffer(@PathVariable Long cameraId) {
        try {
            String offer = streamingService.generateWebRtcOffer(cameraId);
            Map<String, Object> response = new HashMap<>();
            response.put("offer", offer);
            response.put("type", "offer");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to generate WebRTC offer for camera {}: {}", cameraId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "WebRTC offer generation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{cameraId}/webrtc/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> processWebRtcAnswer(
            @PathVariable Long cameraId,
            @RequestBody WebRtcRequest request) {
        try {
            String result = streamingService.processWebRtcAnswer(cameraId, request.getSdp());
            Map<String, Object> response = new HashMap<>();
            response.put("status", result);
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to process WebRTC answer for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "WebRTC answer processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==================== 流控制 ====================

    @PostMapping("/{cameraId}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> pauseStream(@PathVariable Long cameraId) {
        try {
            streamingService.pauseStream(cameraId);
            return ResponseEntity.ok(Map.of("message", "Stream paused successfully"));
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to pause stream for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stream pause failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{cameraId}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> resumeStream(@PathVariable Long cameraId) {
        try {
            streamingService.resumeStream(cameraId);
            return ResponseEntity.ok(Map.of("message", "Stream resumed successfully"));
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to resume stream for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stream resume failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping("/{cameraId}/quality")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> updateStreamQuality(
            @PathVariable Long cameraId,
            @Valid @RequestBody StreamQualityRequest request) {
        try {
            streamingService.updateStreamQuality(
                    cameraId,
                    request.getResolution(),
                    request.getBitrate(),
                    request.getFrameRate());
            return ResponseEntity.ok(Map.of(
                    "message", "Stream quality updated successfully",
                    "resolution", request.getResolution(),
                    "bitrate", request.getBitrate(),
                    "frameRate", request.getFrameRate()));
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to update stream quality for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stream quality update failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==================== 录像相关 ====================

    @GetMapping("/{cameraId}/recording")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getRecordingUrl(
            @PathVariable Long cameraId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            String url = streamingService.getStreamRecordingUrl(cameraId, startTime, endTime);
            Map<String, Object> response = new HashMap<>();
            response.put("recordingUrl", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get recording URL for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get recording URL: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{cameraId}/recording/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> startRecording(@PathVariable Long cameraId) {
        try {
            streamingService.startStreamRecording(cameraId);
            return ResponseEntity.ok(Map.of("message", "Recording started successfully"));
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to start recording for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Recording start failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{cameraId}/recording/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> stopRecording(@PathVariable Long cameraId) {
        try {
            streamingService.stopStreamRecording(cameraId);
            return ResponseEntity.ok(Map.of("message", "Recording stopped successfully"));
        } catch (ServiceException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to stop recording for camera {}: {}", cameraId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Recording stop failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ==================== 活跃会话管理 ====================

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<StreamSession>> getActiveStreams() {
        try {
            List<StreamSession> sessions = streamingService.getActiveSessions();
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Failed to get active streams: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}