package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.enums.ReportFormat;
import com.aick.mmp.shared.model.enums.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表订阅DTO
 */
@Data
public class ReportSubscriptionDTO {
    
    private Long id;
    
    /**
     * 订阅名称
     */
    private String name;
    
    /**
     * 报表类型
     */
    private ReportType reportType;
    
    /**
     * 报表格式
     */
    private ReportFormat format;
    
    /**
     * 包含的统计维度
     */
    private List<String> dimensions;
    
    /**
     * 过滤条件
     */
    private String filters;
    
    /**
     * 接收邮箱列表
     */
    private List<String> recipients;
    
    /**
     * 下次发送时间
     */
    private LocalDateTime nextSendTime;
    
    /**
     * 上次发送时间
     */
    private LocalDateTime lastSendTime;
    
    /**
     * 启用状态
     */
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
