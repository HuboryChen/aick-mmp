package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.shared.model.Camera;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cameras")
public class CameraController {

    private final CameraService cameraService;

    @Autowired
    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CameraDTO>> getCameras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Camera.CameraStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long edgeNodeId) {
        
        Pageable pageable = PageRequest.of(page, size);
        GetCamerasRequestDTO request = GetCamerasRequestDTO.builder()
                .pageable(pageable)
                .status(status)
                .location(location)
                .edgeNodeId(edgeNodeId)
                .build();
        return ResponseEntity.ok(cameraService.getCameras(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CameraDTO> getCameraById(@PathVariable Long id) {
        return ResponseEntity.ok(cameraService.getCameraById(id));
    }

    @GetMapping("/edge-node/{edgeNodeId}")
    public ResponseEntity<List<CameraDTO>> getCamerasByEdgeNode(@PathVariable Long edgeNodeId) {
        List<CameraDTO> cameras = cameraService.getCamerasByEdgeNodeId(edgeNodeId, Pageable.unpaged()).getContent();
        return ResponseEntity.ok(cameras);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CameraDTO> createCamera(@RequestBody CameraDTO cameraDTO) {
        CameraDTO created = cameraService.createCamera(cameraDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CameraDTO> updateCamera(@PathVariable Long id, @RequestBody CameraDTO cameraDTO) {
        return ResponseEntity.ok(cameraService.updateCamera(id, cameraDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        cameraService.deleteCamera(id);
        return ResponseEntity.noContent().build();
    }
}