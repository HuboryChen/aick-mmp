package com.aick.mmp.central.repository;

import com.aick.mmp.central.service.RecordingQueryParams;
import com.aick.mmp.shared.model.Recording;
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
 * 录像仓库接口
 */
@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long>, JpaSpecificationExecutor<Recording> {

    /**
     * 按时间范围查询
     */
    List<Recording> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 按摄像头ID查询
     */
    List<Recording> findByCameraId(Long cameraId);

    /**
     * 按摄像头ID和时间范围查询
     */
    List<Recording> findByCameraIdAndStartTimeBetween(Long cameraId, LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间范围的录像数量
     */
    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定摄像头的录像数量
     */
    long countByCameraId(Long cameraId);

    /**
     * 计算总文件大小
     */
    @Query("SELECT COALESCE(SUM(r.fileSize), 0) FROM Recording r WHERE r.startTime BETWEEN :start AND :end")
    Long sumFileSizeByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询过期的录像（按开始时间判断）
     */
    @Query("SELECT r FROM Recording r WHERE r.startTime < :cutoffDate")
    Page<Recording> findExpiredRecordings(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * 查询用于清理的录像（按开始时间升序，排除锁定的）
     */
    @Query("SELECT r FROM Recording r WHERE r.integrityStatus != 'DELETED' AND r.lockStatus != true ORDER BY r.startTime ASC")
    Page<Recording> findRecordingsForCleanup(Pageable pageable);

    /**
     * 按完整性状态查询录像
     */
    List<Recording> findByIntegrityStatus(String integrityStatus);

    // ========== 增强查询支持 ==========

    /**
     * 增强查询录像（支持状态过滤、文件大小范围过滤）
     */
    @Query("SELECT r FROM Recording r WHERE r.isDeleted = false " +
           "AND (:cameraId IS NULL OR r.cameraId = :cameraId) " +
           "AND (:startTime IS NULL OR r.startTime >= :startTime) " +
           "AND (:endTime IS NULL OR r.startTime <= :endTime) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:integrityStatus IS NULL OR r.integrityStatus = :integrityStatus) " +
           "AND (:recordingType IS NULL OR r.recordingType = :recordingType) " +
           "AND (:minFileSize IS NULL OR r.fileSize >= :minFileSize) " +
           "AND (:maxFileSize IS NULL OR r.fileSize <= :maxFileSize)")
    Page<Recording> findByEnhancedParams(
            @Param("cameraId") Long cameraId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") String status,
            @Param("integrityStatus") String integrityStatus,
            @Param("recordingType") String recordingType,
            @Param("minFileSize") Long minFileSize,
            @Param("maxFileSize") Long maxFileSize,
            Pageable pageable);

    /**
     * 使用查询参数对象进行增强查询
     */
    default Page<Recording> findByEnhancedParams(RecordingQueryParams params, Pageable pageable) {
        return findByEnhancedParams(
                params.getCameraId(),
                params.getStartTime(),
                params.getEndTime(),
                params.getStatus(),
                params.getIntegrityStatus(),
                params.getRecordingType(),
                params.getMinFileSize(),
                params.getMaxFileSize(),
                pageable
        );
    }

    // ========== 软删除支持方法 ==========

    /**
     * 查询已软删除的录像（包含已孤立）
     */
    @Query("SELECT r FROM Recording r WHERE r.isDeleted = true")
    Page<Recording> findDeletedRecordings(Pageable pageable);

    /**
     * 查询孤立录像（已软删除且有孤立标记）
     */
    @Query("SELECT r FROM Recording r WHERE r.isDeleted = true AND r.orphanedAt IS NOT NULL")
    Page<Recording> findOrphanedRecordings(Pageable pageable);

    /**
     * 根据ID查询已删除的录像（用于恢复）
     */
    @Query("SELECT r FROM Recording r WHERE r.id = :id AND r.isDeleted = true")
    Optional<Recording> findDeletedById(@Param("id") Long id);

    /**
     * 标记录像为已删除
     */
    @Modifying
    @Query("UPDATE Recording r SET r.isDeleted = true, r.deletedAt = :deletedAt WHERE r.id = :id")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 恢复已软删除的录像
     */
    @Modifying
    @Query("UPDATE Recording r SET r.isDeleted = false, r.deletedAt = NULL, r.orphanedAt = NULL, r.orphanedBy = NULL WHERE r.id = :id")
    int restore(@Param("id") Long id);

    /**
     * 按摄像头ID批量标记录像为孤立状态
     */
    @Modifying
    @Query("UPDATE Recording r SET r.orphanedAt = :orphanedAt, r.orphanedBy = :orphanedBy WHERE r.cameraId = :cameraId AND r.isDeleted = false")
    int markOrphanedByCameraId(@Param("cameraId") Long cameraId, @Param("orphanedAt") LocalDateTime orphanedAt, @Param("orphanedBy") Long orphanedBy);

    /**
     * 查询超过指定天数的孤立录像（用于清理）
     */
    @Query("SELECT r FROM Recording r WHERE r.isDeleted = true AND r.orphanedAt IS NOT NULL AND r.orphanedAt < :cutoffDate")
    Page<Recording> findOrphanedRecordingsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * 统计孤立录像数量
     */
    @Query("SELECT COUNT(r) FROM Recording r WHERE r.isDeleted = true AND r.orphanedAt IS NOT NULL")
    long countOrphanedRecordings();

    /**
     * 统计已删除录像数量
     */
    @Query("SELECT COUNT(r) FROM Recording r WHERE r.isDeleted = true")
    long countDeletedRecordings();

    /**
     * 统计总存储大小（不包含已软删除的录像）
     */
    @Query("SELECT COALESCE(SUM(r.fileSize), 0) FROM Recording r WHERE r.isDeleted = false")
    Long sumTotalStorageSize();

    /**
     * 查询指定摄像头的所有录像（包括已删除的，用于彻底清理）
     */
    @Query("SELECT r FROM Recording r WHERE r.cameraId = :cameraId")
    List<Recording> findAllByCameraIdIncludingDeleted(@Param("cameraId") Long cameraId);
}
