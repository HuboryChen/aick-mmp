package com.aick.mmp.central.controller;

import com.aick.mmp.central.service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "uploads") String prefix) {
        String objectName = minioService.uploadSnapshot(file, prefix);
        return ResponseEntity.ok(Map.of(
                "objectName", objectName,
                "url", "/api/v1/files/download?objectName=" + objectName
        ));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String objectName) {
        var inputStream = minioService.getSnapshot(objectName);
        String contentType = detectContentType(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/presigned")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestParam String objectName,
            @RequestParam(defaultValue = "60") int expiryMinutes) {
        String url = minioService.getPresignedUrl(objectName, expiryMinutes);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@RequestParam String objectName) {
        minioService.deleteSnapshot(objectName);
        return ResponseEntity.noContent().build();
    }

    private String detectContentType(String objectName) {
        if (objectName == null) return "application/octet-stream";
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }
}
