package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * 查询摄像头列表请求DTO
 *
 * @author huborychen
 * @version 1.0
 * @description 用于查询摄像头列表的请求参数，支持分页、状态、位置和边缘节点过滤
 * @date 2025/8/30 14:55
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCamerasRequestDTO {
    private Pageable pageable;
    private Camera.CameraStatus status;
    private String location;
    private Long regionId;
    private Long edgeNodeId;

}
