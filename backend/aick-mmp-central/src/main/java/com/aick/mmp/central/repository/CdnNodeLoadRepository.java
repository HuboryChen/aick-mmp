package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.CdnNodeLoad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDN节点负载历史数据仓库接口
 */
@Repository
public interface CdnNodeLoadRepository extends JpaRepository<CdnNodeLoad, Long>, 
        JpaSpecificationExecutor<CdnNodeLoad> {

    /**
     * 根据节点ID查询负载历史
     */
    List<CdnNodeLoad> findByCdnNodeIdOrderByRecordedAtDesc(Long cdnNodeId);

    /**
     * 根据节点ID分页查询负载历史
     */
    Page<CdnNodeLoad> findByCdnNodeId(Long cdnNodeId, Pageable pageable);

    /**
     * 查询指定时间范围内的负载历史
     */
    @Query("SELECT l FROM CdnNodeLoad l WHERE l.cdnNodeId = :nodeId " +
           "AND l.recordedAt BETWEEN :startTime AND :endTime ORDER BY l.recordedAt DESC")
    List<CdnNodeLoad> findByCdnNodeIdAndTimeRange(
            @Param("nodeId") Long cdnNodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的负载记录
     */
    @Query("SELECT l FROM CdnNodeLoad l WHERE l.cdnNodeId = :nodeId " +
           "ORDER BY l.recordedAt DESC LIMIT 1")
    CdnNodeLoad findLatestByCdnNodeId(@Param("nodeId") Long cdnNodeId);

    /**
     * 查询最近N条负载记录
     */
    @Query(value = "SELECT * FROM cdn_node_load_history WHERE cdn_node_id = :nodeId " +
                   "ORDER BY recorded_at DESC LIMIT :limit", nativeQuery = true)
    List<CdnNodeLoad> findRecentByCdnNodeId(
            @Param("nodeId") Long cdnNodeId,
            @Param("limit") int limit);

    /**
     * 计算平均负载
     */
    @Query("SELECT AVG(l.currentLoad) FROM CdnNodeLoad l WHERE l.cdnNodeId = :nodeId " +
           "AND l.recordedAt BETWEEN :startTime AND :endTime")
    Double findAverageLoadByNodeIdAndTimeRange(
            @Param("nodeId") Long cdnNodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 计算峰值负载
     */
    @Query("SELECT MAX(l.currentLoad) FROM CdnNodeLoad l WHERE l.cdnNodeId = :nodeId " +
           "AND l.recordedAt BETWEEN :startTime AND :endTime")
    Integer findMaxLoadByNodeIdAndTimeRange(
            @Param("nodeId") Long cdnNodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计健康节点数量
     */
    @Query("SELECT COUNT(DISTINCT l.cdnNodeId) FROM CdnNodeLoad l " +
           "WHERE l.status = 'ONLINE' AND l.recordedAt > :since")
    long countHealthyNodes(@Param("since") LocalDateTime since);

    /**
     * 删除指定节点的所有历史记录
     */
    void deleteByCdnNodeId(Long cdnNodeId);

    /**
     * 删除超过指定时间的记录（数据清理）
     */
    @Query("DELETE FROM CdnNodeLoad l WHERE l.recordedAt < :beforeTime")
    void deleteOldRecords(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 按小时聚合查询负载数据
     */
    @Query(value = "SELECT DATE_FORMAT(recorded_at, '%Y-%m-%d %H:00:00') as hour, " +
                   "AVG(current_load) as avg_load, MAX(current_load) as max_load, " +
                   "AVG(bandwidth_usage) as avg_bandwidth " +
                   "FROM cdn_node_load_history " +
                   "WHERE cdn_node_id = :nodeId AND recorded_at BETWEEN :startTime AND :endTime " +
                   "GROUP BY DATE_FORMAT(recorded_at, '%Y-%m-%d %H:00:00') " +
                   "ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyAggregatedLoad(
            @Param("nodeId") Long cdnNodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 按天聚合查询负载数据
     */
    @Query(value = "SELECT DATE(recorded_at) as day, " +
                   "AVG(current_load) as avg_load, MAX(current_load) as max_load, " +
                   "AVG(bandwidth_usage) as avg_bandwidth " +
                   "FROM cdn_node_load_history " +
                   "WHERE cdn_node_id = :nodeId AND recorded_at BETWEEN :startTime AND :endTime " +
                   "GROUP BY DATE(recorded_at) " +
                   "ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyAggregatedLoad(
            @Param("nodeId") Long cdnNodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
