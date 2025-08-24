package com.aick.mmp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeDTO {
    private Long id;
    private String name;
    private String ipAddress;
    private Integer port;
    private String status;
    private String location;
    private Integer capacity;
    private Integer currentLoad;
    private String lastHeartbeat;
}