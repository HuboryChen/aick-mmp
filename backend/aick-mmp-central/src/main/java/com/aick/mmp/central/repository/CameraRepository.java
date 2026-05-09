package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {
    Page<Camera> findByLocation(String location, Pageable pageable);
    Page<Camera> findByEdgeNodeId(Long edgeNodeId, Pageable pageable);
    List<Camera> findByEdgeNodeId(Long edgeNodeId);
    Page<Camera> findByStatus(Camera.CameraStatus status, Pageable pageable);
    List<Camera> findByStatus(Camera.CameraStatus status);
    List<Camera> findByEdgeNodeIdAndStatus(Long edgeNodeId, Camera.CameraStatus status);
    Optional<Camera> findByConnectionUrl(String connectionUrl);
    boolean existsByConnectionUrl(String connectionUrl);
    long countByStatus(Camera.CameraStatus status);
    long countByEdgeNodeId(Long edgeNodeId);
    
    // 新增：查询未分配边缘节点的摄像头
    List<Camera> findByEdgeNodeIdIsNull();

    // 新增：查询待分配池摄像头（edgeNodeId为空且状态为PENDING_ALLOCATION）
    List<Camera> findByEdgeNodeIdIsNullAndStatus(Camera.CameraStatus status);

    Page<Camera> findAll(Specification<Camera> specification, Pageable pageable);
    
    // Region相关查询
    List<Camera> findByRegionId(Long regionId);
    
    List<Camera> findByRegionIdAndIsDeletedFalse(Long regionId);
    
    long countByRegionId(Long regionId);
    
    long countByRegionIdAndStatus(Long regionId, Camera.CameraStatus status);
    
    @Query("SELECT c FROM Camera c WHERE c.regionId IN :regionIds AND c.isDeleted = false")
    List<Camera> findByRegionIdIn(@Param("regionIds") List<Long> regionIds);
    
    @Query("SELECT c FROM Camera c WHERE c.regionId IS NULL AND c.isDeleted = false")
    List<Camera> findByRegionIdIsNull();
    
    @Query("SELECT COUNT(c) FROM Camera c WHERE c.regionId = :regionId AND c.isDeleted = false")
    long countByRegionIdAndIsDeletedFalse(@Param("regionId") Long regionId);
    
    @Query("SELECT COUNT(c) FROM Camera c WHERE c.regionId = :regionId AND c.status = :status AND c.isDeleted = false")
    long countByRegionIdAndStatusAndIsDeletedFalse(@Param("regionId") Long regionId, @Param("status") Camera.CameraStatus status);
    
    // Analytics相关查询
    List<Camera> findByIdIn(List<Long> ids);

    // ============ 软删除查询方法 ============

    /**
     * 查询所有未删除的摄像头
     */
    @Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
    List<Camera> findAllActive();

    /**
     * 查询所有已删除的摄像头（使用 nativeQuery 以绕过 @Where 过滤器）
     */
    @Query(value = "SELECT * FROM cameras WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Camera> findAllDeleted();

    /**
     * 分页查询所有未删除的摄像头
     */
    @Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
    Page<Camera> findAllActive(Pageable pageable);

    /**
     * 根据ID查询未删除的摄像头
     */
    Optional<Camera> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 统计未删除的摄像头数量
     */
    @Query("SELECT COUNT(c) FROM Camera c WHERE c.deletedAt IS NULL")
    long countActive();

    /**
     * 根据边缘节点ID统计未删除的摄像头数量
     */
    @Query("SELECT COUNT(c) FROM Camera c WHERE c.deletedAt IS NULL AND c.edgeNodeId = :edgeNodeId")
    long countByEdgeNodeIdAndDeletedAtIsNull(@Param("edgeNodeId") Long edgeNodeId);

    // ============ 绕过 @Where 过滤器的查询方法 ============

    /**
     * 根据ID查询摄像头（包括已删除的，用于恢复/强制删除操作）
     */
    @Query(value = "SELECT * FROM cameras WHERE id = :id", nativeQuery = true)
    Optional<Camera> findByIdIncludingDeleted(@Param("id") Long id);

    // ============ 使用 @EntityGraph 优化 N+1 查询 ============

    /**
     * 分页查询未删除摄像头（预加载关联数据）
     */
    @EntityGraph(attributePaths = {"edgeNode", "region"})
    @Query("SELECT c FROM Camera c WHERE c.deletedAt IS NULL")
    Page<Camera> findAllActiveWithDetails(Pageable pageable);

    /**
     * 根据ID查询未删除摄像头（预加载关联数据）
     */
    @EntityGraph(attributePaths = {"edgeNode", "region"})
    @Query("SELECT c FROM Camera c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Camera> findByIdAndDeletedAtIsNullWithDetails(@Param("id") Long id);

    /**
     * 查询特定节点上未删除的在线摄像头（预加载关联数据）
     */
    @EntityGraph(attributePaths = {"edgeNode", "region"})
    @Query("SELECT c FROM Camera c WHERE c.edgeNodeId = :edgeNodeId AND c.status = :status AND c.deletedAt IS NULL")
    List<Camera> findByEdgeNodeIdAndStatusWithDetails(@Param("edgeNodeId") Long edgeNodeId, @Param("status") Camera.CameraStatus status);
}