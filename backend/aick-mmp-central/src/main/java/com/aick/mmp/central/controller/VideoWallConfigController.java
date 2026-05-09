package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.VideoWallPreferencesDTO;
import com.aick.mmp.central.dto.VideoWallPresetDTO;
import com.aick.mmp.central.dto.VideoWallPresetReorderDTO;
import com.aick.mmp.central.security.CurrentUserContext;
import com.aick.mmp.central.service.VideoWallConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频墙配置控制器
 * 提供视频墙预设和偏好设置的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/video-wall")
@RequiredArgsConstructor
@Tag(name = "视频墙配置", description = "视频墙预设和偏好设置管理")
public class VideoWallConfigController {

    private final VideoWallConfigService videoWallConfigService;
    private final CurrentUserContext currentUserContext;

    // ==================== User Preferences Endpoints ====================

    /**
     * 获取当前用户偏好设置
     */
    @GetMapping("/preferences")
    @Operation(summary = "获取用户偏好设置", description = "获取当前用户的视频墙偏好设置")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPreferencesDTO> getPreferences() {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Getting preferences for user {}", userId);
        VideoWallPreferencesDTO preferences = videoWallConfigService.getPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * 更新当前用户偏好设置
     */
    @PutMapping("/preferences")
    @Operation(summary = "更新用户偏好设置", description = "更新当前用户的视频墙偏好设置")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPreferencesDTO> updatePreferences(
            @RequestBody VideoWallPreferencesDTO preferencesDTO) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Updating preferences for user {}", userId);
        VideoWallPreferencesDTO updated = videoWallConfigService.updatePreferences(userId, preferencesDTO);
        return ResponseEntity.ok(updated);
    }

    // ==================== Preset Management Endpoints ====================

    /**
     * 获取当前用户所有预设
     */
    @GetMapping("/presets")
    @Operation(summary = "获取所有预设", description = "获取当前用户的所有视频墙预设")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<VideoWallPresetDTO>> getPresets() {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Getting presets for user {}", userId);
        List<VideoWallPresetDTO> presets = videoWallConfigService.getPresets(userId);
        return ResponseEntity.ok(presets);
    }

    /**
     * 创建新预设
     */
    @PostMapping("/presets")
    @Operation(summary = "创建预设", description = "创建新的视频墙预设")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPresetDTO> createPreset(@RequestBody VideoWallPresetDTO presetDTO) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Creating preset '{}' for user {}", presetDTO.getPresetName(), userId);
        VideoWallPresetDTO created = videoWallConfigService.createPreset(userId, presetDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新预设
     */
    @PutMapping("/presets/{id}")
    @Operation(summary = "更新预设", description = "更新指定的视频墙预设")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPresetDTO> updatePreset(
            @PathVariable Long id,
            @RequestBody VideoWallPresetDTO presetDTO) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Updating preset {} for user {}", id, userId);
        VideoWallPresetDTO updated = videoWallConfigService.updatePreset(userId, id, presetDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除预设
     */
    @DeleteMapping("/presets/{id}")
    @Operation(summary = "删除预设", description = "删除指定的视频墙预设")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Void> deletePreset(@PathVariable Long id) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Deleting preset {} for user {}", id, userId);
        videoWallConfigService.deletePreset(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 应用预设
     * 将预设配置应用到用户的偏好设置
     */
    @PostMapping("/presets/{id}/apply")
    @Operation(summary = "应用预设", description = "将预设配置应用到用户的视频墙偏好设置")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPresetDTO> applyPreset(@PathVariable Long id) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Applying preset {} for user {}", id, userId);
        VideoWallPresetDTO applied = videoWallConfigService.applyPreset(userId, id);
        return ResponseEntity.ok(applied);
    }

    /**
     * 设置默认预设
     */
    @PostMapping("/presets/{id}/set-default")
    @Operation(summary = "设为默认", description = "将指定预设设为用户的默认预设")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<VideoWallPresetDTO> setDefaultPreset(@PathVariable Long id) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Setting preset {} as default for user {}", id, userId);
        VideoWallPresetDTO updated = videoWallConfigService.setDefaultPreset(userId, id);
        return ResponseEntity.ok(updated);
    }

    /**
     * 批量更新预设排序
     */
    @PutMapping("/presets/reorder")
    @Operation(summary = "重新排序预设", description = "批量更新预设的排序顺序")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Void> reorderPresets(@RequestBody VideoWallPresetReorderDTO reorderDTO) {
        Long userId = currentUserContext.getCurrentUserId();
        log.info("Reordering presets for user {}", userId);
        videoWallConfigService.reorderPresets(userId, reorderDTO.getPresetIds());
        return ResponseEntity.ok().build();
    }
}
