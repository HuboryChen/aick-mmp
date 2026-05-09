package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanProgressDTO {
    private Long taskId;
    private Integer progress;
    private Integer totalIps;
    private Integer scannedIps;
    private List<Map<String, Object>> foundDevices;
    private String status;
}
