package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CameraConfigTemplateDTO;
import com.aick.mmp.central.dto.CreateTemplateRequestDTO;
import com.aick.mmp.central.dto.UpdateTemplateRequestDTO;
import com.aick.mmp.central.service.CameraConfigTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camera-config-templates")
@RequiredArgsConstructor
public class CameraConfigTemplateController {

    private final CameraConfigTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<CameraConfigTemplateDTO>> getTemplates(
            Pageable pageable,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String protocol) {
        return ResponseEntity.ok(templateService.getTemplates(pageable, brand, protocol));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CameraConfigTemplateDTO> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CameraConfigTemplateDTO> createTemplate(
            @Valid @RequestBody CreateTemplateRequestDTO request) {
        return new ResponseEntity<>(templateService.createTemplate(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CameraConfigTemplateDTO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequestDTO request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<String> generateUrl(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(templateService.generateUrl(id, params));
    }

    @PostMapping("/match")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CameraConfigTemplateDTO> matchTemplate(
            @RequestParam String brand,
            @RequestParam String model) {
        CameraConfigTemplateDTO result = templateService.matchTemplate(brand, model);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CameraConfigTemplateDTO>> importTemplates(
            @RequestBody List<CreateTemplateRequestDTO> templates) {
        return new ResponseEntity<>(templateService.importTemplates(templates), HttpStatus.CREATED);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CameraConfigTemplateDTO>> exportTemplates(
            @RequestParam(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(templateService.getPresetTemplates());
        }
        return ResponseEntity.ok(templateService.exportTemplates(ids));
    }

    @GetMapping("/brands")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<String>> getBrands() {
        return ResponseEntity.ok(templateService.getBrands());
    }
}
