package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.ReportFormat;
import com.aick.mmp.shared.model.enums.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表请求DTO
 */
@Data
public class ReportRequestDTO {
    
    /**
     * 报表类型
     */
    private ReportType reportType;
    
    /**
     * 报表格式
     */
    private ReportFormat format = ReportFormat.EXCEL;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 包含的统计维度
     */
    private List<String> dimensions;
    
    /**
     * 过滤条件
     */
    private Map<String, Object> filters;
    
    /**
     * 报表标题
     */
    private String title;
    
    /**
     * 描述
     */
    private String description;
}
