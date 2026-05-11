package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.repository.AiPassengerStatsRepository;
import com.aick.mmp.central.repository.AiBehaviorEventRepository;
import com.aick.mmp.central.repository.AiVehicleRecordRepository;
import com.aick.mmp.central.repository.AiVehicleWhitelistRepository;
import com.aick.mmp.central.service.AiAnalysisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {
    private static final String REDIS_KEY = "ai:passenger:realtime:%s";

    private final AiPassengerStatsRepository passengerRepo;
    private final AiBehaviorEventRepository behaviorRepo;
    private final AiVehicleRecordRepository vehicleRepo;
    private final AiVehicleWhitelistRepository whitelistRepo;
    private final StringRedisTemplate redisTemplate;

    public AiAnalysisServiceImpl(AiPassengerStatsRepository passengerRepo,
                                  AiBehaviorEventRepository behaviorRepo,
                                  AiVehicleRecordRepository vehicleRepo,
                                  AiVehicleWhitelistRepository whitelistRepo,
                                  StringRedisTemplate redisTemplate) {
        this.passengerRepo = passengerRepo;
        this.behaviorRepo = behaviorRepo;
        this.vehicleRepo = vehicleRepo;
        this.whitelistRepo = whitelistRepo;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<AiPassengerStats> getPassengerStats(Long cameraId, LocalDateTime from, LocalDateTime to) {
        return passengerRepo.findByCameraIdAndStartTimeBetween(cameraId, from, to);
    }

    @Override
    public String getRealtimePassenger(Long cameraId) {
        String val = redisTemplate.opsForValue().get(String.format(REDIS_KEY, cameraId));
        return val != null ? val : "0";
    }

    @Override
    public List<AiBehaviorEvent> getBehaviorEvents(Long cameraId, String eventType, String status) {
        if (eventType != null && status != null) {
            return behaviorRepo.findByEventTypeAndStatus(eventType, status);
        }
        return behaviorRepo.findByCameraIdOrderByEventTimeDesc(cameraId);
    }

    @Override
    @Transactional
    public AiBehaviorEvent updateBehaviorStatus(Long id, String status) {
        AiBehaviorEvent event = behaviorRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Behavior event not found: " + id));
        event.setStatus(status);
        return behaviorRepo.save(event);
    }

    @Override
    public List<AiVehicleRecord> getVehicleRecords(String plateNumber, Long cameraId) {
        if (plateNumber != null && !plateNumber.isEmpty()) {
            return vehicleRepo.findByPlateNumberOrderByDetectTimeDesc(plateNumber);
        }
        return vehicleRepo.findByCameraIdOrderByDetectTimeDesc(cameraId);
    }

    @Override
    public List<AiVehicleWhitelist> getAllWhitelist() {
        return whitelistRepo.findAll();
    }

    @Override
    public AiVehicleWhitelist addWhitelist(AiVehicleWhitelist entry) {
        return whitelistRepo.save(entry);
    }

    @Override
    public AiVehicleWhitelist updateWhitelist(Long id, AiVehicleWhitelist entry) {
        AiVehicleWhitelist existing = whitelistRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Whitelist entry not found: " + id));
        existing.setPlateNumber(entry.getPlateNumber());
        existing.setPlateColor(entry.getPlateColor());
        existing.setOwnerName(entry.getOwnerName());
        existing.setDescription(entry.getDescription());
        existing.setEnabled(entry.getEnabled());
        return whitelistRepo.save(existing);
    }

    @Override
    public void deleteWhitelist(Long id) {
        whitelistRepo.deleteById(id);
    }
}
