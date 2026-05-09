package com.aick.mmp.central.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 视频墙预设排序请求数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频墙预设排序请求")
public class VideoWallPresetReorderDTO {
    
    @Schema(description = "预设ID列表，按新顺序排列", example = "[1, 3, 2, 4]")
    private List<Long> presetIds;
}
