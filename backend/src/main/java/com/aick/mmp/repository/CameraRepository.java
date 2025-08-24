package com.aick.mmp.repository;

import com.aick.mmp.model.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {
    Page<Camera> findByLocation(String location, Pageable pageable);
    Page<Camera> findByEdgeNodeId(Long edgeNodeId, Pageable pageable);
    Page<Camera> findByStatus(Camera.CameraStatus status, Pageable pageable);
    List<Camera> findByEdgeNodeIdAndStatus(Long edgeNodeId, Camera.CameraStatus status);
    Optional<Camera> findByConnectionUrl(String connectionUrl);
    boolean existsByConnectionUrl(String connectionUrl);
    long countByStatus(Camera.CameraStatus status);
    long countByEdgeNodeId(Long edgeNodeId);
}