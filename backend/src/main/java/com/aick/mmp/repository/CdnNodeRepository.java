package com.aick.mmp.repository;

import com.aick.mmp.model.CdnNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CdnNodeRepository extends JpaRepository<CdnNode, Long> {
    boolean existsByIpAddress(String ipAddress);
    Page<CdnNode> findByRegion(String region, Pageable pageable);
    Page<CdnNode> findByStatus(CdnNode.NodeStatus status, Pageable pageable);
    List<CdnNode> findByRegionAndStatusOrderByCurrentLoadAsc(String region, CdnNode.NodeStatus status);
    List<CdnNode> findByStatusOrderByCurrentLoadAsc(CdnNode.NodeStatus status);
    Optional<CdnNode> findByNodeId(String nodeId);
    CdnNode findByIpAddressAndPort(String ipAddress, Integer port);
}