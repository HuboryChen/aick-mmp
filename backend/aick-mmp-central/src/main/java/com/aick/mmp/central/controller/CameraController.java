package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.BatchOperationDTO;
import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    @GetMapping
    // @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 暂时注释掉以便测试
    public ResponseEntity<Page<CameraDTO>> getAllCameras(
            @RequestParam(required = false) Long regionId,
            Pageable pageable) {
        if (regionId != null) {
            GetCamerasRequestDTO request = GetCamerasRequestDTO.builder()
                    .regionId(regionId)
                    .pageable(pageable)
                    .build();
            Page<CameraDTO> cameras = cameraService.getCameras(request);
            return ResponseEntity.ok(cameras);
        }
        Page<CameraDTO> cameras = cameraService.getAllCameras(pageable);
        return ResponseEntity.ok(cameras);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<CameraDTO>> searchCameras(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String edgeNodeId,
            @RequestParam(required = false) Camera.CameraStatus status,
            Pageable pageable) {
        
        GetCamerasRequestDTO request = GetCamerasRequestDTO.builder()
                .location(location)
                .regionId(regionId)
                .status(status)
                .edgeNodeId(edgeNodeId != null ? Long.parseLong(edgeNodeId) : null)
                .pageable(pageable)
                .build();
        
        Page<CameraDTO> cameras = cameraService.getCameras(request);
        return ResponseEntity.ok(cameras);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CameraDTO> getCameraById(@PathVariable Long id) {
        CameraDTO camera = cameraService.getCameraById(id);
        return ResponseEntity.ok(camera);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CameraDTO> createCamera(@RequestBody CameraDTO cameraDTO) {
        CameraDTO createdCamera = cameraService.createCamera(cameraDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCamera);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CameraDTO> updateCamera(@PathVariable Long id, @RequestBody CameraDTO cameraDTO) {
        CameraDTO updatedCamera = cameraService.updateCamera(id, cameraDTO);
        return ResponseEntity.ok(updatedCamera);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> updateCameraStatus(@PathVariable Long id, @RequestBody CameraStatusUpdateDTO statusUpdateDTO) {
        cameraService.updateCameraStatus(id, statusUpdateDTO);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/resolution")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> updateCameraResolution(@PathVariable Long id, @RequestBody String resolution) {
        cameraService.updateCameraResolution(id, resolution);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateCameraCredentials(@PathVariable Long id, @RequestBody String[] credentials) {
        cameraService.updateCameraCredentials(id, credentials[0], credentials[1]);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        cameraService.deleteCamera(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch-operation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchCameraOperation(@RequestBody BatchOperationDTO batchOperationDTO) {
        switch (batchOperationDTO.getOperation()) {
            case DELETE:
                cameraService.batchDeleteCameras(batchOperationDTO.getCameraIds());
                break;
            case UPDATE_EDGE_NODE:
                cameraService.batchUpdateEdgeNode(batchOperationDTO.getCameraIds(), batchOperationDTO.getEdgeNodeId());
                break;
            case ENABLE:
                // 批量启用摄像头
                batchOperationDTO.getCameraIds().forEach(id -> {
                    CameraStatusUpdateDTO statusUpdate = new CameraStatusUpdateDTO();
                    statusUpdate.setStatus(Camera.CameraStatus.ONLINE.name());
                    cameraService.updateCameraStatus(id, statusUpdate);
                });
                break;
            case DISABLE:
                // 批量禁用摄像头
                batchOperationDTO.getCameraIds().forEach(id -> {
                    CameraStatusUpdateDTO statusUpdate = new CameraStatusUpdateDTO();
                    statusUpdate.setStatus(Camera.CameraStatus.OFFLINE.name());
                    cameraService.updateCameraStatus(id, statusUpdate);
                });
                break;
            default:
                break;
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-update-edge-node")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchUpdateEdgeNode(@RequestBody Map<String, Object> requestBody) {
        List<Long> cameraIds = (List<Long>) requestBody.get("cameraIds");
        Long edgeNodeId = ((Number) requestBody.get("edgeNodeId")).longValue();
        
        cameraService.batchUpdateEdgeNode(cameraIds, edgeNodeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/online")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<CameraDTO>> getOnlineCameras(@RequestParam(required = false) Long edgeNodeId) {
        List<CameraDTO> onlineCameras;
        if (edgeNodeId != null) {
            onlineCameras = cameraService.getOnlineCamerasByEdgeNode(edgeNodeId);
        } else {
            onlineCameras = cameraService.getAllOnlineCameras();
        }
        return ResponseEntity.ok(onlineCameras);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getCameraStatistics(@RequestParam Long cameraId) {
        Map<String, Object> statistics = cameraService.getCameraStatistics(cameraId);
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Boolean> testCameraConnection(@PathVariable Long id) {
        boolean isConnected = cameraService.testCameraConnection(id);
        return ResponseEntity.ok(isConnected);
    }

    @PostMapping("/{id}/start-stream")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<String> startCameraStream(@PathVariable Long id) {
        String streamUrl = cameraService.startCameraStream(id);
        return ResponseEntity.ok(streamUrl);
    }

    @PostMapping("/{id}/stop-stream")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> stopCameraStream(@PathVariable Long id) {
        cameraService.stopCameraStream(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> autoAssignCameras() {
        cameraService.autoAssignCamerasToEdgeNodes();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/optimal-edge-node")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getOptimalEdgeNode(@RequestParam(required = false) String cameraName) {
        CameraDTO cameraDTO = new CameraDTO();
        cameraDTO.setName(cameraName != null ? cameraName : "New Camera");
        Long optimalNodeId = cameraService.selectOptimalEdgeNode(cameraDTO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("optimalEdgeNodeId", optimalNodeId);
        result.put("message", "Optimal edge node selected successfully");
        return ResponseEntity.ok(result);
    }

    /**
     * 查询待分配池中的摄像头
     */
    @GetMapping("/pending-allocation")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<CameraDTO>> getPendingAllocationCameras(Pageable pageable) {
        Page<CameraDTO> pendingCameras = cameraService.getCamerasByStatus(Camera.CameraStatus.PENDING_ALLOCATION, pageable);
        return ResponseEntity.ok(pendingCameras);
    }

    /**
     * 恢复已删除的摄像头
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CameraDTO> restoreCamera(@PathVariable Long id) {
        CameraDTO restoredCamera = cameraService.restoreCamera(id);
        return ResponseEntity.ok(restoredCamera);
    }

    /**
     * 管理员强制物理删除摄像头
     */
    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDeleteCamera(@PathVariable Long id) {
        cameraService.forceDeleteCamera(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 查询已删除的摄像头（仅管理员）
     */
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CameraDTO>> getDeletedCameras() {
        List<CameraDTO> deletedCameras = cameraService.getDeletedCameras();
        return ResponseEntity.ok(deletedCameras);
    }
}