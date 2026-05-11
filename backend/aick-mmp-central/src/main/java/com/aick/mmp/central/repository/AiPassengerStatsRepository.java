package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiPassengerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiPassengerStatsRepository extends JpaRepository<AiPassengerStats, Long> {
    List<AiPassengerStats> findByCameraIdAndStartTimeBetween(Long cameraId, LocalDateTime from, LocalDateTime to);
}
