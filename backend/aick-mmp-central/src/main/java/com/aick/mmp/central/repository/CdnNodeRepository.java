package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.CdnNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CDN节点数据仓库接口
 */
@Repository
public interface CdnNodeRepository extends JpaRepository<CdnNode, Long>, JpaSpecificationExecutor<CdnNode> {

    // ==================== 基础查询 ====================
    
    /**
     * 根据IP地址查询
     */
    boolean existsByIpAddress(String ipAddress);

    /**
     * 根据IP地址和端口查询
     */
    Optional<CdnNode> findByIpAddressAndPort(String ipAddress, Integer port);

    /**
     * 根据节点标识符查询
     */
    Optional<CdnNode> findByNodeId(String nodeId);

    // ==================== 软删除相关查询 ====================
    
    /**
     * 查询未删除的节点
     */
    List<CdnNode> findByIsDeletedFalse();

    /**
     * 分页查询未删除的节点
     */
    Page<CdnNode> findByIsDeletedFalse(Pageable pageable);

    /**
     * 查询未删除且启用的节点
     */
    List<CdnNode> findByIsDeletedFalseAndIsEnabledTrue();

    // ==================== 状态查询 ====================

    // ==================== Region Query Methods ====================

    /**
     * Find CDN nodes by region ID
     */
    List<CdnNode> findByRegionId(Long regionId);

    /**
     * Find CDN nodes by region ID with pagination
     */
    Page<CdnNode> findByRegionId(Long regionId, Pageable pageable);

    /**
     * Find CDN nodes by region ID and not deleted
     */
    Page<CdnNode> findByRegionIdAndIsDeletedFalse(Long regionId, Pageable pageable);

    /**
     * Count CDN nodes by region ID
     */
    long countByRegionId(Long regionId);

    /**
     * Count CDN nodes by region ID and not deleted
     */
    long countByRegionIdAndIsDeletedFalse(Long regionId);

    /**
     * Find CDN nodes by region IDs (for recursive query)
     */
    List<CdnNode> findByRegionIdIn(List<Long> regionIds);

    /**
     * Count CDN nodes by region IDs (for recursive count)
     */
    long countByRegionIdIn(List<Long> regionIds);

    /**
     * Find CDN nodes by region IDs and status ordered by load
     */
    List<CdnNode> findByRegionIdAndStatusOrderByCurrentLoadAsc(Long regionId, CdnNode.NodeStatus status);

    /**
     * Find CDN nodes by region IDs, status and not deleted
     */
    List<CdnNode> findByRegionIdAndStatusAndIsDeletedFalseOrderByCurrentLoadAsc(
            Long regionId, CdnNode.NodeStatus status);

    /**
     * 根据状态分页查询
     */
    Page<CdnNode> findByStatus(CdnNode.NodeStatus status, Pageable pageable);

    /**
     * 根据状态和未删除状态查询
     */
    Page<CdnNode> findByStatusAndIsDeletedFalse(CdnNode.NodeStatus status, Pageable pageable);

    // ==================== 负载均衡相关查询 ====================

    /**
     * 根据状态按负载升序查询
     */
    List<CdnNode> findByStatusOrderByCurrentLoadAsc(CdnNode.NodeStatus status);

    /**
     * 根据状态和未删除状态，按负载升序查询
     */
    List<CdnNode> findByStatusAndIsDeletedFalseOrderByCurrentLoadAsc(CdnNode.NodeStatus status);

    /**
     * 查询在线且未删除的节点
     */
    List<CdnNode> findByStatusAndIsDeletedFalseAndIsEnabledTrueOrderByWeightDesc(
            CdnNode.NodeStatus status);

    // ==================== WLC算法相关查询 ====================

    /**
     * 根据区域ID查询在线节点（用于WLC计算）
     */
    @Query("SELECT n FROM CdnNode n WHERE n.regionId = :regionId " +
           "AND n.status = 'ONLINE' AND n.isDeleted = false AND n.isEnabled = true " +
           "ORDER BY n.weight * (n.capacity - n.currentLoad) / n.capacity DESC")
    List<CdnNode> findAvailableNodesByRegionIdForWlc(@Param("regionId") Long regionId);

    /**
     * 查询所有在线节点（用于WLC计算）
     */
    @Query("SELECT n FROM CdnNode n WHERE n.status = 'ONLINE' " +
           "AND n.isDeleted = false AND n.isEnabled = true " +
           "ORDER BY n.weight * (n.capacity - n.currentLoad) / n.capacity DESC")
    List<CdnNode> findAllAvailableNodesForWlc();

    // ==================== 健康检查相关查询 ====================

    /**
     * 查询心跳超时的节点
     */
    @Query("SELECT n FROM CdnNode n WHERE n.status = 'ONLINE' " +
           "AND n.isDeleted = false AND n.lastHeartbeat < :threshold")
    List<CdnNode> findNodesWithHeartbeatTimeout(@Param("threshold") LocalDateTime threshold);

    /**
     * 查询需要健康检查的节点
     */
    @Query("SELECT n FROM CdnNode n WHERE n.status != 'OFFLINE' " +
           "AND n.isDeleted = false AND n.isEnabled = true")
    List<CdnNode> findNodesNeedingHealthCheck();

    // ==================== 统计查询 ====================

    /**
     * 统计在线节点数量
     */
    long countByStatusAndIsDeletedFalse(CdnNode.NodeStatus status);

    /**
     * 统计启用的节点数量
     */
    long countByIsDeletedFalseAndIsEnabledTrue();

    /**
     * 统计所有在线节点
     */
    @Query("SELECT COUNT(n) FROM CdnNode n WHERE n.status = 'ONLINE' AND n.isDeleted = false")
    long countOnlineNodes();

    /**
     * 统计所有离线节点
     */
    @Query("SELECT COUNT(n) FROM CdnNode n WHERE n.status = 'OFFLINE' AND n.isDeleted = false")
    long countOfflineNodes();

    /**
     * 查询负载超过阈值的节点
     */
    @Query("SELECT n FROM CdnNode n WHERE n.isDeleted = false " +
           "AND n.capacity > 0 AND (n.currentLoad * 100.0 / n.capacity) > :threshold")
    List<CdnNode> findOverloadedNodes(@Param("threshold") double threshold);

    // ==================== 更新操作 ====================

    /**
     * 批量更新节点状态
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.status = :status, n.updatedAt = :now " +
           "WHERE n.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<Long> ids, 
                         @Param("status") CdnNode.NodeStatus status,
                         @Param("now") LocalDateTime now);

    /**
     * 批量更新节点负载
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.currentLoad = :load, n.updatedAt = :now " +
           "WHERE n.id = :id")
    int updateNodeLoad(@Param("id") Long id, 
                       @Param("load") Integer load,
                       @Param("now") LocalDateTime now);

    /**
     * 批量更新心跳时间
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.lastHeartbeat = :heartbeatTime " +
           "WHERE n.nodeId = :nodeId")
    int updateHeartbeat(@Param("nodeId") String nodeId, 
                        @Param("heartbeatTime") LocalDateTime heartbeatTime);

    // ==================== 软删除操作 ====================

    /**
     * 软删除节点
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.isDeleted = true, n.deletedAt = :deletedAt " +
           "WHERE n.id = :id")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 批量软删除
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.isDeleted = true, n.deletedAt = :deletedAt " +
           "WHERE n.id IN :ids")
    int batchSoftDelete(@Param("ids") List<Long> ids, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 恢复已删除的节点
     */
    @Modifying
    @Query("UPDATE CdnNode n SET n.isDeleted = false, n.deletedAt = null " +
           "WHERE n.id = :id")
    int restore(@Param("id") Long id);
}
