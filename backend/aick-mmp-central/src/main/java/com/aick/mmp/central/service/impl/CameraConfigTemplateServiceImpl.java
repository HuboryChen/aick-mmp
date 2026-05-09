package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.CameraConfigTemplateDTO;
import com.aick.mmp.central.dto.CreateTemplateRequestDTO;
import com.aick.mmp.central.dto.UpdateTemplateRequestDTO;
import com.aick.mmp.central.repository.CameraConfigTemplateRepository;
import com.aick.mmp.central.service.CameraConfigTemplateService;
import com.aick.mmp.shared.model.CameraConfigTemplate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraConfigTemplateServiceImpl implements CameraConfigTemplateService {

    private final CameraConfigTemplateRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<CameraConfigTemplateDTO> getTemplates(Pageable pageable, String brand, String protocol) {
        List<CameraConfigTemplate> filtered;

        if (brand != null && !brand.isEmpty() && protocol != null && !protocol.isEmpty()) {
            filtered = repository.findByBrandAndIsDeletedFalse(brand).stream()
                    .filter(t -> t.getProtocol().equalsIgnoreCase(protocol))
                    .collect(Collectors.toList());
        } else if (brand != null && !brand.isEmpty()) {
            filtered = repository.findByBrandAndIsDeletedFalse(brand);
        } else if (protocol != null && !protocol.isEmpty()) {
            filtered = repository.findByProtocolAndIsDeletedFalse(protocol);
        } else {
            return repository.findByIsDeletedFalse(pageable).map(this::toDTO);
        }

        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = (int) Math.min(start + pageable.getPageSize(), filtered.size());
        List<CameraConfigTemplateDTO> pageContent = filtered.subList(start, end).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CameraConfigTemplateDTO getTemplateById(Long id) {
        return toDTO(findById(id));
    }

    @Override
    @Transactional
    public CameraConfigTemplateDTO createTemplate(CreateTemplateRequestDTO request) {
        CameraConfigTemplate template = CameraConfigTemplate.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .protocol(request.getProtocol())
                .defaultPort(request.getDefaultPort())
                .urlPathTemplate(request.getUrlPathTemplate())
                .presetParameters(request.getPresetParameters())
                .isPreset(false)
                .usageCount(0)
                .isDeleted(false)
                .build();

        CameraConfigTemplate saved = repository.save(template);
        log.info("Created camera config template: {} {} ({})", saved.getBrand(), saved.getModel(), saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CameraConfigTemplateDTO updateTemplate(Long id, UpdateTemplateRequestDTO request) {
        CameraConfigTemplate template = findById(id);

        if (request.getBrand() != null) template.setBrand(request.getBrand());
        if (request.getModel() != null) template.setModel(request.getModel());
        if (request.getProtocol() != null) template.setProtocol(request.getProtocol());
        if (request.getDefaultPort() != null) template.setDefaultPort(request.getDefaultPort());
        if (request.getUrlPathTemplate() != null) template.setUrlPathTemplate(request.getUrlPathTemplate());
        if (request.getPresetParameters() != null) template.setPresetParameters(request.getPresetParameters());

        CameraConfigTemplate saved = repository.save(template);
        log.info("Updated camera config template: {}", id);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        CameraConfigTemplate template = findById(id);
        template.setIsDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        repository.save(template);
        log.info("Soft-deleted camera config template: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateUrl(Long id, Map<String, String> params) {
        CameraConfigTemplate template = findById(id);
        String url = template.getUrlPathTemplate();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }

        if (!params.containsKey("port")) {
            url = url.replace("{port}", String.valueOf(template.getDefaultPort()));
        }

        return url;
    }

    @Override
    @Transactional(readOnly = true)
    public CameraConfigTemplateDTO matchTemplate(String brand, String model) {
        // 1. 精确匹配: brand + model
        if (brand != null && model != null) {
            var exact = repository.findByBrandAndModelAndIsDeletedFalse(brand, model);
            if (exact.isPresent()) {
                return toDTO(exact.get());
            }
        }

        // 2. 模糊匹配: brand only
        if (brand != null) {
            List<CameraConfigTemplate> brandTemplates = repository.findByBrandAndIsDeletedFalse(brand);
            if (!brandTemplates.isEmpty()) {
                return toDTO(brandTemplates.get(0));
            }
        }

        return null;
    }

    @Override
    @Transactional
    public void incrementUsage(Long id) {
        CameraConfigTemplate template = findById(id);
        template.setUsageCount(template.getUsageCount() != null ? template.getUsageCount() + 1 : 1);
        template.setLastUsedAt(LocalDateTime.now());
        repository.save(template);
    }

    @Override
    @Transactional
    public List<CameraConfigTemplateDTO> importTemplates(List<CreateTemplateRequestDTO> templates) {
        List<CameraConfigTemplate> entities = templates.stream()
                .map(req -> CameraConfigTemplate.builder()
                        .brand(req.getBrand())
                        .model(req.getModel())
                        .protocol(req.getProtocol())
                        .defaultPort(req.getDefaultPort())
                        .urlPathTemplate(req.getUrlPathTemplate())
                        .presetParameters(req.getPresetParameters())
                        .isPreset(false)
                        .usageCount(0)
                        .isDeleted(false)
                        .build())
                .toList();

        List<CameraConfigTemplate> saved = repository.saveAll(entities);
        log.info("Imported {} camera config templates", saved.size());
        return saved.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CameraConfigTemplateDTO> exportTemplates(List<Long> ids) {
        return repository.findAllById(ids).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CameraConfigTemplateDTO> getPresetTemplates() {
        return repository.findByIsPresetAndIsDeletedFalse(true).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBrands() {
        return repository.findDistinctBrandByIsDeletedFalse();
    }

    private CameraConfigTemplate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Camera config template not found: " + id));
    }

    private CameraConfigTemplateDTO toDTO(CameraConfigTemplate entity) {
        return CameraConfigTemplateDTO.builder()
                .id(entity.getId())
                .brand(entity.getBrand())
                .model(entity.getModel())
                .protocol(entity.getProtocol())
                .defaultPort(entity.getDefaultPort())
                .urlPathTemplate(entity.getUrlPathTemplate())
                .presetParameters(entity.getPresetParameters())
                .isPreset(entity.getIsPreset())
                .usageCount(entity.getUsageCount())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}
