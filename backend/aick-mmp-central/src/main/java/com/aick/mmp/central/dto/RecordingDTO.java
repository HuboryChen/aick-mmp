package com.aick.mmp.central.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RecordingDTO {
    private Long id;
    private Long cameraId;
    private String cameraName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration; // in seconds
    private Long size; // in bytes
    private String quality;
    private String storagePath;
    private LocalDateTime createdAt;
}