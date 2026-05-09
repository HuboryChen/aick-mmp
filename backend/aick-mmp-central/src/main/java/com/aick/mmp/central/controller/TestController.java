package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.service.CameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final CameraService cameraService;
    private final CameraRepository cameraRepository;
    
    @GetMapping("/cameras")
    public ResponseEntity<Page<CameraDTO>> testGetCameras() {
        try {
            Page<CameraDTO> cameras = cameraService.getAllCameras(PageRequest.of(0, 10));
            return ResponseEntity.ok(cameras);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
    
    @GetMapping("/cameras/count")
    public ResponseEntity<Long> testCameraCount() {
        try {
            long count = cameraRepository.count();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(-1L);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}