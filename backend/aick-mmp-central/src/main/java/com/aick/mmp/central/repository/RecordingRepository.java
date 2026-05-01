package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.Recording;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 录像仓库接口
 */
@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long> {

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
     * 查询过期的录像
     */
    Page<Recording> findExpiredRecordings(LocalDateTime cutoffDate, Pageable pageable);

    /**
     * 查询用于清理的录像（按开始时间升序，排除锁定的）
     */
    @Query("SELECT r FROM Recording r WHERE r.integrityStatus != 'DELETED' AND r.lockStatus != true ORDER BY r.startTime ASC")
    Page<Recording> findRecordingsForCleanup(Pageable pageable);

    /**
     * 按完整性状态查询录像
     */
    List<Recording> findByIntegrityStatus(String integrityStatus);
}
