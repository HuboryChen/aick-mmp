package com.aick.mmp.central.service;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 录像查询参数
 */
@Data
@Builder
public class RecordingQueryParams {
    /**
     * 摄像头ID
     */
    private Long cameraId;
    
    /**
     * 位置
     */
    private String location;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 录像状态过滤 (PENDING, RECORDING, COMPLETED, CORRUPTED, DELETED)
     */
    private String status;
    
    /**
     * 完整性状态过滤
     */
    private String integrityStatus;
    
    /**
     * 录像类型过滤 (continuous, timed, motion, event)
     */
    private String recordingType;
    
    /**
     * 文件大小最小值（字节）
     */
    private Long minFileSize;
    
    /**
     * 文件大小最大值（字节）
     */
    private Long maxFileSize;
}
