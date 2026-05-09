package com.aick.mmp.central.repository;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.shared.model.EdgeNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdgeNodeRepository extends JpaRepository<EdgeNode, Long>, JpaSpecificationExecutor<EdgeNode> {
    Page<EdgeNode> findByLocation(String location, Pageable pageable);
    Page<EdgeNode> findByStatus(EdgeNode.NodeStatus status, Pageable pageable);
    
    // 新增：不需要分页参数的重载方法
    List<EdgeNode> findByStatus(EdgeNode.NodeStatus status);
    
    @Query("SELECT e FROM EdgeNode e WHERE e.status IN :statuses")
    List<EdgeNode> findByStatusIn(@Param("statuses") List<EdgeNode.NodeStatus> statuses);
    
    Optional<EdgeNode> findByIpAddressAndPort(String ipAddress, Integer port);
    Optional<EdgeNode> findByName(String name);
    Optional<EdgeNode> findByUuid(String uuid);
    List<EdgeNode> findByStatusAndEnabled(EdgeNode.NodeStatus status, boolean enabled);
    long countByStatus(EdgeNode.NodeStatus status);
    boolean existsByName(String name);
    boolean existsByIpAddressAndPort(String ipAddress, Integer port);
    Optional<EdgeNode> findByIpAddress(String ipAddress);

    // ==================== Region Query Methods ====================

    /**
     * Find edge nodes by region ID
     */
    List<EdgeNode> findByRegionId(Long regionId);

    /**
     * Find edge nodes by region ID with pagination
     */
    Page<EdgeNode> findByRegionId(Long regionId, Pageable pageable);

    /**
     * Count edge nodes by region ID
     */
    long countByRegionId(Long regionId);

    /**
     * Find edge nodes by region IDs (for recursive query)
     */
    List<EdgeNode> findByRegionIdIn(List<Long> regionIds);

    /**
     * Count edge nodes by region IDs (for recursive count)
     */
    long countByRegionIdIn(List<Long> regionIds);
}