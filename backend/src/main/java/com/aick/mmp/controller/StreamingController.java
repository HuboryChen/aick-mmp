package com.aick.mmp.controller;

import com.aick.mmp.service.StreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streaming")
public class StreamingController {

    private final StreamingService streamingService;

    @Autowired
    public StreamingController(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @PostMapping("/{cameraId}/start")
    public ResponseEntity<?> startStream(@PathVariable Long cameraId) {
        streamingService.startStream(cameraId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cameraId}/stop")
    public ResponseEntity<?> stopStream(@PathVariable Long cameraId) {
        streamingService.stopStream(cameraId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cameraId}/status")
    public ResponseEntity<Map<String, Object>> getStreamStatus(@PathVariable Long cameraId) {
        return ResponseEntity.ok(streamingService.getStreamStatus(cameraId));
    }
}