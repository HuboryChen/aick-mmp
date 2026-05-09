package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.RegionDTO;
import com.aick.mmp.central.dto.RegionMoveDTO;
import com.aick.mmp.central.dto.RegionStatsDTO;
import com.aick.mmp.central.service.RegionService;
import com.aick.mmp.shared.model.Camera;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/regions")
@RequiredArgsConstructor
public class RegionController {
    
    private final RegionService regionService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionDTO> createRegion(@Valid @RequestBody RegionDTO regionDTO) {
        RegionDTO created = regionService.createRegion(regionDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionDTO> updateRegion(@PathVariable Long id, @Valid @RequestBody RegionDTO regionDTO) {
        RegionDTO updated = regionService.updateRegion(id, regionDTO);
        return ResponseEntity.ok(updated);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<RegionDTO> getRegionById(@PathVariable Long id) {
        RegionDTO region = regionService.getRegionById(id);
        return ResponseEntity.ok(region);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<RegionDTO>> getAllRegions(
            @RequestParam(required = false) Boolean flat,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer level,
            Pageable pageable) {
        
        Page<RegionDTO> regions;
        if (flat != null && flat) {
            List<RegionDTO> flatList = regionService.getRegionsFlat();
            regions = Page.empty();
        } else if (search != null && !search.isEmpty()) {
            List<RegionDTO> searchResults = regionService.searchRegions(search);
            regions = Page.empty();
        } else if (level != null) {
            List<RegionDTO> levelResults = regionService.getRegionsByLevel(level);
            regions = Page.empty();
        } else {
            regions = regionService.getAllRegions(pageable);
        }
        
        return ResponseEntity.ok(regions);
    }
    
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> getAllRegionsList() {
        List<RegionDTO> regions = regionService.getAllRegions();
        return ResponseEntity.ok(regions);
    }
    
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> getRegionTree() {
        List<RegionDTO> tree = regionService.getRegionTree();
        return ResponseEntity.ok(tree);
    }
    
    @GetMapping("/flat")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> getRegionsFlat() {
        List<RegionDTO> flatList = regionService.getRegionsFlat();
        return ResponseEntity.ok(flatList);
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> searchRegions(@RequestParam String keyword) {
        List<RegionDTO> results = regionService.searchRegions(keyword);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> getRegionsByLevel(@PathVariable Integer level) {
        List<RegionDTO> regions = regionService.getRegionsByLevel(level);
        return ResponseEntity.ok(regions);
    }
    
    @GetMapping("/children/{parentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<RegionDTO>> getChildRegions(@PathVariable Long parentId) {
        List<RegionDTO> regions = regionService.getChildRegions(parentId);
        return ResponseEntity.ok(regions);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRegion(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        regionService.deleteRegion(id, force);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionDTO> moveRegion(
            @PathVariable Long id,
            @RequestBody RegionMoveDTO moveDTO) {
        RegionDTO moved = regionService.moveRegion(id, moveDTO);
        return ResponseEntity.ok(moved);
    }
    
    @GetMapping("/{id}/cameras")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Camera>> getRegionCameras(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        List<Camera> cameras = regionService.getRegionCameras(id, recursive);
        return ResponseEntity.ok(cameras);
    }
    
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<RegionStatsDTO> getRegionStats(@PathVariable Long id) {
        RegionStatsDTO stats = regionService.getRegionStats(id);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/cameras/{cameraId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignCameraToRegion(
            @PathVariable Long cameraId,
            @RequestParam Long regionId) {
        regionService.assignCameraToRegion(cameraId, regionId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/cameras/{cameraId}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeCameraFromRegion(@PathVariable Long cameraId) {
        regionService.removeCameraFromRegion(cameraId);
        return ResponseEntity.ok().build();
    }
}