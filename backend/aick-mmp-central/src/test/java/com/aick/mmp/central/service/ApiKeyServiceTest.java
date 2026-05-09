package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ApiKeyCreatedResponseDTO;
import com.aick.mmp.central.dto.ApiKeyDTO;
import com.aick.mmp.central.dto.CreateApiKeyRequestDTO;
import com.aick.mmp.central.repository.ApiKeyRepository;
import com.aick.mmp.central.repository.SystemAppRepository;
import com.aick.mmp.central.service.impl.ApiKeyServiceImpl;
import com.aick.mmp.shared.model.ApiKey;
import com.aick.mmp.shared.model.SystemApp;
import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import com.aick.mmp.shared.model.enums.OwnerType;
import com.aick.mmp.shared.util.AESEncryptionUtil;
import com.aick.mmp.shared.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyService
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SystemAppRepository systemAppRepository;

    @Mock
    private AESEncryptionUtil encryptionUtil;

    @Mock
    private SignatureUtil signatureUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private static final String TEST_ACCESS_KEY = "ak_test12345678901234567890123456";
    private static final String TEST_SECRET_KEY = "sk_test12345678901234567890123456";
    private static final String ENCRYPTED_SECRET = "encryptedSecret123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiKeyService, "timestampToleranceSeconds", 300L);
    }

    @Test
    @DisplayName("Should create API key for user successfully")
    void testCreateApiKeyForUser() {
        CreateApiKeyRequestDTO request = CreateApiKeyRequestDTO.builder()
                .name("Test Key")
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        when(encryptionUtil.encrypt(anyString())).thenReturn(ENCRYPTED_SECRET);
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        ApiKeyCreatedResponseDTO result = apiKeyService.createApiKeyForUser(1L, request);

        assertNotNull(result);
        assertNotNull(result.getAccessKey());
        assertNotNull(result.getSecretKey());
        assertTrue(result.getAccessKey().startsWith("ak_"));
        assertTrue(result.getSecretKey().startsWith("sk_"));
        assertEquals("Test Key", result.getName());

        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("Should return cached secret key when available")
    void testGetDecryptedSecretKeyCacheHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(TEST_SECRET_KEY);

        Optional<String> result = apiKeyService.getDecryptedSecretKey(TEST_ACCESS_KEY);

        assertTrue(result.isPresent());
        assertEquals(TEST_SECRET_KEY, result.get());

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(contains(TEST_ACCESS_KEY));
        verify(apiKeyRepository, never()).findByAccessKey(any());
    }

    @Test
    @DisplayName("Should decrypt and cache secret key on cache miss")
    void testGetDecryptedSecretKeyCacheMiss() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .encryptedSecret(ENCRYPTED_SECRET)
                .status(ApiKeyStatus.ENABLED)
                .type(ApiKeyType.USER)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findByAccessKey(TEST_ACCESS_KEY)).thenReturn(Optional.of(apiKey));
        when(encryptionUtil.decrypt(ENCRYPTED_SECRET)).thenReturn(TEST_SECRET_KEY);

        Optional<String> result = apiKeyService.getDecryptedSecretKey(TEST_ACCESS_KEY);

        assertTrue(result.isPresent());
        assertEquals(TEST_SECRET_KEY, result.get());

        verify(valueOperations).set(contains(TEST_ACCESS_KEY), eq(TEST_SECRET_KEY), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should return empty when API key not found")
    void testGetDecryptedSecretKeyNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findByAccessKey(anyString())).thenReturn(Optional.empty());

        Optional<String> result = apiKeyService.getDecryptedSecretKey("invalid_key");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should list API keys by user")
    void testListApiKeysByUser() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .name("User Key")
                .type(ApiKeyType.USER)
                .status(ApiKeyStatus.ENABLED)
                .userId(1L)
                .build();

        when(apiKeyRepository.findByUserId(1L)).thenReturn(List.of(apiKey));

        List<ApiKeyDTO> result = apiKeyService.listApiKeysByUser(1L);

        assertEquals(1, result.size());
        assertEquals(TEST_ACCESS_KEY, result.get(0).getAccessKey());
        assertEquals("User Key", result.get(0).getName());

        verify(apiKeyRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should update API key status and invalidate cache")
    void testUpdateKeyStatus() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .status(ApiKeyStatus.ENABLED)
                .build();

        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        apiKeyService.updateKeyStatus(1L, ApiKeyStatus.DISABLED);

        assertEquals(ApiKeyStatus.DISABLED, apiKey.getStatus());
        verify(apiKeyRepository).save(apiKey);
        verify(redisTemplate).delete(contains(TEST_ACCESS_KEY));
    }

    @Test
    @DisplayName("Should delete API key and invalidate cache")
    void testDeleteApiKey() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .build();

        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(apiKey));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        apiKeyService.deleteApiKey(1L);

        verify(apiKeyRepository).delete(apiKey);
        verify(redisTemplate).delete(contains(TEST_ACCESS_KEY));
    }

    @Test
    @DisplayName("Should throw exception when deleting other user's API key")
    void testDeleteApiKeyForUserNotOwner() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .userId(2L) // Different user
                .build();

        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(apiKey));

        assertThrows(RuntimeException.class, () -> {
            apiKeyService.deleteApiKeyForUser(1L, 1L); // User 1 trying to delete User 2's key
        });

        verify(apiKeyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should validate API key with correct signature")
    void testValidateApiKeySuccess() {
        String timestamp = "2026-04-05T10:00:00Z";
        String stringToSign = "POST\n/api/edge/register\n" + timestamp;

        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .encryptedSecret(ENCRYPTED_SECRET)
                .status(ApiKeyStatus.ENABLED)
                .type(ApiKeyType.USER)
                .build();

        when(signatureUtil.isTimestampValid(timestamp)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findByAccessKey(TEST_ACCESS_KEY)).thenReturn(Optional.of(apiKey));
        when(encryptionUtil.decrypt(ENCRYPTED_SECRET)).thenReturn(TEST_SECRET_KEY);
        when(signatureUtil.buildStringToSign("POST", "/api/edge/register", timestamp)).thenReturn(stringToSign);
        when(signatureUtil.verifySignature(eq(stringToSign), eq("validSig"), eq(TEST_SECRET_KEY))).thenReturn(true);
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        boolean result = apiKeyService.validateApiKey(TEST_ACCESS_KEY, "validSig", timestamp, "POST", "/api/edge/register");

        assertTrue(result);
        verify(apiKeyRepository).save(apiKey); // Verify last used timestamp update
    }

    @Test
    @DisplayName("Should reject API key with invalid timestamp")
    void testValidateApiKeyInvalidTimestamp() {
        when(signatureUtil.isTimestampValid(anyString())).thenReturn(false);

        boolean result = apiKeyService.validateApiKey(
                TEST_ACCESS_KEY, "validSig", "invalid-ts", "POST", "/api/edge/register");

        assertFalse(result);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Should reject API key with invalid signature")
    void testValidateApiKeyInvalidSignature() {
        String timestamp = "2026-04-05T10:00:00Z";
        String stringToSign = "POST\n/api/edge/register\n" + timestamp;

        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .accessKey(TEST_ACCESS_KEY)
                .encryptedSecret(ENCRYPTED_SECRET)
                .status(ApiKeyStatus.ENABLED)
                .type(ApiKeyType.USER)
                .build();

        when(signatureUtil.isTimestampValid(timestamp)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findByAccessKey(TEST_ACCESS_KEY)).thenReturn(Optional.of(apiKey));
        when(encryptionUtil.decrypt(ENCRYPTED_SECRET)).thenReturn(TEST_SECRET_KEY);
        when(signatureUtil.buildStringToSign("POST", "/api/edge/register", timestamp)).thenReturn(stringToSign);
        when(signatureUtil.verifySignature(eq(stringToSign), eq("invalidSig"), eq(TEST_SECRET_KEY))).thenReturn(false);

        boolean result = apiKeyService.validateApiKey(TEST_ACCESS_KEY, "invalidSig", timestamp, "POST", "/api/edge/register");

        assertFalse(result);
    }
}
