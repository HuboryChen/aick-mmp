package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.VideoWallPreferencesDTO;
import com.aick.mmp.central.dto.VideoWallPresetDTO;
import com.aick.mmp.central.entity.VideoWallPreferences;
import com.aick.mmp.central.entity.VideoWallPreset;
import com.aick.mmp.central.repository.VideoWallPreferencesRepository;
import com.aick.mmp.central.repository.VideoWallPresetRepository;
import com.aick.mmp.central.service.VideoWallConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频墙配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoWallConfigServiceImpl implements VideoWallConfigService {

    private final VideoWallPresetRepository presetRepository;
    private final VideoWallPreferencesRepository preferencesRepository;
    private final ObjectMapper objectMapper;

    // ==================== DTO 转换方法 ====================

    /**
     * 将实体转换为DTO
     */
    private VideoWallPresetDTO toPresetDTO(VideoWallPreset preset) {
        return VideoWallPresetDTO.builder()
                .id(preset.getId())
                .presetName(preset.getPresetName())
                .layout(preset.getLayout())
                .quality(preset.getQuality())
                .bitrate(preset.getBitrate())
                .cameraIds(parseCameraIds(preset.getCameraIds()))
                .isDefault(preset.getIsDefault())
                .sortOrder(preset.getSortOrder())
                .createdAt(preset.getCreatedAt())
                .updatedAt(preset.getUpdatedAt())
                .build();
    }

    /**
     * 将DTO转换为实体
     */
    private VideoWallPreset toPresetEntity(VideoWallPresetDTO dto) {
        return VideoWallPreset.builder()
                .id(dto.getId())
                .presetName(dto.getPresetName())
                .layout(dto.getLayout())
                .quality(dto.getQuality())
                .bitrate(dto.getBitrate())
                .cameraIds(serializeCameraIds(dto.getCameraIds()))
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
    }

    /**
     * 将实体转换为DTO
     */
    private VideoWallPreferencesDTO toPreferencesDTO(VideoWallPreferences preferences) {
        return VideoWallPreferencesDTO.builder()
                .id(preferences.getId())
                .layout(preferences.getLayout())
                .quality(preferences.getQuality())
                .bitrate(preferences.getBitrate())
                .cameraIds(parseCameraIds(preferences.getCameraIds()))
                .autoApply(preferences.getAutoApply())
                .lastPresetId(preferences.getLastPresetId())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }

    /**
     * 将DTO转换为实体
     */
    private VideoWallPreferences toPreferencesEntity(VideoWallPreferencesDTO dto) {
        return VideoWallPreferences.builder()
                .id(dto.getId())
                .layout(dto.getLayout())
                .quality(dto.getQuality())
                .bitrate(dto.getBitrate())
                .cameraIds(serializeCameraIds(dto.getCameraIds()))
                .autoApply(dto.getAutoApply() != null ? dto.getAutoApply() : true)
                .lastPresetId(dto.getLastPresetId())
                .build();
    }

    /**
     * 解析摄像头ID列表 (JSON -> List)
     */
    private List<Long> parseCameraIds(String cameraIdsJson) {
        if (cameraIdsJson == null || cameraIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(cameraIdsJson, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse cameraIds JSON: {}", cameraIdsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化摄像头ID列表 (List -> JSON)
     */
    private String serializeCameraIds(List<Long> cameraIds) {
        if (cameraIds == null || cameraIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(cameraIds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cameraIds: {}", cameraIds, e);
            return "[]";
        }
    }

    // ==================== Preset Management ====================

    @Override
    @Transactional(readOnly = true)
    public List<VideoWallPresetDTO> getPresets(Long userId) {
        List<VideoWallPreset> presets = presetRepository.findByUserIdOrderBySortOrderAsc(userId);
        return presets.stream()
                .map(this::toPresetDTO)
                .toList();
    }

    @Override
    @Transactional
    public VideoWallPresetDTO createPreset(Long userId, VideoWallPresetDTO presetDTO) {
        // 检查预设名称是否重复
        if (presetRepository.existsByUserIdAndPresetName(userId, presetDTO.getPresetName())) {
            throw new IllegalArgumentException("预设名称已存在: " + presetDTO.getPresetName());
        }

        // 获取当前最大排序号
        int maxSortOrder = (int) presetRepository.countByUserId(userId);

        VideoWallPreset preset = toPresetEntity(presetDTO);
        preset.setUserId(userId);
        preset.setSortOrder(maxSortOrder);

        VideoWallPreset savedPreset = presetRepository.save(preset);
        log.info("Created preset '{}' for user {}", savedPreset.getPresetName(), userId);
        
        return toPresetDTO(savedPreset);
    }

    @Override
    @Transactional
    public VideoWallPresetDTO updatePreset(Long userId, Long presetId, VideoWallPresetDTO presetDTO) {
        VideoWallPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("预设不存在: " + presetId));

        // 验证用户权限
        if (!preset.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权操作此预设");
        }

        // 检查新名称是否与其他预设冲突
        if (!preset.getPresetName().equals(presetDTO.getPresetName()) &&
            presetRepository.existsByUserIdAndPresetName(userId, presetDTO.getPresetName())) {
            throw new IllegalArgumentException("预设名称已存在: " + presetDTO.getPresetName());
        }

        preset.setPresetName(presetDTO.getPresetName());
        preset.setLayout(presetDTO.getLayout());
        preset.setQuality(presetDTO.getQuality());
        preset.setBitrate(presetDTO.getBitrate());
        preset.setCameraIds(serializeCameraIds(presetDTO.getCameraIds()));
        if (presetDTO.getIsDefault() != null) {
            preset.setIsDefault(presetDTO.getIsDefault());
        }

        VideoWallPreset updatedPreset = presetRepository.save(preset);
        log.info("Updated preset {} for user {}", presetId, userId);
        
        return toPresetDTO(updatedPreset);
    }

    @Override
    @Transactional
    public void deletePreset(Long userId, Long presetId) {
        VideoWallPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("预设不存在: " + presetId));

        // 验证用户权限
        if (!preset.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权操作此预设");
        }

        presetRepository.delete(preset);
        log.info("Deleted preset {} for user {}", presetId, userId);
    }

    @Override
    @Transactional
    public VideoWallPresetDTO applyPreset(Long userId, Long presetId) {
        VideoWallPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("预设不存在: " + presetId));

        // 验证用户权限
        if (!preset.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权操作此预设");
        }

        // 获取或创建用户偏好设置
        VideoWallPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> VideoWallPreferences.builder()
                        .userId(userId)
                        .layout("4")
                        .quality("720p")
                        .bitrate(2048)
                        .cameraIds("[]")
                        .autoApply(true)
                        .build());

        // 应用预设配置到偏好设置
        preferences.setLayout(preset.getLayout());
        preferences.setQuality(preset.getQuality());
        preferences.setBitrate(preset.getBitrate());
        preferences.setCameraIds(preset.getCameraIds());
        preferences.setLastPresetId(presetId);

        VideoWallPreferences savedPreferences = preferencesRepository.save(preferences);
        log.info("Applied preset {} to preferences for user {}", presetId, userId);
        
        return toPresetDTO(preset);
    }

    @Override
    @Transactional
    public VideoWallPresetDTO setDefaultPreset(Long userId, Long presetId) {
        VideoWallPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("预设不存在: " + presetId));

        // 验证用户权限
        if (!preset.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权操作此预设");
        }

        // 清除其他默认标记
        presetRepository.clearDefaultByUserId(userId);

        // 设置新的默认标记
        preset.setIsDefault(true);
        VideoWallPreset updatedPreset = presetRepository.save(preset);
        
        log.info("Set preset {} as default for user {}", presetId, userId);
        return toPresetDTO(updatedPreset);
    }

    @Override
    @Transactional
    public void reorderPresets(Long userId, List<Long> presetIds) {
        List<Long> validPresetIds = new ArrayList<>();
        
        for (int i = 0; i < presetIds.size(); i++) {
            Long presetId = presetIds.get(i);
            VideoWallPreset preset = presetRepository.findById(presetId)
                    .orElseThrow(() -> new EntityNotFoundException("预设不存在: " + presetId));
            
            // 验证用户权限
            if (!preset.getUserId().equals(userId)) {
                throw new AccessDeniedException("无权操作此预设: " + presetId);
            }
            
            preset.setSortOrder(i);
            presetRepository.save(preset);
            validPresetIds.add(presetId);
        }
        
        log.info("Reordered {} presets for user {}", validPresetIds.size(), userId);
    }

    // ==================== Preferences Management ====================

    @Override
    @Transactional(readOnly = true)
    public VideoWallPreferencesDTO getPreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .map(this::toPreferencesDTO)
                .orElseGet(() -> VideoWallPreferencesDTO.builder()
                        .layout("4")
                        .quality("720p")
                        .bitrate(2048)
                        .cameraIds(new ArrayList<>())
                        .autoApply(true)
                        .build());
    }

    @Override
    @Transactional
    public VideoWallPreferencesDTO updatePreferences(Long userId, VideoWallPreferencesDTO preferencesDTO) {
        VideoWallPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> VideoWallPreferences.builder()
                        .userId(userId)
                        .build());

        // 更新非空字段
        if (preferencesDTO.getLayout() != null) {
            preferences.setLayout(preferencesDTO.getLayout());
        }
        if (preferencesDTO.getQuality() != null) {
            preferences.setQuality(preferencesDTO.getQuality());
        }
        if (preferencesDTO.getBitrate() != null) {
            preferences.setBitrate(preferencesDTO.getBitrate());
        }
        if (preferencesDTO.getCameraIds() != null) {
            preferences.setCameraIds(serializeCameraIds(preferencesDTO.getCameraIds()));
        }
        if (preferencesDTO.getAutoApply() != null) {
            preferences.setAutoApply(preferencesDTO.getAutoApply());
        }
        if (preferencesDTO.getLastPresetId() != null) {
            preferences.setLastPresetId(preferencesDTO.getLastPresetId());
        }

        VideoWallPreferences savedPreferences = preferencesRepository.save(preferences);
        log.info("Updated preferences for user {}", userId);
        
        return toPreferencesDTO(savedPreferences);
    }
}
