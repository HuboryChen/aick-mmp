package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchOperationDTO {
    private BatchOperationType operation;
    
    @NotEmpty(message = "ID列表不能为空")
    private List<Long> userIds;
    
    private List<Long> cameraIds;
    
    private Long edgeNodeId;
    
    private String role;
    
    public enum BatchOperationType {
        DELETE,
        ENABLE,
        DISABLE,
        UPDATE_EDGE_NODE,
        UPDATE_ROLE
    }
}
