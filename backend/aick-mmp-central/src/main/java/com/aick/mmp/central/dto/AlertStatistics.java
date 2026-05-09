package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 告警统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatistics {
    
    private long totalCount;
    private long unresolvedCount;
    private long acknowledgedCount;
    private long todayCount;
    private long criticalCount;
    private long warningCount;
    private long infoCount;
    
    private Map<String, Long> countByLevel;
    private Map<String, Long> countByType;
    private Map<String, Long> countByStatus;
}
