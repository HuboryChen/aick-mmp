package com.aick.mmp.central.controller;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.service.AiAnalysisService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalysisController {

    private final AiAnalysisService service;

    public AiAnalysisController(AiAnalysisService service) {
        this.service = service;
    }

    // --- Passenger Stats ---

    @GetMapping("/stats/passenger")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiPassengerStats>> getPassengerStats(
            @RequestParam Long cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(service.getPassengerStats(cameraId, startTime, endTime));
    }

    @GetMapping("/stats/passenger/realtime/{cameraId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<String> getRealtimePassenger(@PathVariable Long cameraId) {
        return ResponseEntity.ok(service.getRealtimePassenger(cameraId));
    }

    // --- Behavior Events ---

    @GetMapping("/alerts/behavior")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiBehaviorEvent>> getBehaviorEvents(
            @RequestParam(defaultValue = "0") Long cameraId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getBehaviorEvents(cameraId, eventType, status));
    }

    @PutMapping("/alerts/behavior/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiBehaviorEvent> updateBehaviorStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(service.updateBehaviorStatus(id, status));
    }

    // --- Vehicle Records ---

    @GetMapping("/vehicles/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiVehicleRecord>> getVehicleRecords(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(defaultValue = "0") Long cameraId) {
        return ResponseEntity.ok(service.getVehicleRecords(plateNumber, cameraId));
    }

    // --- Whitelist ---

    @GetMapping("/vehicles/whitelist")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<AiVehicleWhitelist>> getAllWhitelist() {
        return ResponseEntity.ok(service.getAllWhitelist());
    }

    @PostMapping("/vehicles/whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiVehicleWhitelist> addWhitelist(@RequestBody AiVehicleWhitelist entry) {
        return ResponseEntity.ok(service.addWhitelist(entry));
    }

    @PutMapping("/vehicles/whitelist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiVehicleWhitelist> updateWhitelist(
            @PathVariable Long id, @RequestBody AiVehicleWhitelist entry) {
        return ResponseEntity.ok(service.updateWhitelist(id, entry));
    }

    @DeleteMapping("/vehicles/whitelist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWhitelist(@PathVariable Long id) {
        service.deleteWhitelist(id);
        return ResponseEntity.noContent().build();
    }
}
