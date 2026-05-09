package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.CameraDTO;
import com.aick.mmp.central.dto.CameraImportDTO;
import com.aick.mmp.central.dto.ImportProgressDTO;
import com.aick.mmp.central.dto.ImportTaskDTO;
import com.aick.mmp.central.dto.ValidationErrorDTO;
import com.aick.mmp.central.repository.CameraBatchImportTaskRepository;
import com.aick.mmp.central.service.CameraBatchImportService;
import com.aick.mmp.central.service.CameraConfigTemplateService;
import com.aick.mmp.central.service.CameraService;
import com.aick.mmp.central.service.RegionService;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.CameraBatchImportTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraBatchImportServiceImpl implements CameraBatchImportService {

    private final CameraBatchImportTaskRepository repository;
    private final CameraService cameraService;
    private final CameraConfigTemplateService templateService;
    private final RegionService regionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${camera-batch-import.batch-size:50}")
    private int batchSize;

    @Value("${camera-batch-import.batch-delay-ms:100}")
    private int batchDelayMs;

    private final ConcurrentHashMap<Long, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final String[] TEMPLATE_HEADERS = {
            "摄像头名称", "品牌", "型号", "IP地址", "端口", "所属区域", "用户名", "密码", "分辨率", "描述"
    };

    @Override
    public byte[] getImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("摄像头导入模板");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] exampleData = {
                    {"门口摄像头-01", "海康威视", "DS-2CD2T45D-I5", "192.168.1.100", "554", "总部-1楼", "admin", "password123", "1920x1080", "大门口监控"},
                    {"仓库摄像头-01", "大华", "DH-IPC-HFW2431T-ZS", "192.168.1.101", "554", "总部-仓库", "admin", "password123", "1920x1080", "仓库区域监控"}
            };

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);

            for (int r = 0; r < exampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < exampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(exampleData[r][c]);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate import template", e);
        }
    }

    @Override
    @Transactional
    public Long startImport(MultipartFile file, Long userId) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
        if (!"xlsx".equals(extension) && !"csv".equals(extension)) {
            throw new IllegalArgumentException("Only .xlsx and .csv files are supported");
        }

        CameraBatchImportTask task = CameraBatchImportTask.builder()
                .userId(userId)
                .fileName(fileName)
                .status("PENDING")
                .totalRecords(0)
                .successCount(0)
                .failCount(0)
                .build();

        CameraBatchImportTask saved = repository.save(task);
        executeImport(saved.getId(), file, userId);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ImportProgressDTO getImportProgress(Long taskId) {
        CameraBatchImportTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Import task not found: " + taskId));
        return toImportProgressDTO(task);
    }

    @Override
    @Transactional
    public void cancelImport(Long taskId) {
        CameraBatchImportTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Import task not found: " + taskId));

        if (!"VALIDATING".equals(task.getStatus()) && !"IMPORTING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot cancel task with status: " + task.getStatus());
        }

        cancellationFlags.put(taskId, true);
        task.setStatus("CANCELLED");
        task.setCompletedAt(LocalDateTime.now());
        repository.save(task);

        pushProgress(toImportProgressDTO(task));
        log.info("Cancelled import task: {}", taskId);
    }

    @Override
    public byte[] downloadErrorReport(Long taskId) {
        CameraBatchImportTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Import task not found: " + taskId));

        if (task.getErrorDetails() == null || task.getErrorDetails().isEmpty()) {
            throw new IllegalStateException("No errors in this import task");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入错误报告");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"行号", "字段", "错误信息"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            @SuppressWarnings("unchecked")
            List<ValidationErrorDTO> errors = objectMapper.readValue(task.getErrorDetails(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ValidationErrorDTO.class));

            for (int i = 0; i < errors.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ValidationErrorDTO error = errors.get(i);
                row.createCell(0).setCellValue(error.getRowNumber());
                row.createCell(1).setCellValue(error.getFieldName());
                row.createCell(2).setCellValue(error.getErrorMessage());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate error report", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportTaskDTO> getImportHistory(Pageable pageable, Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toImportTaskDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationErrorDTO> validateImportData(List<CameraImportDTO> data) {
        List<ValidationErrorDTO> errors = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            CameraImportDTO dto = data.get(i);
            int rowNum = i + 1;

            if (dto.getCameraName() == null || dto.getCameraName().trim().isEmpty()) {
                errors.add(createError(rowNum, "摄像头名称", "摄像头名称不能为空"));
            } else if (dto.getCameraName().length() < 2 || dto.getCameraName().length() > 50) {
                errors.add(createError(rowNum, "摄像头名称", "摄像头名称长度需为2-50字符"));
            }

            if (dto.getBrand() == null || dto.getBrand().trim().isEmpty()) {
                errors.add(createError(rowNum, "品牌", "品牌不能为空"));
            }

            if (dto.getModel() == null || dto.getModel().trim().isEmpty()) {
                errors.add(createError(rowNum, "型号", "型号不能为空"));
            }

            if (dto.getIp() == null || !IP_PATTERN.matcher(dto.getIp()).matches()) {
                errors.add(createError(rowNum, "IP地址", "IP地址格式不正确"));
            }

            if (dto.getRegionName() == null || dto.getRegionName().trim().isEmpty()) {
                errors.add(createError(rowNum, "所属区域", "所属区域不能为空"));
            }
        }
        return errors;
    }

    @Async
    public void executeImport(Long taskId, MultipartFile file, Long userId) {
        CameraBatchImportTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Import task not found: " + taskId));

        try {
            task.setStatus("VALIDATING");
            repository.save(task);

            List<CameraImportDTO> records = parseFile(file);
            task.setTotalRecords(records.size());
            repository.save(task);

            List<ValidationErrorDTO> errors = validateImportData(records);
            if (!errors.isEmpty()) {
                task.setStatus("FAILED");
                task.setFailCount(errors.size());
                task.setErrorDetails(objectMapper.writeValueAsString(errors));
                task.setCompletedAt(LocalDateTime.now());
                repository.save(task);
                log.warn("Import task {} validation failed: {} errors", taskId, errors.size());
                return;
            }

            task.setStatus("IMPORTING");
            repository.save(task);

            int successCount = 0;
            int failCount = 0;
            List<ValidationErrorDTO> importErrors = new ArrayList<>();

            for (int i = 0; i < records.size(); i += batchSize) {
                if (cancellationFlags.getOrDefault(taskId, false)) {
                    cancellationFlags.remove(taskId);
                    return;
                }

                int batchEnd = Math.min(i + batchSize, records.size());
                List<CameraImportDTO> batch = records.subList(i, batchEnd);

                List<CameraDTO> cameraBatch = new ArrayList<>();
                for (int j = 0; j < batch.size(); j++) {
                    CameraImportDTO dto = batch.get(j);
                    try {
                        CameraDTO cameraDTO = buildCameraDTO(dto, userId);
                        cameraBatch.add(cameraDTO);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        importErrors.add(createError(i + j + 1, "导入", e.getMessage()));
                    }
                }

                if (!cameraBatch.isEmpty()) {
                    for (CameraDTO cd : cameraBatch) {
                        try {
                            cameraService.createCamera(cd);
                        } catch (Exception e) {
                            failCount++;
                            successCount--;
                            importErrors.add(createError(0, "导入", e.getMessage()));
                        }
                    }
                }

                task.setSuccessCount(successCount);
                task.setFailCount(failCount);
                repository.save(task);

                ImportProgressDTO progress = ImportProgressDTO.builder()
                        .taskId(taskId)
                        .status("IMPORTING")
                        .progress((i + batchSize) * 100 / records.size())
                        .totalRecords(records.size())
                        .successCount(successCount)
                        .failCount(failCount)
                        .build();
                pushProgress(progress);

                Thread.sleep(batchDelayMs);
            }

            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
            if (!importErrors.isEmpty()) {
                task.setErrorDetails(objectMapper.writeValueAsString(importErrors));
            }
            repository.save(task);

            pushProgress(ImportProgressDTO.builder()
                    .taskId(taskId)
                    .status("COMPLETED")
                    .progress(100)
                    .totalRecords(records.size())
                    .successCount(successCount)
                    .failCount(failCount)
                    .build());

            log.info("Import task {} completed: {} success, {} failed", taskId, successCount, failCount);
        } catch (Exception e) {
            log.error("Import task {} failed: {}", taskId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setCompletedAt(LocalDateTime.now());
            repository.save(task);
        } finally {
            cancellationFlags.remove(taskId);
        }
    }

    private CameraDTO buildCameraDTO(CameraImportDTO dto, Long userId) {
        String connectionUrl = buildConnectionUrl(dto);
        Camera.Protocol protocol = dto.getPort() != null && dto.getPort() == 80
                ? Camera.Protocol.HTTP : Camera.Protocol.RTSP;

        CameraDTO.CameraDTOBuilder builder = CameraDTO.builder()
                .name(dto.getCameraName().trim())
                .protocol(protocol)
                .connectionUrl(connectionUrl)
                .resolution(dto.getResolution() != null ? dto.getResolution() : "1920x1080")
                .username(dto.getUsername() != null ? dto.getUsername().trim() : null)
                .password(dto.getPassword() != null ? dto.getPassword().trim() : null)
                .status(Camera.CameraStatus.OFFLINE)
                .location(dto.getDescription());

        if (dto.getRegionName() != null && !dto.getRegionName().isEmpty()) {
            Long regionId = resolveRegionId(dto.getRegionName().trim(), userId);
            if (regionId != null) {
                builder.regionId(regionId);
            }
        }

        return builder.build();
    }

    private String buildConnectionUrl(CameraImportDTO dto) {
        var match = templateService.matchTemplate(dto.getBrand(), dto.getModel());
        if (match != null) {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("ip", dto.getIp());
            params.put("port", String.valueOf(dto.getPort() != null ? dto.getPort() : match.getDefaultPort()));
            params.put("username", dto.getUsername() != null ? dto.getUsername() : "admin");
            params.put("password", dto.getPassword() != null ? dto.getPassword() : "");
            params.put("channel", "1");
            return templateService.generateUrl(match.getId(), params);
        }
        return "rtsp://" + dto.getIp() + ":" + (dto.getPort() != null ? dto.getPort() : 554);
    }

    private Long resolveRegionId(String regionName, Long userId) {
        try {
            var regions = regionService.searchRegions(regionName);
            if (!regions.isEmpty()) {
                return regions.get(0).getId();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve region name '{}': {}", regionName, e.getMessage());
        }
        return null;
    }

    private List<CameraImportDTO> parseFile(MultipartFile file) {
        String extension = file.getOriginalFilename()
                .substring(file.getOriginalFilename().lastIndexOf(".") + 1).toLowerCase();

        if ("csv".equals(extension)) {
            return parseCsv(file);
        } else {
            return parseExcel(file);
        }
    }

    private List<CameraImportDTO> parseExcel(MultipartFile file) {
        List<CameraImportDTO> records = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                CameraImportDTO dto = CameraImportDTO.builder()
                        .cameraName(getCellStringValue(row.getCell(0)))
                        .brand(getCellStringValue(row.getCell(1)))
                        .model(getCellStringValue(row.getCell(2)))
                        .ip(getCellStringValue(row.getCell(3)))
                        .port(getCellNumericValue(row.getCell(4)))
                        .regionName(getCellStringValue(row.getCell(5)))
                        .username(getCellStringValue(row.getCell(6)))
                        .password(getCellStringValue(row.getCell(7)))
                        .resolution(getCellStringValue(row.getCell(8)))
                        .description(getCellStringValue(row.getCell(9)))
                        .build();

                if (dto.getCameraName() != null && !dto.getCameraName().isEmpty()) {
                    records.add(dto);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file", e);
        }
        return records;
    }

    private List<CameraImportDTO> parseCsv(MultipartFile file) {
        List<CameraImportDTO> records = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return records;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                CameraImportDTO dto = CameraImportDTO.builder()
                        .cameraName(fields.length > 0 ? fields[0].trim().replaceAll("^\"|\"$", "") : null)
                        .brand(fields.length > 1 ? fields[1].trim().replaceAll("^\"|\"$", "") : null)
                        .model(fields.length > 2 ? fields[2].trim().replaceAll("^\"|\"$", "") : null)
                        .ip(fields.length > 3 ? fields[3].trim().replaceAll("^\"|\"$", "") : null)
                        .port(fields.length > 4 && !fields[4].trim().isEmpty() ? Integer.parseInt(fields[4].trim()) : null)
                        .regionName(fields.length > 5 ? fields[5].trim().replaceAll("^\"|\"$", "") : null)
                        .username(fields.length > 6 ? fields[6].trim().replaceAll("^\"|\"$", "") : null)
                        .password(fields.length > 7 ? fields[7].trim().replaceAll("^\"|\"$", "") : null)
                        .resolution(fields.length > 8 ? fields[8].trim().replaceAll("^\"|\"$", "") : null)
                        .description(fields.length > 9 ? fields[9].trim().replaceAll("^\"|\"$", "") : null)
                        .build();

                if (dto.getCameraName() != null && !dto.getCameraName().isEmpty()) {
                    records.add(dto);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file", e);
        }
        return records;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private void pushProgress(ImportProgressDTO progress) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/import/" + progress.getTaskId(),
                    objectMapper.writeValueAsString(progress)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize import progress: {}", e.getMessage());
        }
    }

    private ValidationErrorDTO createError(int rowNumber, String fieldName, String errorMessage) {
        return ValidationErrorDTO.builder()
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .errorMessage(errorMessage)
                .build();
    }

    private ImportProgressDTO toImportProgressDTO(CameraBatchImportTask task) {
        int total = task.getTotalRecords() != null ? task.getTotalRecords() : 0;
        int success = task.getSuccessCount() != null ? task.getSuccessCount() : 0;
        int fail = task.getFailCount() != null ? task.getFailCount() : 0;
        int progress = total > 0 ? (success + fail) * 100 / total : 0;

        return ImportProgressDTO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .progress(progress)
                .totalRecords(total)
                .successCount(success)
                .failCount(fail)
                .build();
    }

    private ImportTaskDTO toImportTaskDTO(CameraBatchImportTask entity) {
        return ImportTaskDTO.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .status(entity.getStatus())
                .totalRecords(entity.getTotalRecords())
                .successCount(entity.getSuccessCount())
                .failCount(entity.getFailCount())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
