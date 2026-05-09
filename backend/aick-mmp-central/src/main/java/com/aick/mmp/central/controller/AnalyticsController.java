package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.security.CurrentUserContext;
import com.aick.mmp.central.service.AnalyticsService;
import com.aick.mmp.central.service.ReportService;
import com.aick.mmp.shared.model.enums.AggregationLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportService reportService;
    private final CurrentUserContext currentUserContext;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 获取设备利用率统计
     */
    @GetMapping("/device-usage")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<DeviceUsageStatsDTO> getDeviceUsageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "HOUR") AggregationLevel level,
            @RequestParam(required = false) List<Long> cameraIds) {

        DeviceUsageStatsDTO stats = analyticsService.getDeviceUsageStats(startTime, endTime, level, cameraIds);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取带宽分析统计
     */
    @GetMapping("/bandwidth")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<BandwidthStatsDTO> getBandwidthStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "HOUR") AggregationLevel level) {

        BandwidthStatsDTO stats = analyticsService.getBandwidthStats(startTime, endTime, level);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取存储容量统计
     */
    @GetMapping("/storage")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<StorageStatsDTO> getStorageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") AggregationLevel level) {

        StorageStatsDTO stats = analyticsService.getStorageStats(startTime, endTime, level);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<AlertStatsDTO> getAlertStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") AggregationLevel level) {

        AlertStatsDTO stats = analyticsService.getAlertStats(startTime, endTime, level);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取通用分析数据
     */
    @PostMapping("/query")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<AnalyticsResponseDTO> queryAnalytics(@RequestBody AnalyticsRequestDTO request) {
        AnalyticsResponseDTO response = analyticsService.getAnalyticsData(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<AnalyticsResponseDTO.DataPoint>> getTrends(
            @RequestParam String dimension,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "HOUR") AggregationLevel level) {

        List<AnalyticsResponseDTO.DataPoint> trends = analyticsService.getTrendData(null, dimension, startTime, endTime, level);
        return ResponseEntity.ok(trends);
    }

    /**
     * 获取可用统计维度
     */
    @GetMapping("/dimensions")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<String>> getAvailableDimensions() {
        return ResponseEntity.ok(reportService.getAvailableDimensions());
    }

    /**
     * 导出报表
     */
    @PostMapping("/reports/export")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<byte[]> exportReport(@RequestBody ReportRequestDTO request) {
        byte[] reportData = reportService.generateReport(request);

        String filename = "report_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String contentType = switch (request.getFormat()) {
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case CSV -> "text/csv";
            case PDF -> "application/pdf";
            default -> "application/octet-stream";
        };
        String extension = request.getFormat() != null ? "." + request.getFormat().name().toLowerCase() : ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + extension)
                .contentType(MediaType.parseMediaType(contentType))
                .body(reportData);
    }

    // ==================== 报表订阅管理 ====================

    /**
     * 获取订阅列表
     */
    @GetMapping("/subscriptions")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<ReportSubscriptionDTO>> getSubscriptions() {
        Long userId = currentUserContext.getCurrentUserId();
        return ResponseEntity.ok(reportService.getSubscriptions(userId));
    }

    /**
     * 获取订阅详情
     */
    @GetMapping("/subscriptions/{id}")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ReportSubscriptionDTO> getSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getSubscription(id));
    }

    /**
     * 创建订阅
     */
    @PostMapping("/subscriptions")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ReportSubscriptionDTO> createSubscription(@RequestBody ReportSubscriptionDTO dto) {
        Long userId = currentUserContext.getCurrentUserId();
        return ResponseEntity.ok(reportService.createSubscription(dto, userId));
    }

    /**
     * 更新订阅
     */
    @PutMapping("/subscriptions/{id}")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ReportSubscriptionDTO> updateSubscription(
            @PathVariable Long id,
            @RequestBody ReportSubscriptionDTO dto) {
        return ResponseEntity.ok(reportService.updateSubscription(id, dto));
    }

    /**
     * 删除订阅
     */
    @DeleteMapping("/subscriptions/{id}")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        reportService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用/禁用订阅
     */
    @PatchMapping("/subscriptions/{id}/toggle")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ReportSubscriptionDTO> toggleSubscription(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(reportService.toggleSubscription(id, enabled));
    }

    /**
     * 手动触发报表生成
     */
    @PostMapping("/subscriptions/{id}/trigger")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<byte[]> triggerReport(@PathVariable Long id) {
        byte[] reportData = reportService.triggerReport(id);

        String filename = "subscription_report_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportData);
    }

    /**
     * 获取报表模板
     */
    @GetMapping("/report-templates")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<String>> getReportTemplates() {
        return ResponseEntity.ok(reportService.getReportTemplates());
    }
}
