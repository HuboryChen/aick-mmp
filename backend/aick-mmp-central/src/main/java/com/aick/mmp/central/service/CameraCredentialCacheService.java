package com.aick.mmp.central.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CameraCredentialCacheService {

    private static final String CACHE_KEY_PREFIX = "camera:pwd:decrypted:";
    private static final long CACHE_TTL_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;

    public String getDecryptedPassword(Long cameraId) {
        String cacheKey = buildKey(cameraId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for camera credential: {}", cameraId);
        }
        return cached;
    }

    public void cacheDecryptedPassword(Long cameraId, String plaintext) {
        if (plaintext == null) return;
        String cacheKey = buildKey(cameraId);
        redisTemplate.opsForValue().set(cacheKey, plaintext, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Cached decrypted password for camera: {} (TTL: {}h)", cameraId, CACHE_TTL_HOURS);
    }

    public void invalidateCamera(Long cameraId) {
        String cacheKey = buildKey(cameraId);
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated cached password for camera: {}", cameraId);
    }

    public void invalidateCameras(List<Long> cameraIds) {
        if (cameraIds == null || cameraIds.isEmpty()) return;
        List<String> keys = cameraIds.stream()
                .map(this::buildKey)
                .toList();
        redisTemplate.delete(keys);
        log.debug("Invalidated cached passwords for {} cameras", cameraIds.size());
    }

    private String buildKey(Long cameraId) {
        return CACHE_KEY_PREFIX + cameraId;
    }
}
