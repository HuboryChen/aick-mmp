package com.aick.mmp.central.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class CameraStatusUpdateDTO {
    @NotNull(message = "Camera ID cannot be null")
    private Long id;

    @NotNull(message = "Status cannot be null")
    private String status;

    private LocalDateTime lastActiveTime;
    private Integer bitrate;
    private String resolution;
    private Integer frameRate;
}