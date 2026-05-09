package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.SystemConfig;
import com.aick.mmp.shared.model.enums.ConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓库
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 根据配置键查询
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
    
    /**
     * 根据配置键查询（包含敏感信息）
     */
    @Query("SELECT s FROM SystemConfig s WHERE s.configKey = :configKey")
    Optional<SystemConfig> findByConfigKeyWithSensitive(@Param("configKey") String configKey);
    
    /**
     * 根据分类查询
     */
    List<SystemConfig> findByCategoryOrderBySortOrderAsc(ConfigCategory category);
    
    /**
     * 根据分类查询（只返回启用的）
     */
    List<SystemConfig> findByCategoryAndEnabledTrueOrderBySortOrderAsc(ConfigCategory category);
    
    /**
     * 根据分组查询
     */
    List<SystemConfig> findByConfigGroupOrderBySortOrderAsc(String configGroup);
    
    /**
     * 根据分组查询（只返回启用的）
     */
    List<SystemConfig> findByConfigGroupAndEnabledTrueOrderBySortOrderAsc(String configGroup);
    
    /**
     * 查询所有启用的配置
     */
    List<SystemConfig> findByEnabledTrueOrderByCategoryAscSortOrderAsc();
    
    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);
    
    /**
     * 根据配置键删除
     */
    @Modifying
    @Query("DELETE FROM SystemConfig s WHERE s.configKey = :configKey")
    int deleteByConfigKey(@Param("configKey") String configKey);
    
    /**
     * 批量更新配置值
     */
    @Modifying
    @Query("UPDATE SystemConfig s SET s.configValue = :value, s.updatedAt = CURRENT_TIMESTAMP WHERE s.configKey = :key")
    int updateConfigValue(@Param("key") String key, @Param("value") String value);
    
    /**
     * 根据分类统计配置数量
     */
    long countByCategory(ConfigCategory category);
    
    /**
     * 获取所有分组
     */
    @Query("SELECT DISTINCT s.configGroup FROM SystemConfig s WHERE s.configGroup IS NOT NULL ORDER BY s.configGroup")
    List<String> findAllGroups();
    
    /**
     * 根据多个配置键批量查询
     */
    List<SystemConfig> findByConfigKeyIn(List<String> configKeys);
}
