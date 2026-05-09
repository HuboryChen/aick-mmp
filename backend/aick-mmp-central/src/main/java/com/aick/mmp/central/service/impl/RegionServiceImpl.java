package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.RegionDTO;
import com.aick.mmp.central.dto.RegionMoveDTO;
import com.aick.mmp.central.dto.RegionStatsDTO;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.CdnNodeRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.RegionService;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl implements RegionService {
    
    private final RegionRepository regionRepository;
    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final CdnNodeRepository cdnNodeRepository;
    
    @Override
    @Transactional
    public RegionDTO createRegion(RegionDTO regionDTO) {
        if (regionRepository.existsByCodeAndIsDeletedFalse(regionDTO.getCode())) {
            throw new IllegalArgumentException("Region code already exists: " + regionDTO.getCode());
        }
        
        Region region = Region.builder()
                .code(regionDTO.getCode())
                .name(regionDTO.getName())
                .description(regionDTO.getDescription())
                .parentId(regionDTO.getParentId())
                .sortOrder(regionDTO.getSortOrder() != null ? regionDTO.getSortOrder() : 0)
                .isDeleted(false)
                .build();
        
        if (regionDTO.getParentId() != null) {
            Region parent = regionRepository.findByIdAndIsDeletedFalse(regionDTO.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent region not found: " + regionDTO.getParentId()));
            region.setLevel(parent.getLevel() + 1);
            region.setPath(parent.getPath() + "/" + region.getCode());
        } else {
            region.setLevel(1);
            region.setPath("/" + region.getCode());
        }
        
        Region savedRegion = regionRepository.save(region);
        log.info("Created new region: {} ({})", savedRegion.getName(), savedRegion.getCode());
        return convertToDTO(savedRegion);
    }
    
    @Override
    @Transactional
    public RegionDTO updateRegion(Long id, RegionDTO regionDTO) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + id));
        
        if (!region.getCode().equals(regionDTO.getCode()) 
                && regionRepository.existsByCodeAndIsDeletedFalse(regionDTO.getCode())) {
            throw new IllegalArgumentException("Region code already exists: " + regionDTO.getCode());
        }
        
        region.setCode(regionDTO.getCode());
        region.setName(regionDTO.getName());
        region.setDescription(regionDTO.getDescription());
        if (regionDTO.getSortOrder() != null) {
            region.setSortOrder(regionDTO.getSortOrder());
        }
        
        Region updatedRegion = regionRepository.save(region);
        log.info("Updated region: {} ({})", updatedRegion.getName(), updatedRegion.getCode());
        return convertToDTO(updatedRegion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public RegionDTO getRegionById(Long id) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + id));
        return convertToDTO(region);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RegionDTO> getAllRegions(Pageable pageable) {
        return regionRepository.findAll(pageable).map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getChildRegions(Long parentId) {
        return regionRepository.findByParentIdAndIsDeletedFalse(parentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteRegion(Long id, boolean force) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + id));
        
        long cameraCount = cameraRepository.countByRegionIdAndIsDeletedFalse(id);
        long childRegionCount = regionRepository.countByParentId(id);
        
        if (!force && (cameraCount > 0 || childRegionCount > 0)) {
            throw new IllegalStateException("Region is not empty. Use force=true to delete recursively.");
        }
        
        if (force) {
            deleteRegionRecursively(id);
        } else {
            softDeleteRegion(region);
        }
        
        log.info("Deleted region with id: {} (force: {})", id, force);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getRegionTree() {
        List<Region> rootRegions = regionRepository.findByParentIdIsNullAndIsDeletedFalse();
        return rootRegions.stream()
                .map(this::buildRegionTree)
                .sorted(Comparator.comparing(RegionDTO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getRegionsFlat() {
        return regionRepository.findByIsDeletedFalseOrderBySortOrderAsc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> searchRegions(String keyword) {
        return regionRepository.searchByKeyword(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getRegionsByLevel(Integer level) {
        return regionRepository.findByLevelAndIsDeletedFalse(level).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public RegionDTO moveRegion(Long id, RegionMoveDTO moveDTO) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + id));
        
        Long newParentId = moveDTO.getNewParentId();
        
        if (id.equals(newParentId)) {
            throw new IllegalArgumentException("Cannot move region to itself");
        }
        
        if (newParentId != null) {
            if (wouldCreateCycle(id, newParentId)) {
                throw new IllegalArgumentException("Moving region would create a cycle in the hierarchy");
            }
            
            Region newParent = regionRepository.findByIdAndIsDeletedFalse(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("New parent region not found: " + newParentId));
            
            region.setParentId(newParentId);
            region.setLevel(newParent.getLevel() + 1);
            region.setPath(newParent.getPath() + "/" + region.getCode());
            
            updateDescendantPaths(region);
        } else {
            region.setParentId(null);
            region.setLevel(1);
            region.setPath("/" + region.getCode());
            
            updateDescendantPaths(region);
        }
        
        Region updatedRegion = regionRepository.save(region);
        log.info("Moved region {} to new parent {}", id, newParentId);
        return convertToDTO(updatedRegion);
    }
    
    @Override
    public boolean wouldCreateCycle(Long regionId, Long newParentId) {
        if (newParentId == null) {
            return false;
        }
        if (regionId.equals(newParentId)) {
            return true;
        }
        
        Set<Long> visited = new HashSet<>();
        Long currentId = newParentId;
        
        while (currentId != null) {
            if (visited.contains(currentId)) {
                break;
            }
            if (currentId.equals(regionId)) {
                return true;
            }
            visited.add(currentId);
            
            Optional<Region> parent = regionRepository.findByIdAndIsDeletedFalse(currentId);
            if (parent.isPresent()) {
                currentId = parent.get().getParentId();
            } else {
                break;
            }
        }
        
        return false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Camera> getRegionCameras(Long regionId, boolean recursive) {
        if (recursive) {
            List<Long> regionIds = getAllDescendantRegionIds(regionId);
            return cameraRepository.findByRegionIdIn(regionIds);
        } else {
            return cameraRepository.findByRegionIdAndIsDeletedFalse(regionId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public RegionStatsDTO getRegionStats(Long regionId) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(regionId)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + regionId));
        
        long totalCameras = cameraRepository.countByRegionIdAndIsDeletedFalse(regionId);
        long onlineCameras = cameraRepository.countByRegionIdAndStatusAndIsDeletedFalse(regionId, Camera.CameraStatus.ONLINE);
        long offlineCameras = totalCameras - onlineCameras;
        long childRegions = regionRepository.countByParentId(regionId);
        
        // Get all descendant region IDs (recursive)
        List<Long> allRegionIds = getAllDescendantRegionIds(regionId);
        
        // Direct counts (only this region)
        long directEdgeNodeCount = edgeNodeRepository.countByRegionId(regionId);
        long directCdnNodeCount = cdnNodeRepository.countByRegionIdAndIsDeletedFalse(regionId);
        
        // Recursive counts (this region + all descendants)
        long recursiveEdgeNodeCount = edgeNodeRepository.countByRegionIdIn(allRegionIds);
        long recursiveCdnNodeCount = cdnNodeRepository.countByRegionIdIn(allRegionIds);
        
        return RegionStatsDTO.builder()
                .regionId(regionId)
                .regionName(region.getName())
                .totalCameras(totalCameras)
                .onlineCameras(onlineCameras)
                .offlineCameras(offlineCameras)
                .childRegions(childRegions)
                .edgeNodeCount(recursiveEdgeNodeCount)
                .cdnNodeCount(recursiveCdnNodeCount)
                .directEdgeNodeCount(directEdgeNodeCount)
                .directCdnNodeCount(directCdnNodeCount)
                .build();
    }
    
    @Override
    @Transactional
    public void assignCameraToRegion(Long cameraId, Long regionId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new IllegalArgumentException("Camera not found with id: " + cameraId));
        
        if (regionId != null) {
            Region region = regionRepository.findByIdAndIsDeletedFalse(regionId)
                    .orElseThrow(() -> new IllegalArgumentException("Region not found with id: " + regionId));
        }
        
        camera.setRegionId(regionId);
        cameraRepository.save(camera);
        log.info("Assigned camera {} to region {}", cameraId, regionId);
    }
    
    @Override
    @Transactional
    public void removeCameraFromRegion(Long cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new IllegalArgumentException("Camera not found with id: " + cameraId));
        
        camera.setRegionId(null);
        cameraRepository.save(camera);
        log.info("Removed camera {} from region", cameraId);
    }
    
    private void deleteRegionRecursively(Long regionId) {
        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(regionId);
        for (Region child : children) {
            deleteRegionRecursively(child.getId());
        }
        
        List<Camera> cameras = cameraRepository.findByRegionIdAndIsDeletedFalse(regionId);
        for (Camera camera : cameras) {
            if (regionRepository.findByIdAndIsDeletedFalse(regionId).isPresent()) {
                Region parent = regionRepository.findByIdAndIsDeletedFalse(regionId).get().getParentId() != null
                        ? regionRepository.findByIdAndIsDeletedFalse(regionId).get()
                        : null;
                camera.setRegionId(parent != null ? parent.getId() : null);
                cameraRepository.save(camera);
            }
        }
        
        Region region = regionRepository.findByIdAndIsDeletedFalse(regionId).orElse(null);
        if (region != null) {
            softDeleteRegion(region);
        }
    }
    
    private void softDeleteRegion(Region region) {
        region.setIsDeleted(true);
        region.setDeletedAt(LocalDateTime.now());
        regionRepository.save(region);
    }
    
    private RegionDTO buildRegionTree(Region region) {
        RegionDTO dto = convertToDTO(region);
        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(region.getId());
        
        if (!children.isEmpty()) {
            dto.setChildren(children.stream()
                    .map(this::buildRegionTree)
                    .sorted(Comparator.comparing(RegionDTO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private List<Long> getAllDescendantRegionIds(Long regionId) {
        List<Long> result = new ArrayList<>();
        result.add(regionId);
        
        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(regionId);
        for (Region child : children) {
            result.addAll(getAllDescendantRegionIds(child.getId()));
        }
        
        return result;
    }
    
    private void updateDescendantPaths(Region parent) {
        List<Region> children = regionRepository.findByParentIdAndIsDeletedFalse(parent.getId());
        for (Region child : children) {
            child.setLevel(parent.getLevel() + 1);
            child.setPath(parent.getPath() + "/" + child.getCode());
            regionRepository.save(child);
            updateDescendantPaths(child);
        }
    }
    
    private RegionDTO convertToDTO(Region region) {
        RegionDTO.RegionDTOBuilder builder = RegionDTO.builder()
                .id(region.getId())
                .code(region.getCode())
                .name(region.getName())
                .description(region.getDescription())
                .parentId(region.getParentId())
                .level(region.getLevel())
                .path(region.getPath())
                .sortOrder(region.getSortOrder())
                .createdAt(region.getCreatedAt())
                .updatedAt(region.getUpdatedAt())
                .deletedAt(region.getDeletedAt());
        
        if (!region.getIsDeleted()) {
            long cameraCount = cameraRepository.countByRegionIdAndIsDeletedFalse(region.getId());
            long onlineCount = cameraRepository.countByRegionIdAndStatusAndIsDeletedFalse(
                    region.getId(), Camera.CameraStatus.ONLINE);
            long childCount = regionRepository.countByParentId(region.getId());
            
            builder.cameraCount(cameraCount)
                    .onlineCameraCount(onlineCount)
                    .childRegionCount(childCount);
        }
        
        return builder.build();
    }
}