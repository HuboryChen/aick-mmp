package com.aick.mmp.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class EdgeNodeStatusUpdateDTO {
    @NotNull(message = "Node ID cannot be null")
    private Long id;

    @NotNull(message = "Status cannot be null")
    private String status;

    private LocalDateTime lastHeartbeatTime;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double storageUsage;
    private Long networkBandwidth;
}