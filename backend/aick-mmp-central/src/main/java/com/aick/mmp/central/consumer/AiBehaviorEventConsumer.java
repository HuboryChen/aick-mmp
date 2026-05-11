package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.repository.AiBehaviorEventRepository;
import com.aick.mmp.central.service.AlertNotificationService;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AiBehaviorEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiBehaviorEventConsumer.class);

    private final AiBehaviorEventRepository repository;
    private final AlertNotificationService alertService;
    private final ObjectMapper objectMapper;

    public AiBehaviorEventConsumer(AiBehaviorEventRepository repository,
                                   AlertNotificationService alertService,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-behavior-events", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            String eventType = json.get("event_type").asText();
            String level = json.get("level").asText();
            String description = json.get("description").asText();

            AiBehaviorEvent event = new AiBehaviorEvent();
            event.setCameraId(cameraId);
            event.setEventType(eventType);
            event.setLevel(level);
            event.setDescription(description);
            event.setEventTime(LocalDateTime.now());
            event.setStatus("UNRESOLVED");
            event = repository.save(event);

            AlertRecord alert = new AlertRecord();
            alert.setRuleId(0L);
            alert.setRuleName("AI Behavior: " + eventType);
            alert.setAlertType(AlertRule.AlertType.CUSTOM);
            try {
                alert.setLevel(AlertRule.AlertLevel.valueOf(level));
            } catch (IllegalArgumentException e) {
                alert.setLevel(AlertRule.AlertLevel.WARNING);
            }
            alert.setTitle("AI Behavior Alert: " + eventType);
            alert.setMessage(description);
            alert.setCameraId(cameraId);
            alert.setSource("AI_SERVICE");
            alert.setTargetType(AlertRule.TargetType.CAMERA);
            alert.setTargetId(cameraId);
            alertService.sendAlertNotification(alert);

            log.info("Behavior alert processed: {} for camera {}", eventType, cameraId);
        } catch (Exception e) {
            log.error("Failed to process behavior event: {}", e.getMessage());
        }
    }
}
