package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.UserConfigDTO;
import com.aick.mmp.shared.model.UserConfig;

import java.util.List;
import java.util.Optional;

/**
 * 用户配置服务接口
 */
public interface UserConfigService {
    
    /**
     * 保存或更新用户配置
     *
     * @param userId 用户ID
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 保存的配置
     */
    UserConfig saveConfig(Long userId, String configKey, String configValue);
    
    /**
     * 批量保存用户配置
     *
     * @param userId 用户ID
     * @param configs 配置列表
     * @return 保存的配置列表
     */
    List<UserConfig> saveConfigs(Long userId, List<UserConfigDTO> configs);
    
    /**
     * 获取用户指定配置
     *
     * @param userId 用户ID
     * @param configKey 配置键
     * @return 配置值
     */
    Optional<UserConfig> getConfig(Long userId, String configKey);
    
    /**
     * 获取用户所有配置
     *
     * @param userId 用户ID
     * @return 配置列表
     */
    List<UserConfig> getUserConfigs(Long userId);
    
    /**
     * 删除用户指定配置
     *
     * @param userId 用户ID
     * @param configKey 配置键
     */
    void deleteConfig(Long userId, String configKey);
    
    /**
     * 删除用户所有配置
     *
     * @param userId 用户ID
     */
    void deleteUserConfigs(Long userId);
}
