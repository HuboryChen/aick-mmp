package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    
    Optional<Region> findByCode(String code);
    
    Optional<Region> findByIdAndIsDeletedFalse(Long id);
    
    List<Region> findByParentId(Long parentId);
    
    List<Region> findByParentIdAndIsDeletedFalse(Long parentId);
    
    List<Region> findByParentIdIsNullAndIsDeletedFalse();
    
    List<Region> findByIsDeletedFalseOrderBySortOrderAsc();
    
    List<Region> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name);
    
    List<Region> findByLevelAndIsDeletedFalse(Integer level);
    
    @Query("SELECT r FROM Region r WHERE r.isDeleted = false AND " +
           "(r.name LIKE %:keyword% OR r.code LIKE %:keyword%)")
    List<Region> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT COALESCE(MAX(r.level), 0) FROM Region r WHERE r.parentId = :parentId AND r.isDeleted = false")
    Integer findMaxLevelByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(r) FROM Region r WHERE r.parentId = :parentId AND r.isDeleted = false")
    long countByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(r) FROM Region r WHERE r.parentId = :parentId AND r.isDeleted = false AND r.id IN " +
           "(SELECT c.regionId FROM Camera c WHERE c.regionId IS NOT NULL AND c.isDeleted = false)")
    long countCamerasInRegion(@Param("parentId") Long parentId);
    
    @Query("SELECT DISTINCT r.level FROM Region r WHERE r.isDeleted = false ORDER BY r.level")
    List<Integer> findAllLevels();
    
    @Modifying
    @Query("UPDATE Region r SET r.isDeleted = true, r.deletedAt = CURRENT_TIMESTAMP WHERE r.parentId = :parentId")
    void softDeleteByParentId(@Param("parentId") Long parentId);
    
    @Query(value = "WITH RECURSIVE region_tree AS (" +
           "SELECT id, parent_id, name, code, level, path, sort_order FROM regions " +
           "WHERE id = :rootId AND is_deleted = false " +
           "UNION ALL " +
           "SELECT r.id, r.parent_id, r.name, r.code, r.level, r.path, r.sort_order " +
           "FROM regions r INNER JOIN region_tree rt ON r.parent_id = rt.id " +
           "WHERE r.is_deleted = false) " +
           "SELECT * FROM region_tree ORDER BY level, sort_order", nativeQuery = true)
    List<Region> findRegionTreeByRootId(@Param("rootId") Long rootId);
    
    @Query("SELECT r FROM Region r WHERE r.path LIKE :pathPrefix AND r.isDeleted = false")
    List<Region> findDescendantsByPath(@Param("pathPrefix") String pathPrefix);
    
    boolean existsByCodeAndIsDeletedFalse(String code);
}