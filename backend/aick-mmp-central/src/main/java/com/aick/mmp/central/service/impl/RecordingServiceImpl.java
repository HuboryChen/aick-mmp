package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.RecordingDTO;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.RecordingQueryParams;
import com.aick.mmp.central.service.RecordingService;
import com.aick.mmp.shared.model.Recording;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingServiceImpl implements RecordingService {

    private final RecordingRepository recordingRepository;

    @Override
    public Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        RecordingQueryParams params = RecordingQueryParams.builder()
                .cameraId(cameraId)
                .location(location)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return getRecordings(params, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordingDTO> getRecordings(RecordingQueryParams params, Pageable pageable) {
        // 使用增强的查询方法
        Page<Recording> recordings = recordingRepository.findByEnhancedParams(params, pageable);
        
        List<RecordingDTO> dtos = recordings.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, recordings.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable, boolean includeDeleted) {
        Page<Recording> recordings;

        if (includeDeleted) {
            // 查询所有录像（包括已删除的）
            recordings = recordingRepository.findAll(pageable);
        } else {
            // 默认只查询未删除的录像（@Where 自动过滤）
            recordings = recordingRepository.findAll(pageable);
        }

        List<RecordingDTO> dtos = recordings.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, recordings.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingDTO getRecordingById(Long id) {
        return getRecordingById(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingDTO getRecordingById(Long id, boolean includeDeleted) {
        Recording recording;
        if (includeDeleted) {
            recording = recordingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Recording not found: " + id));
        } else {
            recording = recordingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Recording not found: " + id));
        }
        return toDTO(recording);
    }

    @Override
    public String getRecordingUrl(Long recordingId) {
        // 返回录像流URL
        return "/api/recordings/" + recordingId + "/stream";
    }

    @Override
    @Transactional
    public void deleteRecording(Long recordingId) {
        // 物理删除录像记录和文件
        recordingRepository.deleteById(recordingId);
        log.info("Recording physically deleted: {}", recordingId);
    }

    @Override
    @Transactional
    public void softDelete(Long recordingId) {
        int updated = recordingRepository.softDelete(recordingId, LocalDateTime.now());
        if (updated > 0) {
            log.info("Recording soft deleted: {}", recordingId);
        } else {
            throw new RuntimeException("Recording not found for soft delete: " + recordingId);
        }
    }

    @Override
    @Transactional
    public void restore(Long recordingId) {
        int updated = recordingRepository.restore(recordingId);
        if (updated > 0) {
            log.info("Recording restored: {}", recordingId);
        } else {
            throw new RuntimeException("Recording not found for restore: " + recordingId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordingDTO> getRecordingsByCameraId(Long cameraId) {
        List<Recording> recordings = recordingRepository.findByCameraId(cameraId);
        return recordings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordingDTO> getOrphanedRecordings(Pageable pageable) {
        Page<Recording> orphanedRecordings = recordingRepository.findOrphanedRecordings(pageable);
        List<RecordingDTO> dtos = orphanedRecordings.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, orphanedRecordings.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecordingDTO> getDeletedRecordings(Pageable pageable) {
        Page<Recording> deletedRecordings = recordingRepository.findDeletedRecordings(pageable);
        List<RecordingDTO> dtos = deletedRecordings.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, deletedRecordings.getTotalElements());
    }

    @Override
    @Transactional
    public int cleanupOrphanedRecordings(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        Page<Recording> recordingsToCleanup = recordingRepository.findOrphanedRecordingsForCleanup(cutoffDate, Pageable.unpaged());

        int count = 0;
        for (Recording recording : recordingsToCleanup.getContent()) {
            // 实际应该删除文件
            recordingRepository.delete(recording);
            count++;
        }

        log.info("Cleaned up {} orphaned recordings older than {} days", count, daysOld);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrphanedRecordings() {
        return recordingRepository.countOrphanedRecordings();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalRecordingSize() {
        return 0L; // TODO: 实现实际统计
    }

    /**
     * 将 Recording 实体转换为 DTO（包含增强的元数据）
     */
    private RecordingDTO toDTO(Recording recording) {
        return RecordingDTO.builder()
                .id(recording.getId())
                .cameraId(recording.getCameraId())
                .startTime(recording.getStartTime())
                .endTime(recording.getEndTime())
                .duration(recording.getDuration() != null ? recording.getDuration().longValue() : null)
                .size(recording.getFileSize())
                .quality(recording.getResolution())
                .storagePath(recording.getStoragePath())
                .createdAt(recording.getCreatedAt())
                // 增强元数据
                .status(recording.getStatus())
                .recordingType(recording.getRecordingType())
                .format(recording.getFormat())
                .md5(recording.getMd5())
                .integrityStatus(recording.getIntegrityStatus())
                .lockStatus(recording.getLockStatus())
                .updatedAt(recording.getUpdatedAt())
                // 软删除支持字段
                .isDeleted(recording.getIsDeleted())
                .deletedAt(recording.getDeletedAt())
                .orphanedAt(recording.getOrphanedAt())
                .build();
    }
}