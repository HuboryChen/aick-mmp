package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.VideoWallPreferencesDTO;
import com.aick.mmp.central.dto.VideoWallPresetDTO;

import java.util.List;

/**
 * 视频墙配置服务接口
 * 提供视频墙预设和偏好设置的CRUD操作
 */
public interface VideoWallConfigService {
    
    // ==================== Preset Management ====================
    
    /**
     * 获取用户的所有预设
     *
     * @param userId 用户ID
     * @return 预设列表
     */
    List<VideoWallPresetDTO> getPresets(Long userId);
    
    /**
     * 创建预设
     *
     * @param userId    用户ID
     * @param presetDTO 预设数据
     * @return 创建的预设
     */
    VideoWallPresetDTO createPreset(Long userId, VideoWallPresetDTO presetDTO);
    
    /**
     * 更新预设
     *
     * @param userId    用户ID
     * @param presetId  预设ID
     * @param presetDTO 预设数据
     * @return 更新后的预设
     */
    VideoWallPresetDTO updatePreset(Long userId, Long presetId, VideoWallPresetDTO presetDTO);
    
    /**
     * 删除预设
     *
     * @param userId   用户ID
     * @param presetId 预设ID
     */
    void deletePreset(Long userId, Long presetId);
    
    /**
     * 应用预设
     * 将预设配置应用到用户的偏好设置
     *
     * @param userId   用户ID
     * @param presetId 预设ID
     * @return 应用后的偏好设置
     */
    VideoWallPresetDTO applyPreset(Long userId, Long presetId);
    
    /**
     * 设置默认预设
     *
     * @param userId   用户ID
     * @param presetId 预设ID
     * @return 更新后的预设
     */
    VideoWallPresetDTO setDefaultPreset(Long userId, Long presetId);
    
    /**
     * 重新排序预设
     *
     * @param userId    用户ID
     * @param presetIds 预设ID列表（新顺序）
     */
    void reorderPresets(Long userId, List<Long> presetIds);
    
    // ==================== Preferences Management ====================
    
    /**
     * 获取用户偏好设置
     *
     * @param userId 用户ID
     * @return 偏好设置
     */
    VideoWallPreferencesDTO getPreferences(Long userId);
    
    /**
     * 更新用户偏好设置
     *
     * @param userId         用户ID
     * @param preferencesDTO 偏好设置数据
     * @return 更新后的偏好设置
     */
    VideoWallPreferencesDTO updatePreferences(Long userId, VideoWallPreferencesDTO preferencesDTO);
}
