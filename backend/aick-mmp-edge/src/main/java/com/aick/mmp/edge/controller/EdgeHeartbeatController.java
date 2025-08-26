package com.aick.mmp.edge.controller;

import com.aick.mmp.edge.dto.HeartbeatRequest;
import com.aick.mmp.edge.dto.NetworkMetricsDTO;
import com.aick.mmp.edge.service.EdgeHeartbeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/edge/heartbeat")
@Tag(name = "Edge Heartbeat", description = "Edge node heartbeat management")
@Profile("edge")
public class EdgeHeartbeatController {

    private static final Logger logger = LoggerFactory.getLogger(EdgeHeartbeatController.class);

    @Autowired
    private EdgeHeartbeatService edgeHeartbeatService;

    @GetMapping("/status")
    @Operation(summary = "Get current edge node status")
    public ResponseEntity<HeartbeatRequest> getCurrentStatus() {
        try {
            HeartbeatRequest status = edgeHeartbeatService.getCurrentStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting current status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/network-metrics")
    @Operation(summary = "Get current network metrics")
    public ResponseEntity<NetworkMetricsDTO> getNetworkMetrics() {
        try {
            NetworkMetricsDTO metrics = edgeHeartbeatService.collectNetworkMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error getting network metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/manual")
    @Operation(summary = "Send manual heartbeat to central server")
    public ResponseEntity<String> sendManualHeartbeat() {
        try {
            HeartbeatRequest heartbeat = edgeHeartbeatService.collectSystemMetrics();
            boolean success = edgeHeartbeatService.sendHeartbeat(heartbeat);
            
            if (success) {
                return ResponseEntity.ok("Heartbeat sent successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to send heartbeat");
            }
        } catch (Exception e) {
            logger.error("Error sending manual heartbeat", e);
            return ResponseEntity.internalServerError().body("Error sending heartbeat: " + e.getMessage());
        }
    }

    @PostMapping("/start")
    @Operation(summary = "Start automatic heartbeat monitoring")
    public ResponseEntity<String> startHeartbeatMonitoring() {
        try {
            edgeHeartbeatService.startHeartbeatMonitoring();
            return ResponseEntity.ok("Heartbeat monitoring started");
        } catch (Exception e) {
            logger.error("Error starting heartbeat monitoring", e);
            return ResponseEntity.internalServerError().body("Error starting monitoring: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop automatic heartbeat monitoring")
    public ResponseEntity<String> stopHeartbeatMonitoring() {
        try {
            edgeHeartbeatService.stopHeartbeatMonitoring();
            return ResponseEntity.ok("Heartbeat monitoring stopped");
        } catch (Exception e) {
            logger.error("Error stopping heartbeat monitoring", e);
            return ResponseEntity.internalServerError().body("Error stopping monitoring: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check for edge node")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Edge node is healthy");
    }
}