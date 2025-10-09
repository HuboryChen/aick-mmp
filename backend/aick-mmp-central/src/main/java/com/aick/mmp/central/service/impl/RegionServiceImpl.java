package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.RegionDTO;
import com.aick.mmp.central.repository.RegionRepository;
import com.aick.mmp.central.service.RegionService;
import com.aick.mmp.shared.model.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl implements RegionService {
    
    private final RegionRepository regionRepository;
    
    @Override
    @Transactional
    public RegionDTO createRegion(RegionDTO regionDTO) {
        Region region = Region.builder()
                .code(regionDTO.getCode())
                .name(regionDTO.getName())
                .description(regionDTO.getDescription())
                .parentId(regionDTO.getParentId())
                .build();
        
        Region savedRegion = regionRepository.save(region);
        log.info("Created new region: {} ({})", savedRegion.getName(), savedRegion.getCode());
        return convertToDTO(savedRegion);
    }
    
    @Override
    @Transactional
    public RegionDTO updateRegion(Long id, RegionDTO regionDTO) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + id));
        
        region.setCode(regionDTO.getCode());
        region.setName(regionDTO.getName());
        region.setDescription(regionDTO.getDescription());
        region.setParentId(regionDTO.getParentId());
        
        Region updatedRegion = regionRepository.save(region);
        log.info("Updated region: {} ({})", updatedRegion.getName(), updatedRegion.getCode());
        return convertToDTO(updatedRegion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public RegionDTO getRegionById(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + id));
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
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RegionDTO> getChildRegions(Long parentId) {
        return regionRepository.findByParentId(parentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteRegion(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new RuntimeException("Region not found with id: " + id);
        }
        regionRepository.deleteById(id);
        log.info("Deleted region with id: {}", id);
    }
    
    private RegionDTO convertToDTO(Region region) {
        return RegionDTO.builder()
                .id(region.getId())
                .code(region.getCode())
                .name(region.getName())
                .description(region.getDescription())
                .parentId(region.getParentId())
                .createdAt(region.getCreatedAt())
                .updatedAt(region.getUpdatedAt())
                .build();
    }
}