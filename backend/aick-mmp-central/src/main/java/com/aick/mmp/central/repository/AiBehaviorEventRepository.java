package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiBehaviorEventRepository extends JpaRepository<AiBehaviorEvent, Long> {
    List<AiBehaviorEvent> findByCameraIdOrderByEventTimeDesc(Long cameraId);
    List<AiBehaviorEvent> findByEventTypeAndStatus(String eventType, String status);
}
