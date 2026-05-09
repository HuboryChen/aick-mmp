package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgressDTO {
    private Long taskId;
    private String status;
    private Integer progress;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failCount;
    private String currentRecord;
}
