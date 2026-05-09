package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.ApiKey;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    /**
     * Find API key by access key (AK)
     */
    Optional<ApiKey> findByAccessKey(String accessKey);
    
    /**
     * Find all API keys for a user
     */
    List<ApiKey> findByUserId(Long userId);
    
    /**
     * Find all API keys by type (USER type only)
     */
    List<ApiKey> findByType(ApiKeyType type);
    
    /**
     * Check if access key already exists
     */
    boolean existsByAccessKey(String accessKey);
    
    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :lastUsedAt WHERE a.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
