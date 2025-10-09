package com.aick.mmp.central.dto;

import com.aick.mmp.shared.model.Camera;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * @author huborychen
 * @version 1.0
 * @description TODO
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
    private Long edgeNodeId;

}
