package com.aick.mmp.controller;

import com.aick.mmp.dto.CameraDTO;
import com.aick.mmp.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameraService;

    @Autowired
    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @GetMapping
    public ResponseEntity<Page<CameraDTO>> getAllCameras(Pageable pageable) {
        return ResponseEntity.ok(cameraService.getAllCameras(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CameraDTO> getCameraById(@PathVariable Long id) {
        return ResponseEntity.ok(cameraService.getCameraById(id));
    }

    @PostMapping
    public ResponseEntity<CameraDTO> createCamera(@RequestBody CameraDTO cameraDTO) {
        CameraDTO created = cameraService.createCamera(cameraDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CameraDTO> updateCamera(@PathVariable Long id, @RequestBody CameraDTO cameraDTO) {
        return ResponseEntity.ok(cameraService.updateCamera(id, cameraDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        cameraService.deleteCamera(id);
        return ResponseEntity.noContent().build();
    }
}