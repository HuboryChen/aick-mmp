package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.RecordingDTO;
import com.aick.mmp.central.service.RecordingQueryParams;
import com.aick.mmp.central.service.RecordingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/recordings")
public class RecordingController {

    private final RecordingService recordingService;

    @Autowired
    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<RecordingDTO>> getRecordings(
            @RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String integrityStatus,
            @RequestParam(required = false) String recordingType,
            @RequestParam(required = false) Long minFileSize,
            @RequestParam(required = false) Long maxFileSize,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        RecordingQueryParams params = RecordingQueryParams.builder()
                .cameraId(cameraId)
                .location(location)
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .integrityStatus(integrityStatus)
                .recordingType(recordingType)
                .minFileSize(minFileSize)
                .maxFileSize(maxFileSize)
                .build();
        
        Page<RecordingDTO> recordings = recordingService.getRecordings(params, pageable);
        return ResponseEntity.ok(recordings);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<RecordingDTO> getRecordingById(@PathVariable Long id) {
        RecordingDTO recording = recordingService.getRecordingById(id);
        return ResponseEntity.ok(recording);
    }

    @GetMapping("/{id}/url")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, String>> getRecordingUrl(@PathVariable Long id) {
        String url = recordingService.getRecordingUrl(id);
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecording(@PathVariable Long id) {
        recordingService.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }
}