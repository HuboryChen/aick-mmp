package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiVehicleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiVehicleRecordRepository extends JpaRepository<AiVehicleRecord, Long> {
    List<AiVehicleRecord> findByPlateNumberOrderByDetectTimeDesc(String plateNumber);
    List<AiVehicleRecord> findByCameraIdOrderByDetectTimeDesc(Long cameraId);
}
