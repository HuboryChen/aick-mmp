package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.repository.AiVehicleRecordRepository;
import com.aick.mmp.central.repository.AiVehicleWhitelistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AiVehicleRecordConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiVehicleRecordConsumer.class);

    private final AiVehicleRecordRepository repository;
    private final AiVehicleWhitelistRepository whitelistRepository;
    private final ObjectMapper objectMapper;

    public AiVehicleRecordConsumer(AiVehicleRecordRepository repository,
                                   AiVehicleWhitelistRepository whitelistRepository,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.whitelistRepository = whitelistRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-vehicle-records", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            String plateNumber = json.get("plate_number").asText();

            Optional<AiVehicleWhitelist> whitelistEntry = whitelistRepository.findByPlateNumber(plateNumber);

            AiVehicleRecord record = new AiVehicleRecord();
            record.setCameraId(cameraId);
            record.setPlateNumber(plateNumber);
            record.setPlateColor(json.has("plate_color") ? json.get("plate_color").asText() : null);
            record.setConfidence(json.has("confidence") ? BigDecimal.valueOf(json.get("confidence").asDouble()) : null);
            record.setDetectTime(LocalDateTime.now());
            record.setIsWhitelisted(whitelistEntry.isPresent() && whitelistEntry.get().getEnabled());
            record.setIsBlacklisted(false);
            repository.save(record);

            if (whitelistEntry.isPresent() && whitelistEntry.get().getEnabled()) {
                log.info("Whitelisted vehicle detected: {}", plateNumber);
            }
        } catch (Exception e) {
            log.error("Failed to process vehicle record: {}", e.getMessage());
        }
    }
}
