package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraImportDTO;
import com.aick.mmp.central.dto.ImportProgressDTO;
import com.aick.mmp.central.dto.ImportTaskDTO;
import com.aick.mmp.central.dto.ValidationErrorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CameraBatchImportService {

    byte[] getImportTemplate();

    Long startImport(MultipartFile file, Long userId);

    ImportProgressDTO getImportProgress(Long taskId);

    void cancelImport(Long taskId);

    byte[] downloadErrorReport(Long taskId);

    Page<ImportTaskDTO> getImportHistory(Pageable pageable, Long userId);

    List<ValidationErrorDTO> validateImportData(List<CameraImportDTO> data);
}
