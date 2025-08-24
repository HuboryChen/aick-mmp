package com.aick.mmp.service;


import com.aick.mmp.dto.CdnNodeDTO;
import com.aick.mmp.model.CdnNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CdnNodeService {
    Page<CdnNodeDTO> getAllCdnNodes(Pageable pageable);

    Page<CdnNodeDTO> getCdnNodesByRegion(String region, Pageable pageable);

    Page<CdnNodeDTO> getCdnNodesByStatus(CdnNode.NodeStatus status, Pageable pageable);

    CdnNodeDTO getCdnNodeById(Long id);

    CdnNodeDTO createCdnNode(CdnNodeDTO cdnNodeDTO);

    CdnNodeDTO updateCdnNode(Long id, CdnNodeDTO cdnNodeDTO);

    void updateCdnNodeStatus(Long id, String status, String message);

    void deleteCdnNode(Long id);

    void registerHeartbeat(String nodeId, Map<String, Object> metrics);

    List<CdnNodeDTO> getBestCdnNodesForRegion(String region, int count);

    Map<String, Object> getCdnNodeStatistics(Long nodeId);

}