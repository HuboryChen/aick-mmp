package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiAnalysisConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAnalysisConfigRepository extends JpaRepository<AiAnalysisConfig, Long> {
    Optional<AiAnalysisConfig> findByCameraId(Long cameraId);
    void deleteByCameraId(Long cameraId);
}
