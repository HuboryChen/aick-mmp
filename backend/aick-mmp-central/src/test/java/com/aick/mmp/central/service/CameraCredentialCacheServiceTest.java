package com.aick.mmp.central.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraCredentialCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CameraCredentialCacheService cacheService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CameraCredentialCacheService(redisTemplate);
    }

    @Test
    void testCacheHit() {
        Long cameraId = 1L;
        String expectedPassword = "decrypted-password";
        when(valueOperations.get("camera:pwd:decrypted:1")).thenReturn(expectedPassword);

        String result = cacheService.getDecryptedPassword(cameraId);

        assertEquals(expectedPassword, result);
        verify(valueOperations).get("camera:pwd:decrypted:1");
    }

    @Test
    void testCacheMiss() {
        Long cameraId = 2L;
        when(valueOperations.get("camera:pwd:decrypted:2")).thenReturn(null);

        String result = cacheService.getDecryptedPassword(cameraId);

        assertNull(result);
        verify(valueOperations).get("camera:pwd:decrypted:2");
    }

    @Test
    void testCacheDecryptedPassword() {
        Long cameraId = 3L;
        String plaintext = "my-plaintext-password";

        cacheService.cacheDecryptedPassword(cameraId, plaintext);

        verify(valueOperations).set("camera:pwd:decrypted:3", plaintext, 1L, TimeUnit.HOURS);
    }

    @Test
    void testCacheNullPlaintext() {
        Long cameraId = 4L;

        cacheService.cacheDecryptedPassword(cameraId, null);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testInvalidateSingle() {
        Long cameraId = 5L;

        cacheService.invalidateCamera(cameraId);

        verify(redisTemplate).delete("camera:pwd:decrypted:5");
    }

    @Test
    void testInvalidateMultiple() {
        List<Long> cameraIds = List.of(6L, 7L, 8L);

        cacheService.invalidateCameras(cameraIds);

        verify(redisTemplate).delete(List.of("camera:pwd:decrypted:6", "camera:pwd:decrypted:7", "camera:pwd:decrypted:8"));
    }

    @Test
    void testInvalidateEmptyList() {
        cacheService.invalidateCameras(List.of());

        verify(redisTemplate, never()).delete(any());
    }

    @Test
    void testInvalidateNullList() {
        cacheService.invalidateCameras(null);

        verify(redisTemplate, never()).delete(any());
    }

    @Test
    void testCacheKeyPattern() {
        Long cameraId = 1L;

        when(valueOperations.get("camera:pwd:decrypted:1")).thenReturn("test");

        String result = cacheService.getDecryptedPassword(cameraId);

        assertEquals("test", result);
        verify(valueOperations).get("camera:pwd:decrypted:1");
    }
}
