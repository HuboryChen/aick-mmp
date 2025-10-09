package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RegionDTO;
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
    
    void deleteRegion(Long id);
}