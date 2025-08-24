package com.aick.mmp.repository;

import com.aick.mmp.model.StreamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {

    List<StreamSession> findByEdgeNodeId(String edgeNodeId);

    List<StreamSession> findByCdnNodeId(String cdnNodeId);

    List<StreamSession> findByStatus(StreamSession.StreamStatus status);

    void deleteBySessionId(String sessionId);
}