package com.aick.mmp.service;

import com.aick.mmp.dto.CameraDTO;
import com.aick.mmp.dto.CameraStatusUpdateDTO;
import com.aick.mmp.model.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CameraService {
    Page<CameraDTO> getAllCameras(Pageable pageable);
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
    String getCameraStreamUrl(Long cameraId);
    Map<String, Object> getCameraStatistics(Long cameraId);
    List<CameraDTO> getOnlineCamerasByEdgeNode(Long edgeNodeId);
    long getCameraCountByStatus(Camera.CameraStatus status);
    boolean testCameraConnection(Long cameraId);
}