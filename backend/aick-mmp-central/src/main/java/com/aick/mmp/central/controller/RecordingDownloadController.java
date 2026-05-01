package com.aick.mmp.central.controller;

import com.aick.mmp.central.service.recording.RecordingDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 录像下载控制器
 */
@RestController
@RequestMapping("/recordings")
@Tag(name = "Recording Download", description = "录像下载API")
public class RecordingDownloadController {

    private static final Logger log = LoggerFactory.getLogger(RecordingDownloadController.class);

    private final RecordingDownloadService downloadService;

    public RecordingDownloadController(RecordingDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /**
     * 下载单个录像文件
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载录像文件", description = "下载指定ID的录像文件")
    @ApiResponse(responseCode = "200", description = "下载成功")
    @ApiResponse(responseCode = "404", description = "录像不存在")
    @ApiResponse(responseCode = "503", description = "并发限制或文件被锁定")
    public ResponseEntity<byte[]> downloadRecording(
            @Parameter(description = "录像ID") @PathVariable Long id) {

        log.info("开始下载录像: id={}", id);

        var downloadResult = downloadService.prepareDownload(id);
        if (downloadResult.isEmpty()) {
            log.warn("录像下载失败: id={}", id);
            return ResponseEntity.status(503).build();
        }

        try {
            var result = downloadResult.get();
            // 读取文件内容到内存
            byte[] content = result.inputStream().readAllBytes();
            
            // 释放下载资源
            downloadService.releaseDownload(id);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(result.format()));
            headers.setContentDispositionFormData("attachment", result.fileName());
            headers.setContentLength(content.length);

            log.info("录像下载成功: id={}, size={}", id, content.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);

        } catch (Exception e) {
            log.error("读取下载文件失败: id={}", id, e);
            downloadService.releaseDownload(id);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前下载状态
     */
    @GetMapping("/download/status")
    @Operation(summary = "获取下载状态", description = "获取当前活动下载的数量和列表")
    public ResponseEntity<DownloadStatus> getDownloadStatus() {
        List<RecordingDownloadService.DownloadTask> activeDownloads = downloadService.getActiveDownloads();
        
        return ResponseEntity.ok(new DownloadStatus(
                downloadService.getActiveDownloadCount(),
                activeDownloads
        ));
    }

    /**
     * 取消下载
     */
    @PostMapping("/{id}/download/cancel")
    @Operation(summary = "取消下载", description = "取消指定录像的下载任务")
    public ResponseEntity<Void> cancelDownload(
            @Parameter(description = "录像ID") @PathVariable Long id) {
        
        downloadService.releaseDownload(id);
        log.info("取消下载: id={}", id);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量下载 - 返回文件列表信息
     * 注意：实际下载需要多次调用 /recordings/{id}/download
     */
    @PostMapping("/batch-download")
    @Operation(summary = "批量下载准备", description = "批量准备下载多个录像文件")
    public ResponseEntity<BatchDownloadResponse> prepareBatchDownload(
            @RequestBody BatchDownloadRequest request) {
        
        log.info("批量下载准备: ids={}", request.recordingIds());

        List<Long> validIds = request.recordingIds().stream()
                .filter(id -> downloadService.prepareDownload(id).isPresent())
                .toList();

        return ResponseEntity.ok(new BatchDownloadResponse(
                validIds.size(),
                validIds,
                downloadService.getActiveDownloadCount()
        ));
    }

    /**
     * 根据格式获取媒体类型
     */
    private MediaType getMediaType(String format) {
        if (format == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return switch (format.toLowerCase()) {
            case "mp4" -> MediaType.parseMediaType("video/mp4");
            case "avi" -> MediaType.parseMediaType("video/x-msvideo");
            case "mkv" -> MediaType.parseMediaType("video/x-matroska");
            case "webm" -> MediaType.parseMediaType("video/webm");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * 下载状态响应
     */
    public record DownloadStatus(
            int activeCount,
            List<RecordingDownloadService.DownloadTask> tasks
    ) {}

    /**
     * 批量下载请求
     */
    public record BatchDownloadRequest(
            List<Long> recordingIds
    ) {}

    /**
     * 批量下载响应
     */
    public record BatchDownloadResponse(
            int preparedCount,
            List<Long> preparedIds,
            int activeCount
    ) {}
}
