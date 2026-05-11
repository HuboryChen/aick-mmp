package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.repository.AiPassengerStatsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class AiPassengerStatsConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiPassengerStatsConsumer.class);
    private static final String REDIS_KEY = "ai:passenger:realtime:%s";

    private final AiPassengerStatsRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiPassengerStatsConsumer(AiPassengerStatsRepository repository,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-passenger-stats", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            int enterCount = json.get("enter_count").asInt();
            int exitCount = json.get("exit_count").asInt();
            int insideCount = json.get("inside_count").asInt();

            redisTemplate.opsForValue().set(
                String.format(REDIS_KEY, cameraId),
                String.valueOf(insideCount),
                5, TimeUnit.MINUTES
            );

            AiPassengerStats stats = new AiPassengerStats();
            stats.setCameraId(cameraId);
            stats.setStartTime(LocalDateTime.now().minusMinutes(1));
            stats.setEndTime(LocalDateTime.now());
            stats.setEnterCount(enterCount);
            stats.setExitCount(exitCount);
            stats.setInsideCount(insideCount);
            repository.save(stats);

        } catch (Exception e) {
            log.error("Failed to process passenger stats: {}", e.getMessage());
        }
    }
}
