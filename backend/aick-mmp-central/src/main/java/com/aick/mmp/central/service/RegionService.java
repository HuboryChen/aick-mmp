package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RegionDTO;
import com.aick.mmp.central.dto.RegionMoveDTO;
import com.aick.mmp.central.dto.RegionStatsDTO;
import com.aick.mmp.shared.model.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RegionService {
    RegionDTO createRegion(RegionDTO regionDTO);
    
    RegionDTO updateRegion(Long id, RegionDTO regionDTO);
    
    RegionDTO getRegionById(Long id);
    
    Page<RegionDTO> getAllRegions(Pageable pageable);
    
    List<RegionDTO> getAllRegions();
    
    List<RegionDTO> getChildRegions(Long parentId);
    
    void deleteRegion(Long id, boolean force);
    
    List<RegionDTO> getRegionTree();
    
    List<RegionDTO> getRegionsFlat();
    
    List<RegionDTO> searchRegions(String keyword);
    
    List<RegionDTO> getRegionsByLevel(Integer level);
    
    RegionDTO moveRegion(Long id, RegionMoveDTO moveDTO);
    
    boolean wouldCreateCycle(Long regionId, Long newParentId);
    
    List<Camera> getRegionCameras(Long regionId, boolean recursive);
    
    RegionStatsDTO getRegionStats(Long regionId);
    
    void assignCameraToRegion(Long cameraId, Long regionId);
    
    void removeCameraFromRegion(Long cameraId);
}