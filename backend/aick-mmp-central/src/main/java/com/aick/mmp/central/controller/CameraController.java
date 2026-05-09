package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.BatchOperationDTO;
import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatisticsDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.central.dto.RecordingDTO;
import com.aick.mmp.central.entity.RecordingSchedule;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.RecordingScheduleService;
import com.aick.mmp.central.service.RecordingService;
import com.aick.mmp.shared.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;
    private final RecordingService recordingService;
    private final RecordingScheduleService recordingScheduleService;
    private final RecordingRepository recordingRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
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
    public ResponseEntity<Map<String, Object>> batchCameraOperation(@RequestBody BatchOperationDTO batchOperationDTO) {
        Map<String, Object> result = new HashMap<>();
        List<Long> processedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        switch (batchOperationDTO.getOperation()) {
            case DELETE:
                try {
                    cameraService.batchDeleteCameras(batchOperationDTO.getCameraIds());
                    processedIds.addAll(batchOperationDTO.getCameraIds());
                } catch (Exception e) {
                    failedIds.addAll(batchOperationDTO.getCameraIds());
                    log.error("Batch delete failed", e);
                }
                break;
            case UPDATE_EDGE_NODE:
                try {
                    cameraService.batchUpdateEdgeNode(batchOperationDTO.getCameraIds(), batchOperationDTO.getEdgeNodeId());
                    processedIds.addAll(batchOperationDTO.getCameraIds());
                } catch (Exception e) {
                    failedIds.addAll(batchOperationDTO.getCameraIds());
                    log.error("Batch update edge node failed", e);
                }
                break;
            case ENABLE:
                // 批量启用摄像头
                batchOperationDTO.getCameraIds().forEach(id -> {
                    try {
                        CameraStatusUpdateDTO statusUpdate = new CameraStatusUpdateDTO();
                        statusUpdate.setStatus(Camera.CameraStatus.ONLINE.name());
                        cameraService.updateCameraStatus(id, statusUpdate);
                        processedIds.add(id);
                    } catch (Exception e) {
                        failedIds.add(id);
                        log.error("Enable camera {} failed", id, e);
                    }
                });
                break;
            case DISABLE:
                // 批量禁用摄像头
                batchOperationDTO.getCameraIds().forEach(id -> {
                    try {
                        CameraStatusUpdateDTO statusUpdate = new CameraStatusUpdateDTO();
                        statusUpdate.setStatus(Camera.CameraStatus.OFFLINE.name());
                        cameraService.updateCameraStatus(id, statusUpdate);
                        processedIds.add(id);
                    } catch (Exception e) {
                        failedIds.add(id);
                        log.error("Disable camera {} failed", id, e);
                    }
                });
                break;
            default:
                break;
        }

        result.put("success", failedIds.isEmpty());
        result.put("processedCount", processedIds.size());
        result.put("failedCount", failedIds.size());
        result.put("processedIds", processedIds);
        result.put("failedIds", failedIds);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch-update-edge-node")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchUpdateEdgeNode(@RequestBody Map<String, Object> requestBody) {
        List<Long> cameraIds = (List<Long>) requestBody.get("cameraIds");
        Long edgeNodeId = ((Number) requestBody.get("edgeNodeId")).longValue();

        List<Long> processedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        try {
            cameraService.batchUpdateEdgeNode(cameraIds, edgeNodeId);
            processedIds.addAll(cameraIds);
        } catch (Exception e) {
            failedIds.addAll(cameraIds);
            log.error("Batch update edge node failed", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", failedIds.isEmpty());
        result.put("processedCount", processedIds.size());
        result.put("failedCount", failedIds.size());
        result.put("processedIds", processedIds);
        result.put("failedIds", failedIds);

        return ResponseEntity.ok(result);
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
    public ResponseEntity<Map<String, Object>> getOptimalEdgeNode(@RequestParam(required = false) String cameraName,
                                                                    @RequestParam(required = false) Long regionId) {
        CameraDTO cameraDTO = new CameraDTO();
        cameraDTO.setName(cameraName != null ? cameraName : "New Camera");
        cameraDTO.setRegionId(regionId);
        Long optimalNodeId = cameraService.selectOptimalEdgeNode(cameraDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("optimalEdgeNodeId", optimalNodeId);
        result.put("regionId", regionId);
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

    // ========== 孤立录像管理接口 ==========

    /**
     * 查询孤立录像列表
     */
    @GetMapping("/recordings/orphaned")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<RecordingDTO>> getOrphanedRecordings(Pageable pageable) {
        Page<RecordingDTO> orphanedRecordings = recordingService.getOrphanedRecordings(pageable);
        return ResponseEntity.ok(orphanedRecordings);
    }

    /**
     * 查询已删除录像列表
     */
    @GetMapping("/recordings/deleted")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<RecordingDTO>> getDeletedRecordings(Pageable pageable) {
        Page<RecordingDTO> deletedRecordings = recordingService.getDeletedRecordings(pageable);
        return ResponseEntity.ok(deletedRecordings);
    }

    /**
     * 获取孤立录像统计
     */
    @GetMapping("/recordings/orphaned/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getOrphanedRecordingsCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("orphanedCount", recordingService.countOrphanedRecordings());
        result.put("deletedCount", recordingRepository.count());
        return ResponseEntity.ok(result);
    }

    /**
     * 手动清理孤立录像（仅管理员）
     */
    @PostMapping("/recordings/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOrphanedRecordings(
            @RequestParam(defaultValue = "30") int daysOld) {
        int cleanedCount = recordingService.cleanupOrphanedRecordings(daysOld);
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("daysOld", daysOld);
        return ResponseEntity.ok(result);
    }

    /**
     * 恢复已软删除的录像（仅管理员）
     */
    @PostMapping("/recordings/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreRecording(@PathVariable Long id) {
        recordingService.restore(id);
        return ResponseEntity.ok().build();
    }

    // ========== 统计聚合接口 ==========

    /**
     * 获取摄像头统计概览
     */
    @GetMapping("/statistics/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<CameraStatisticsDTO> getStatisticsSummary(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long edgeNodeId,
            @RequestHeader(value = "X-Cache-Refresh", required = false) Boolean refresh) {
        boolean forceRefresh = refresh != null && refresh;
        CameraStatisticsDTO statistics = cameraService.getCameraStatisticsSummary(regionId, edgeNodeId, forceRefresh);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 手动刷新统计缓存（仅管理员）
     */
    @PostMapping("/statistics/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> refreshStatistics() {
        cameraService.refreshStatisticsCache();
        return ResponseEntity.ok().build();
    }

    // ========== 录像计划管理接口 ==========

    /**
     * 获取所有录像计划
     */
    @GetMapping("/recording-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<RecordingSchedule>> getAllRecordingSchedules() {
        List<RecordingSchedule> schedules = recordingScheduleService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * 获取录像计划详情
     */
    @GetMapping("/recording-schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<RecordingSchedule> getRecordingSchedule(@PathVariable Long id) {
        return recordingScheduleService.getSchedule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建录像计划
     */
    @PostMapping("/recording-schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecordingSchedule> createRecordingSchedule(@RequestBody RecordingSchedule schedule) {
        RecordingSchedule created = recordingScheduleService.createSchedule(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新录像计划
     */
    @PutMapping("/recording-schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecordingSchedule> updateRecordingSchedule(
            @PathVariable Long id, 
            @RequestBody RecordingSchedule schedule) {
        RecordingSchedule updated = recordingScheduleService.updateSchedule(id, schedule);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除录像计划
     */
    @DeleteMapping("/recording-schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecordingSchedule(@PathVariable Long id) {
        recordingScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用/禁用录像计划
     */
    @PatchMapping("/recording-schedules/{id}/enabled")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<RecordingSchedule> setRecordingScheduleEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        RecordingSchedule updated = recordingScheduleService.setEnabled(id, enabled);
        return ResponseEntity.ok(updated);
    }

    /**
     * 根据摄像头ID获取录像计划
     */
    @GetMapping("/recording-schedules/camera/{cameraId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RecordingSchedule>> getRecordingSchedulesByCamera(@PathVariable Long cameraId) {
        List<RecordingSchedule> schedules = recordingScheduleService.getSchedulesByCamera(cameraId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 获取所有已启用的录像计划（供边缘节点同步）
     */
    @GetMapping("/recording-schedules/enabled")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<RecordingSchedule>> getAllEnabledRecordingSchedules() {
        List<RecordingSchedule> schedules = recordingScheduleService.getAllEnabledSchedules();
        return ResponseEntity.ok(schedules);
    }
}