package com.aick.mmp.central.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Janus Gateway 健康检查指示器
 */
@Component("janus")
@RequiredArgsConstructor
@Slf4j
public class JanusHealthIndicator implements HealthIndicator {

    private final JanusClient janusClient;

    @Override
    public Health health() {
        try {
            if (janusClient.isHealthy()) {
                Map<String, Object> info = janusClient.getInfo();
                
                return Health.up()
                        .withDetail("status", "running")
                        .withDetail("activeSessions", info.getOrDefault("activeSessions", 0))
                        .withDetail("activeHandles", info.getOrDefault("activeHandles", 0))
                        .withDetail("version", info.getOrDefault("version", "unknown"))
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "unreachable")
                        .withDetail("error", "Janus Gateway is not responding")
                        .build();
            }
        } catch (Exception e) {
            log.error("Janus health check failed", e);
            return Health.down()
                    .withDetail("status", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
