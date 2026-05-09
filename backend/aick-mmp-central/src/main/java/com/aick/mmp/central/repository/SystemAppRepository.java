package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.OwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemAppRepository extends JpaRepository<SystemApp, Long> {
    
    /**
     * Find system app by app key (for authentication)
     */
    Optional<SystemApp> findByAppKey(String appKey);
    
    /**
     * Find all system apps by owner type
     */
    Page<SystemApp> findByOwnerType(OwnerType ownerType, Pageable pageable);
    
    /**
     * Find all system apps by status
     */
    Page<SystemApp> findByStatus(SystemApp.AppStatus status, Pageable pageable);
    
    /**
     * Find all system apps by owner type and status
     */
    Page<SystemApp> findByOwnerTypeAndStatus(OwnerType ownerType, SystemApp.AppStatus status, Pageable pageable);
    
    /**
     * Find all system-level apps (not owned by any user)
     */
    List<SystemApp> findByOwnerType(OwnerType ownerType);
    
    /**
     * Find apps owned by a specific user
     */
    List<SystemApp> findByOwnerId(Long ownerId);
    
    /**
     * Check if app key already exists
     */
    boolean existsByAppKey(String appKey);
    
    /**
     * Check if app name already exists
     */
    boolean existsByName(String name);
    
    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE SystemApp s SET s.lastUsedAt = :lastUsedAt WHERE s.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
