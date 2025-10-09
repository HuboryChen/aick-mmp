package com.aick.mmp.edge.controller;

import com.aick.mmp.edge.dto.EdgeStreamDTO;
import com.aick.mmp.edge.service.EdgeStreamService;
import com.aick.mmp.shared.model.StreamSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/edge/streams")
@Tag(name = "Edge Stream Management", description = "Stream management operations on edge nodes")
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeStreamController {

    private final EdgeStreamService edgeStreamService;

    @PostMapping("/start/{cameraId}")
    @Operation(summary = "Start a stream for a camera")
    public ResponseEntity<EdgeStreamDTO> startStream(
            @PathVariable Long cameraId,
            @RequestBody(required = false) Map<String, Object> parameters) {
        try {
            EdgeStreamDTO stream = edgeStreamService.startStream(cameraId, parameters);
            return ResponseEntity.ok(stream);
        } catch (Exception e) {
            log.error("Error starting stream for camera: {}", cameraId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/stop/{sessionId}")
    @Operation(summary = "Stop a stream session")
    public ResponseEntity<Void> stopStream(@PathVariable String sessionId) {
        try {
            edgeStreamService.stopStream(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error stopping stream: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get stream by session ID")
    public ResponseEntity<EdgeStreamDTO> getStream(@PathVariable String sessionId) {
        try {
            Optional<EdgeStreamDTO> stream = edgeStreamService.getStream(sessionId);
            return stream.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting stream: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all active streams on this edge node")
    public ResponseEntity<List<EdgeStreamDTO>> getActiveStreams() {
        try {
            List<EdgeStreamDTO> streams = edgeStreamService.getActiveStreams();
            return ResponseEntity.ok(streams);
        } catch (Exception e) {
            log.error("Error getting active streams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/camera/{cameraId}")
    @Operation(summary = "Get streams by camera ID")
    public ResponseEntity<List<EdgeStreamDTO>> getStreamsByCamera(@PathVariable Long cameraId) {
        try {
            List<EdgeStreamDTO> streams = edgeStreamService.getStreamsByCamera(cameraId);
            return ResponseEntity.ok(streams);
        } catch (Exception e) {
            log.error("Error getting streams for camera: {}", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{sessionId}/quality")
    @Operation(summary = "Adjust stream quality")
    public ResponseEntity<Void> adjustStreamQuality(
            @PathVariable String sessionId,
            @RequestParam int qualityLevel) {
        try {
            edgeStreamService.adjustStreamQuality(sessionId, qualityLevel);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error adjusting stream quality for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/metrics")
    @Operation(summary = "Get stream metrics")
    public ResponseEntity<Map<String, Object>> getStreamMetrics(@PathVariable String sessionId) {
        try {
            Map<String, Object> metrics = edgeStreamService.getStreamMetrics(sessionId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting stream metrics for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/metrics/all")
    @Operation(summary = "Get all stream metrics for this edge node")
    public ResponseEntity<Map<String, Object>> getAllStreamMetrics() {
        try {
            Map<String, Object> metrics = edgeStreamService.getAllStreamMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting all stream metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{sessionId}/status")
    @Operation(summary = "Update stream status")
    public ResponseEntity<Void> updateStreamStatus(
            @PathVariable String sessionId,
            @RequestParam StreamSession.StreamStatus status) {
        try {
            edgeStreamService.updateStreamStatus(sessionId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating stream status for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Get stream count by status")
    public ResponseEntity<Long> getStreamCountByStatus(@PathVariable StreamSession.StreamStatus status) {
        try {
            long count = edgeStreamService.getStreamCountByStatus(status);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting stream count by status: {}", status, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/count/active")
    @Operation(summary = "Get total active streams count")
    public ResponseEntity<Long> getActiveStreamsCount() {
        try {
            long count = edgeStreamService.getActiveStreamsCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting active streams count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/capacity/available")
    @Operation(summary = "Check if edge node can handle more streams")
    public ResponseEntity<Boolean> canHandleMoreStreams() {
        try {
            boolean available = edgeStreamService.canHandleMoreStreams();
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            log.error("Error checking stream capacity", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/bandwidth/current")
    @Operation(summary = "Get current bandwidth usage")
    public ResponseEntity<Double> getCurrentBandwidthUsage() {
        try {
            double usage = edgeStreamService.getCurrentBandwidthUsage();
            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            log.error("Error getting current bandwidth usage", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/restart-failed")
    @Operation(summary = "Restart failed streams")
    public ResponseEntity<Void> restartFailedStreams() {
        try {
            edgeStreamService.restartFailedStreams();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error restarting failed streams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup-inactive")
    @Operation(summary = "Cleanup inactive streams")
    public ResponseEntity<Void> cleanupInactiveStreams() {
        try {
            edgeStreamService.cleanupInactiveStreams();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cleaning up inactive streams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/url/local")
    @Operation(summary = "Get stream URL for local access")
    public ResponseEntity<String> getLocalStreamUrl(@PathVariable String sessionId) {
        try {
            String url = edgeStreamService.getLocalStreamUrl(sessionId);
            return url != null ? ResponseEntity.ok(url) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting local stream URL for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/url/external")
    @Operation(summary = "Get stream URL for external access")
    public ResponseEntity<String> getExternalStreamUrl(@PathVariable String sessionId) {
        try {
            String url = edgeStreamService.getExternalStreamUrl(sessionId);
            return url != null ? ResponseEntity.ok(url) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting external stream URL for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialize streaming service")
    public ResponseEntity<Void> initializeStreaming() {
        try {
            edgeStreamService.initializeStreaming();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error initializing streaming service", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/shutdown")
    @Operation(summary = "Shutdown all streams")
    public ResponseEntity<Void> shutdownAllStreams() {
        try {
            edgeStreamService.shutdownAllStreams();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error shutting down all streams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{sessionId}/heartbeat")
    @Operation(summary = "Process stream heartbeat")
    public ResponseEntity<Void> processStreamHeartbeat(@PathVariable String sessionId) {
        try {
            edgeStreamService.processStreamHeartbeat(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing stream heartbeat for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/health")
    @Operation(summary = "Get stream health status")
    public ResponseEntity<Map<String, Object>> getStreamHealth(@PathVariable String sessionId) {
        try {
            Map<String, Object> health = edgeStreamService.getStreamHealth(sessionId);
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting stream health for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check for stream service")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Edge stream service is healthy");
    }
}