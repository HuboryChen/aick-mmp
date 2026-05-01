package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraDTO;
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
    Page<CameraDTO> getCamerasByLocation(String location, Pageable pageable);
    Page<CameraDTO> getCamerasByEdgeNodeId(Long edgeNodeId, Pageable pageable);
    Page<CameraDTO> getCamerasByStatus(Camera.CameraStatus status, Pageable pageable);
    CameraDTO getCameraById(Long id);
    CameraDTO createCamera(CameraDTO cameraDTO);
    CameraDTO updateCamera(Long id, CameraDTO cameraDTO);
    void updateCameraStatus(Long id, CameraStatusUpdateDTO statusUpdateDTO);
    void updateCameraResolution(Long id, String resolution);
    void updateCameraCredentials(Long id, String username, String password);
    void deleteCamera(Long id);
    void batchDeleteCameras(List<Long> cameraIds);
    void batchUpdateEdgeNode(List<Long> cameraIds, Long edgeNodeId);
    String getCameraStreamUrl(Long cameraId);
    Map<String, Object> getCameraStatistics(Long cameraId);
    List<CameraDTO> getOnlineCamerasByEdgeNode(Long edgeNodeId);
    long getCameraCountByStatus(Camera.CameraStatus status);
    long getCameraCount();
    boolean testCameraConnection(Long cameraId);

    // 添加流媒体相关方法
    String startCameraStream(Long cameraId);
    void stopCameraStream(Long cameraId);
    
    // 负载均衡分配相关方法
    Long selectOptimalEdgeNode(CameraDTO cameraDTO);
    void autoAssignCamerasToEdgeNodes();

    // 软删除相关方法
    CameraDTO restoreCamera(Long id);
    void forceDeleteCamera(Long id);
    List<CameraDTO> getAllOnlineCameras();
    List<CameraDTO> getDeletedCameras();
}