package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraStatisticsDTO;
import com.aick.mmp.central.dto.CameraStatusUpdateDTO;
import com.aick.mmp.central.dto.GetCamerasRequestDTO;
import com.aick.mmp.shared.model.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CameraService {
    Page<CameraDTO> getAllCameras(Pageable pageable);
    Page<CameraDTO> getCameras(GetCamerasRequestDTO request);
    CameraDTO getCameraById(Long id);
    CameraDTO createCamera(CameraDTO cameraDTO);
    CameraDTO updateCamera(Long id, CameraDTO cameraDTO);
    void updateCameraStatus(Long id, CameraStatusUpdateDTO statusUpdateDTO);
    void updateCameraResolution(Long id, String resolution);
    void updateCameraCredentials(Long id, String username, String password);
    void deleteCamera(Long id);
    void batchDeleteCameras(List<Long> cameraIds);
    void batchUpdateEdgeNode(List<Long> cameraIds, Long edgeNodeId);
    String startCameraStream(Long cameraId);
    void stopCameraStream(Long cameraId);
    Page<CameraDTO> getCamerasByStatus(Camera.CameraStatus status, Pageable pageable);
    List<CameraDTO> getCamerasByEdgeNode(Long edgeNodeId);
    List<CameraDTO> getAllOnlineCameras();
    CameraDTO restoreCamera(Long id);
    void forceDeleteCamera(Long id);
    List<CameraDTO> getDeletedCameras();
    Map<String, Object> batchUpdateStatus(List<Long> ids, Camera.CameraStatus newStatus);
    CameraDTO convertToDto(Camera camera);
    Camera convertToEntity(CameraDTO dto);
    Long selectOptimalEdgeNode(CameraDTO cameraDTO);
    void autoAssignCamerasToEdgeNodes();

    // Additional methods used by controllers and other services
    List<CameraDTO> getOnlineCamerasByEdgeNode(Long edgeNodeId);
    Map<String, Object> getCameraStatistics(Long cameraId);
    boolean testCameraConnection(Long cameraId);
    long getCameraCountByStatus(Camera.CameraStatus status);

    // ========== 统计聚合API ==========
    /**
     * 获取摄像头统计概览
     */
    CameraStatisticsDTO getCameraStatisticsSummary(Long regionId, Long edgeNodeId, boolean forceRefresh);

    /**
     * 刷新统计缓存
     */
    void refreshStatisticsCache();

    // ========== 孤立录像管理 ==========
    /**
     * 获取孤立录像数量
     */
    long getOrphanedRecordingsCount();

    /**
     * 清理超过指定天数的孤立录像
     */
    int cleanupOrphanedRecordings(int daysOld);
}