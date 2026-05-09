package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.ConfigHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 配置历史仓库
 */
@Repository
public interface ConfigHistoryRepository extends JpaRepository<ConfigHistory, Long> {
    
    /**
     * 根据配置键查询历史
     */
    List<ConfigHistory> findByConfigKeyOrderByCreatedAtDesc(String configKey);
    
    /**
     * 分页查询配置历史
     */
    Page<ConfigHistory> findByConfigKeyOrderByCreatedAtDesc(String configKey, Pageable pageable);
    
    /**
     * 根据配置ID查询历史
     */
    List<ConfigHistory> findByConfigIdOrderByCreatedAtDesc(Long configId);
    
    /**
     * 分页查询配置历史（按配置ID）
     */
    Page<ConfigHistory> findByConfigIdOrderByCreatedAtDesc(Long configId, Pageable pageable);
    
    /**
     * 查询可回滚的历史记录
     */
    List<ConfigHistory> findByConfigKeyAndRollbackableTrueAndRolledBackFalseOrderByCreatedAtDesc(String configKey);
    
    /**
     * 查询最近的回滚点
     */
    @Query("SELECT h FROM ConfigHistory h WHERE h.configKey = :configKey AND h.rollbackable = true AND h.rolledBack = false ORDER BY h.createdAt DESC LIMIT 1")
    Optional<ConfigHistory> findLastRollbackPoint(@Param("configKey") String configKey);
    
    /**
     * 根据时间范围查询
     */
    Page<ConfigHistory> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据操作者查询
     */
    Page<ConfigHistory> findByOperatorIdOrderByCreatedAtDesc(Long operatorId, Pageable pageable);
    
    /**
     * 根据操作类型查询
     */
    Page<ConfigHistory> findByOperationTypeOrderByCreatedAtDesc(String operationType, Pageable pageable);
    
    /**
     * 统计配置变更次数
     */
    long countByConfigKey(String configKey);
    
    /**
     * 删除配置键的所有历史
     */
    void deleteByConfigKey(String configKey);
    
    /**
     * 清理过期历史（保留最近N条）
     */
    @Query(value = "DELETE FROM config_history WHERE config_key = :configKey AND id NOT IN (SELECT id FROM config_history WHERE config_key = :configKey ORDER BY created_at DESC LIMIT :keepCount)", nativeQuery = true)
    void cleanupOldHistory(@Param("configKey") String configKey, @Param("keepCount") int keepCount);
}
