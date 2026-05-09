package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionStatsDTO {
    private Long regionId;
    private String regionName;
    private Long totalCameras;
    private Long onlineCameras;
    private Long offlineCameras;
    private Long childRegions;
    
    // Edge node statistics (recursive - includes all child regions)
    private Long edgeNodeCount;
    
    // CDN node statistics (recursive - includes all child regions)
    private Long cdnNodeCount;
    
    // Direct statistics (only this region, excludes child regions)
    private Long directEdgeNodeCount;
    private Long directCdnNodeCount;
}
