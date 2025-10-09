package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RecordingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface RecordingService {
    Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    RecordingDTO getRecordingById(Long id);
    String getRecordingUrl(Long recordingId);
    void deleteRecording(Long recordingId);
    List<RecordingDTO> getRecordingsByCameraId(Long cameraId);
    long getTotalRecordingSize();
}