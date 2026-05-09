package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警处理请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRecordRequest {

    private String resolutionNote;
}
