package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CameraConfigTemplateDTO;
import com.aick.mmp.central.dto.CreateTemplateRequestDTO;
import com.aick.mmp.central.dto.UpdateTemplateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CameraConfigTemplateService {

    Page<CameraConfigTemplateDTO> getTemplates(Pageable pageable, String brand, String protocol);

    CameraConfigTemplateDTO getTemplateById(Long id);

    CameraConfigTemplateDTO createTemplate(CreateTemplateRequestDTO request);

    CameraConfigTemplateDTO updateTemplate(Long id, UpdateTemplateRequestDTO request);

    void deleteTemplate(Long id);

    String generateUrl(Long id, Map<String, String> params);

    CameraConfigTemplateDTO matchTemplate(String brand, String model);

    void incrementUsage(Long id);

    List<CameraConfigTemplateDTO> importTemplates(List<CreateTemplateRequestDTO> templates);

    List<CameraConfigTemplateDTO> exportTemplates(List<Long> ids);

    List<CameraConfigTemplateDTO> getPresetTemplates();

    List<String> getBrands();
}
