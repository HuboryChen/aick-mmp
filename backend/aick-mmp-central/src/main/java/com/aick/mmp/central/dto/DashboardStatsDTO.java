package com.aick.mmp.central.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalCameras;
    private long onlineCameras;
    private long totalEdgeNodes;
    private long onlineEdgeNodes;
    private long totalStreams;
    private long activeStreams;
    private long onlineUsers;
}