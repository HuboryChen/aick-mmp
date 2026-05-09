package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConfigRepository extends JpaRepository<UserConfig, Long> {
    
    /**
     * 根据用户ID和配置键查找配置
     */
    Optional<UserConfig> findByUserIdAndConfigKey(Long userId, String configKey);
    
    /**
     * 获取用户的所有配置
     */
    List<UserConfig> findByUserId(Long userId);
    
    /**
     * 删除用户的所有配置
     */
    void deleteByUserId(Long userId);
    
    /**
     * 删除用户指定配置
     */
    void deleteByUserIdAndConfigKey(Long userId, String configKey);
    
    /**
     * 检查配置是否存在
     */
    boolean existsByUserIdAndConfigKey(Long userId, String configKey);
}
