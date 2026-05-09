package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.central.repository.ReportSubscriptionRepository;
import com.aick.mmp.central.service.AnalyticsService;
import com.aick.mmp.central.service.ReportService;
import com.aick.mmp.shared.model.ReportSubscription;
import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.ReportType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportSubscriptionRepository subscriptionRepository;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public byte[] generateReport(ReportRequestDTO request) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            generateReportToStream(request, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate report", e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    @Override
    public void generateReportToStream(ReportRequestDTO request, OutputStream outputStream) {
        try {
            switch (request.getFormat()) {
                case EXCEL -> generateExcelReport(request, outputStream);
                case CSV -> generateCsvReport(request, outputStream);
                default -> throw new IllegalArgumentException("Unsupported format: " + request.getFormat());
            }
        } catch (IOException e) {
            log.error("Failed to generate report to stream", e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    @Override
    public List<ReportSubscriptionDTO> getSubscriptions(Long userId) {
        List<ReportSubscription> subscriptions;
        if (userId != null) {
            subscriptions = subscriptionRepository.findByCreatedBy(userId);
        } else {
            subscriptions = subscriptionRepository.findAll();
        }
        return subscriptions.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public ReportSubscriptionDTO getSubscription(Long id) {
        ReportSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
        return convertToDTO(subscription);
    }

    @Override
    @Transactional
    public ReportSubscriptionDTO createSubscription(ReportSubscriptionDTO dto, Long userId) {
        ReportSubscription subscription = new ReportSubscription();
        subscription.setName(dto.getName());
        subscription.setReportType(dto.getReportType());
        subscription.setFormat(dto.getFormat());
        subscription.setDimensions(toJson(dto.getDimensions()));
        subscription.setFilters(dto.getFilters());
        subscription.setRecipients(toJson(dto.getRecipients()));
        subscription.setNextSendTime(calculateNextSendTime(dto.getReportType()));
        subscription.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        subscription.setCreatedBy(userId);

        ReportSubscription saved = subscriptionRepository.save(subscription);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ReportSubscriptionDTO updateSubscription(Long id, ReportSubscriptionDTO dto) {
        ReportSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));

        if (dto.getName() != null) subscription.setName(dto.getName());
        if (dto.getReportType() != null) subscription.setReportType(dto.getReportType());
        if (dto.getFormat() != null) subscription.setFormat(dto.getFormat());
        if (dto.getDimensions() != null) subscription.setDimensions(toJson(dto.getDimensions()));
        if (dto.getFilters() != null) subscription.setFilters(dto.getFilters());
        if (dto.getRecipients() != null) subscription.setRecipients(toJson(dto.getRecipients()));
        if (dto.getEnabled() != null) subscription.setEnabled(dto.getEnabled());

        // 如果报告类型改变，重新计算下次发送时间
        if (dto.getReportType() != null) {
            subscription.setNextSendTime(calculateNextSendTime(dto.getReportType()));
        }

        ReportSubscription updated = subscriptionRepository.save(subscription);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteSubscription(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("Subscription not found: " + id);
        }
        subscriptionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ReportSubscriptionDTO toggleSubscription(Long id, boolean enabled) {
        ReportSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
        subscription.setEnabled(enabled);
        ReportSubscription updated = subscriptionRepository.save(subscription);
        return convertToDTO(updated);
    }

    @Override
    public byte[] triggerReport(Long subscriptionId) {
        ReportSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        ReportRequestDTO request = new ReportRequestDTO();
        request.setReportType(subscription.getReportType());
        request.setFormat(subscription.getFormat());
        request.setStartTime(LocalDateTime.now().minusDays(30));
        request.setEndTime(LocalDateTime.now());
        request.setDimensions(fromJson(subscription.getDimensions(), new TypeReference<List<String>>() {}));

        return generateReport(request);
    }

    @Override
    public void sendSubscriptionReports() {
        List<ReportSubscription> dueSubscriptions = subscriptionRepository.findDueSubscriptions(LocalDateTime.now());

        for (ReportSubscription subscription : dueSubscriptions) {
            try {
                byte[] reportData = triggerReport(subscription.getId());
                // 实际发送邮件逻辑将通过NotificationService实现
                log.info("Generated report for subscription: {}", subscription.getName());

                // 更新下次发送时间
                subscription.setLastSendTime(LocalDateTime.now());
                subscription.setNextSendTime(calculateNextSendTime(subscription.getReportType()));
                subscriptionRepository.save(subscription);
            } catch (Exception e) {
                log.error("Failed to send subscription report: {}", subscription.getName(), e);
            }
        }
    }

    @Override
    public List<String> getAvailableDimensions() {
        return Arrays.asList(
                "device_usage",
                "network_bandwidth",
                "storage_capacity",
                "alert_count",
                "stream_quality",
                "user_activity",
                "api_usage"
        );
    }

    @Override
    public List<String> getReportTemplates() {
        return Arrays.asList(
                "daily_summary",
                "weekly_summary",
                "monthly_summary",
                "device_status",
                "bandwidth_usage",
                "storage_analysis",
                "alert_report",
                "custom"
        );
    }

    // ==================== 私有方法 ====================

    private void generateExcelReport(ReportRequestDTO request, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据分析报告");

            // 创建标题样式
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 0;

            // 标题行
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(request.getTitle() != null ? request.getTitle() : "数据分析报告");
            titleCell.setCellStyle(titleStyle);

            // 时间范围
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("统计周期：");
            dateRow.createCell(1).setCellValue(
                    request.getStartTime().format(DATE_FORMATTER) + " 至 " + request.getEndTime().format(DATE_FORMATTER));

            rowNum++; // 空行

            // 根据维度生成数据
            List<String> dimensions = request.getDimensions();
            if (dimensions == null || dimensions.isEmpty()) {
                dimensions = getAvailableDimensions();
            }

            for (String dimension : dimensions) {
                // 分隔行
                Row separatorRow = sheet.createRow(rowNum++);
                Cell sepCell = separatorRow.createCell(0);
                sepCell.setCellValue(getDimensionTitle(dimension));
                sepCell.setCellStyle(titleStyle);

                // 表头
                Row headerRow = sheet.createRow(rowNum++);
                String[] headers = getDimensionHeaders(dimension);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 数据行
                List<AnalyticsResponseDTO.DataPoint> data = analyticsService
                        .getTrendData(null, dimension, request.getStartTime(), request.getEndTime(), AggregationLevel.HOUR);

                for (AnalyticsResponseDTO.DataPoint point : data) {
                    Row dataRow = sheet.createRow(rowNum++);
                    writeDataRow(dataRow, dimension, point);
                }

                rowNum++; // 空行
            }

            workbook.write(outputStream);
        }
    }

    private void generateCsvReport(ReportRequestDTO request, OutputStream outputStream) throws IOException {
        StringBuilder csv = new StringBuilder();

        // CSV标题
        csv.append("维度,时间,值\n");

        List<String> dimensions = request.getDimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            dimensions = getAvailableDimensions();
        }

        for (String dimension : dimensions) {
            List<AnalyticsResponseDTO.DataPoint> data = analyticsService
                    .getTrendData(null, dimension, request.getStartTime(), request.getEndTime(), AggregationLevel.HOUR);

            for (AnalyticsResponseDTO.DataPoint point : data) {
                csv.append(String.format("%s,%s,%.2f\n",
                        dimension,
                        point.getTimestamp().format(DATE_FORMATTER),
                        point.getValue()));
            }
        }

        outputStream.write(csv.toString().getBytes());
    }

    private String getDimensionTitle(String dimension) {
        return switch (dimension) {
            case "device_usage" -> "设备利用率";
            case "network_bandwidth" -> "网络带宽";
            case "storage_capacity" -> "存储容量";
            case "alert_count" -> "告警统计";
            case "stream_quality" -> "流质量";
            default -> dimension;
        };
    }

    private String[] getDimensionHeaders(String dimension) {
        return switch (dimension) {
            case "device_usage" -> new String[]{"时间", "在线率(%)", "离线率(%)", "故障率(%)"};
            case "network_bandwidth" -> new String[]{"时间", "带宽(Mbps)"};
            case "storage_capacity" -> new String[]{"时间", "已用存储(GB)", "使用率(%)"};
            case "alert_count" -> new String[]{"时间", "告警数", "已处理", "待处理"};
            default -> new String[]{"时间", "值"};
        };
    }

    private void writeDataRow(Row row, String dimension, AnalyticsResponseDTO.DataPoint point) {
        row.createCell(0).setCellValue(point.getTimestamp().format(DATE_FORMATTER));
        row.createCell(1).setCellValue(point.getValue());

        if ("device_usage".equals(dimension)) {
            row.createCell(2).setCellValue(100 - point.getValue()); // 离线率
            row.createCell(3).setCellValue(0.0); // 故障率
        }
    }

    private LocalDateTime calculateNextSendTime(ReportType reportType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (reportType) {
            case DAILY -> now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
            case WEEKLY -> now.plusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            case MONTHLY -> now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            default -> now.plusDays(1);
        };
    }

    private ReportSubscriptionDTO convertToDTO(ReportSubscription subscription) {
        ReportSubscriptionDTO dto = new ReportSubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setName(subscription.getName());
        dto.setReportType(subscription.getReportType());
        dto.setFormat(subscription.getFormat());
        dto.setDimensions(fromJson(subscription.getDimensions(), new TypeReference<List<String>>() {}));
        dto.setFilters(subscription.getFilters());
        dto.setRecipients(fromJson(subscription.getRecipients(), new TypeReference<List<String>>() {}));
        dto.setNextSendTime(subscription.getNextSendTime());
        dto.setLastSendTime(subscription.getLastSendTime());
        dto.setEnabled(subscription.getEnabled());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (Exception e) {
            log.error("Failed to convert to JSON", e);
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return json != null ? objectMapper.readValue(json, typeRef) : null;
        } catch (Exception e) {
            log.error("Failed to parse JSON", e);
            return null;
        }
    }
}
