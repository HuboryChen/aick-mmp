package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.CameraImportDTO;
import com.aick.mmp.central.dto.ImportProgressDTO;
import com.aick.mmp.central.dto.ImportTaskDTO;
import com.aick.mmp.central.dto.ValidationErrorDTO;
import com.aick.mmp.central.service.CameraBatchImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camera-batch-import")
@RequiredArgsConstructor
public class CameraBatchImportController {

    private final CameraBatchImportService importService;

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Resource> downloadTemplate() {
        byte[] data = importService.getImportTemplate();
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=camera-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> startImport(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Long taskId = importService.startImport(file, userId);
        return new ResponseEntity<>(Map.of("taskId", taskId), HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ImportProgressDTO> getImportProgress(@PathVariable Long taskId) {
        return ResponseEntity.ok(importService.getImportProgress(taskId));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelImport(@PathVariable Long taskId) {
        importService.cancelImport(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/errors")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Resource> downloadErrorReport(@PathVariable Long taskId) {
        byte[] data = importService.downloadErrorReport(taskId);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=import-error-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<ImportTaskDTO>> getImportHistory(
            Pageable pageable,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(importService.getImportHistory(pageable, userId));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<ValidationErrorDTO>> validateImportData(
            @RequestBody List<CameraImportDTO> data) {
        return ResponseEntity.ok(importService.validateImportData(data));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.aick.mmp.central.security.CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return 0L;
    }
}
