package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析数据请求DTO
 */
@Data
public class AnalyticsRequestDTO {
    
    /**
     * 统计类型
     */
    private AnalyticsType type;
    
    /**
     * 聚合粒度
     */
    private AggregationLevel level = AggregationLevel.HOUR;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 维度列表
     */
    private List<String> dimensions;
    
    /**
     * 过滤条件
     */
    private Map<String, Object> filters;
    
    /**
     * 是否包含额外数据
     */
    private boolean includeExtraData = false;
}
