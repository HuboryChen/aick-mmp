package com.aick.mmp.repository;

import com.aick.mmp.dto.EdgeNodeDTO;
import com.aick.mmp.model.EdgeNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdgeNodeRepository extends JpaRepository<EdgeNode, Long> {
    Page<EdgeNode> findByLocation(String location, Pageable pageable);
    Page<EdgeNode> findByStatus(EdgeNode.NodeStatus status, Pageable pageable);
    Optional<EdgeNode> findByIpAddressAndPort(String ipAddress, Integer port);
    Optional<EdgeNode> findByName(String name);
    List<EdgeNode> findByStatusAndEnabled(EdgeNode.NodeStatus status, boolean enabled);
    long countByStatus(EdgeNode.NodeStatus status);
    boolean existsByName(String name);
    boolean existsByIpAddressAndPort(String ipAddress, Integer port);
    Optional<EdgeNodeDTO> findByIpAddress(String ipAddress);
}