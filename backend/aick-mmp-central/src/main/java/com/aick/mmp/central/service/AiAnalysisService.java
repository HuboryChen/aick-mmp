package com.aick.mmp.central.service;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;

import java.time.LocalDateTime;
import java.util.List;

public interface AiAnalysisService {
    List<AiPassengerStats> getPassengerStats(Long cameraId, LocalDateTime from, LocalDateTime to);
    String getRealtimePassenger(Long cameraId);

    List<AiBehaviorEvent> getBehaviorEvents(Long cameraId, String eventType, String status);
    AiBehaviorEvent updateBehaviorStatus(Long id, String status);

    List<AiVehicleRecord> getVehicleRecords(String plateNumber, Long cameraId);

    List<AiVehicleWhitelist> getAllWhitelist();
    AiVehicleWhitelist addWhitelist(AiVehicleWhitelist entry);
    AiVehicleWhitelist updateWhitelist(Long id, AiVehicleWhitelist entry);
    void deleteWhitelist(Long id);
}
