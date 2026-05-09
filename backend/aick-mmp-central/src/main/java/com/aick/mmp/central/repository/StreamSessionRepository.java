package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.StreamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {

    List<StreamSession> findByEdgeNodeId(Long edgeNodeId);

    List<StreamSession> findByCdnNodeId(String cdnNodeId);

    List<StreamSession> findByStatus(StreamSession.StreamStatus status);

    Optional<StreamSession> findBySessionId(String sessionId);

    Optional<StreamSession> findByCameraId(Long cameraId);

    void deleteBySessionId(String sessionId);
}