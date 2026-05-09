package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RecordingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface RecordingService {
    /**
     * 查询录像（增强版，支持状态过滤、文件大小范围过滤）
     */
    Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询录像（增强版，支持状态过滤、文件大小范围过滤）
     */
    Page<RecordingDTO> getRecordings(RecordingQueryParams params, Pageable pageable);

    /**
     * 查询录像（支持包含已删除的录像）
     */
    Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable, boolean includeDeleted);

    RecordingDTO getRecordingById(Long id);

    /**
     * 根据ID查询录像（支持包含已删除的录像）
     */
    RecordingDTO getRecordingById(Long id, boolean includeDeleted);

    String getRecordingUrl(Long recordingId);
    void deleteRecording(Long recordingId);

    /**
     * 软删除录像
     */
    void softDelete(Long recordingId);

    /**
     * 恢复已软删除的录像
     */
    void restore(Long recordingId);

    List<RecordingDTO> getRecordingsByCameraId(Long cameraId);

    /**
     * 查询孤立录像
     */
    Page<RecordingDTO> getOrphanedRecordings(Pageable pageable);

    /**
     * 查询已删除的录像
     */
    Page<RecordingDTO> getDeletedRecordings(Pageable pageable);

    /**
     * 批量清理孤立录像
     */
    int cleanupOrphanedRecordings(int daysOld);

    /**
     * 统计孤立录像数量
     */
    long countOrphanedRecordings();

    long getTotalRecordingSize();
}