package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.ConfigHistory;
import com.aick.mmp.shared.model.SystemConfig;
import com.aick.mmp.shared.model.enums.ConfigCategory;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {
    
    // ==================== CRUD操作 ====================
    
    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * 获取所有启用的配置
     */
    List<SystemConfig> getEnabledConfigs();
    
    /**
     * 根据配置键获取配置
     */
    SystemConfig getByConfigKey(String configKey);
    
    /**
     * 根据配置键获取配置值（不返回敏感值）
     */
    String getConfigValue(String configKey);
    
    /**
     * 根据配置键获取配置值（包含敏感值）
     */
    String getConfigValueWithSensitive(String configKey);
    
    /**
     * 根据分类获取配置
     */
    List<SystemConfig> getByCategory(ConfigCategory category);
    
    /**
     * 根据分组获取配置
     */
    List<SystemConfig> getByGroup(String group);
    
    /**
     * 创建配置
     */
    SystemConfig createConfig(SystemConfig config);
    
    /**
     * 更新配置
     */
    SystemConfig updateConfig(Long id, SystemConfig config);
    
    /**
     * 删除配置
     */
    void deleteConfig(Long id);
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量更新配置
     */
    Map<String, Boolean> batchUpdateConfigs(Map<String, String> configValues);
    
    // ==================== 配置验证 ====================
    
    /**
     * 验证配置值
     */
    boolean validateConfigValue(SystemConfig config, String value);
    
    // ==================== 配置历史 ====================
    
    /**
     * 获取配置历史
     */
    List<ConfigHistory> getConfigHistory(String configKey);
    
    /**
     * 回滚配置
     */
    SystemConfig rollbackConfig(String configKey);
    
    // ==================== 配置重置 ====================
    
    /**
     * 重置配置到默认值
     */
    SystemConfig resetConfig(String configKey);
    
    /**
     * 重置所有配置到默认值
     */
    void resetAllConfigs();
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取所有分组
     */
    List<String> getAllGroups();
    
    /**
     * 初始化默认配置
     */
    void initializeDefaultConfigs();
}
