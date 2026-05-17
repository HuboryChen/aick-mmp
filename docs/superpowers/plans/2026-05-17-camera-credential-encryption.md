# Camera Credential Encryption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Encrypt camera RTSP/ONVIF passwords at rest using AES-256-GCM with transparent JPA AttributeConverter, Redis cache, and minimal code changes.

**Architecture:** AESEncryptionUtil gets a second key (`camera-credential-key`) + public encrypt/decrypt methods. A JPA AttributeConverter on `Camera.password` auto-encrypts on write and decrypts on read. Management APIs mask the password field; edge node API returns decrypted plaintext. Redis caches decrypted values (1h TTL) with invalidation on credential updates.

**Tech Stack:** Java 21, Spring Boot 3.2, JPA/Hibernate 6, AES-256-GCM, Redis (Lettuce), Flyway

---

### Task 1: Add camera credential key and methods to AESEncryptionUtil

**Files:**
- Modify: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/util/AESEncryptionUtil.java`
- Test: `backend/aick-mmp-shared/src/test/java/com/aick/mmp/shared/util/AESEncryptionUtilTest.java`

- [ ] **Step 1: Add second `@Value` parameter and new methods to AESEncryptionUtil**

Current constructor takes one `@Value` for `secret-key`. Add a second `@Value` for `camera-credential-key` with a safe default for development, and add `encryptCameraPassword()`/`decryptCameraPassword()` methods.

```java
package com.aick.mmp.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String KEY_ALGORITHM = "AES";

    private final SecretKey secretKey;
    private final SecretKey cameraSecretKey;
    private final SecureRandom secureRandom;

    public AESEncryptionUtil(
            @Value("${security.encryption.secret-key:mmp-default-encryption-key-32bytes}") String encryptionKey,
            @Value("${security.encryption.camera-credential-key:mmp-default-camera-key-32bytes!}") String cameraKey) {
        this.secretKey = new SecretKeySpec(ensureKeyLength(encryptionKey), KEY_ALGORITHM);
        this.cameraSecretKey = new SecretKeySpec(ensureKeyLength(cameraKey), KEY_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plaintext) {
        return encryptWithKey(plaintext, secretKey);
    }

    public String decrypt(String ciphertext) {
        return decryptWithKey(ciphertext, secretKey);
    }

    public String encryptCameraPassword(String plaintext) {
        if (plaintext == null) return null;
        if (plaintext.isEmpty()) return "";
        return encryptWithKey(plaintext, cameraSecretKey);
    }

    public String decryptCameraPassword(String ciphertext) {
        if (ciphertext == null) return null;
        if (ciphertext.isEmpty()) return "";
        return decryptWithKey(ciphertext, cameraSecretKey);
    }

    private String encryptWithKey(String plaintext, SecretKey key) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decryptWithKey(String ciphertext, SecretKey key) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private byte[] ensureKeyLength(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32];
        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, result, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
        }
        return result;
    }
}
```

- [ ] **Step 2: Add unit tests for camera password methods**

```java
// Add these test methods to AESEncryptionUtilTest.java

@Test
void testEncryptDecryptCameraPassword() {
    AESEncryptionUtil util = new AESEncryptionUtil(
        "test-encryption-key-32bytes!!!",
        "test-camera-key-32bytes!!!!"
    );
    String plaintext = "camera123!";
    String encrypted = util.encryptCameraPassword(plaintext);
    assertNotNull(encrypted);
    assertNotEquals(plaintext, encrypted);
    String decrypted = util.decryptCameraPassword(encrypted);
    assertEquals(plaintext, decrypted);
}

@Test
void testCameraPasswordDeterministicDifferentCiphertext() {
    AESEncryptionUtil util = new AESEncryptionUtil(
        "test-encryption-key-32bytes!!!",
        "test-camera-key-32bytes!!!!"
    );
    String plaintext = "admin123";
    String encrypted1 = util.encryptCameraPassword(plaintext);
    String encrypted2 = util.encryptCameraPassword(plaintext);
    // Same plaintext should produce different ciphertext (random IV)
    assertNotEquals(encrypted1, encrypted2);
    assertEquals(plaintext, util.decryptCameraPassword(encrypted1));
    assertEquals(plaintext, util.decryptCameraPassword(encrypted2));
}

@Test
void testCameraPasswordNullAndEmpty() {
    AESEncryptionUtil util = new AESEncryptionUtil(
        "test-encryption-key-32bytes!!!",
        "test-camera-key-32bytes!!!!"
    );
    assertNull(util.encryptCameraPassword(null));
    assertEquals("", util.encryptCameraPassword(""));
    assertNull(util.decryptCameraPassword(null));
    assertEquals("", util.decryptCameraPassword(""));
}

@Test
void testCameraEncryptionKeyIsolatedFromMainKey() {
    AESEncryptionUtil util = new AESEncryptionUtil(
        "main-key-32bytes-don't-use-this!!!",
        "camera-key-32bytes-separate-isolate"
    );
    String cameraPwd = "rtspPass!";
    String encrypted = util.encryptCameraPassword(cameraPwd);
    // Decrypting with main key should fail
    assertThrows(RuntimeException.class, () -> util.decrypt(encrypted));
    // Decrypting camera password with main key via wrong method
    assertThrows(RuntimeException.class, () -> util.decryptCameraPassword(util.encrypt(cameraPwd)));
}
```

- [ ] **Step 3: Run tests to verify they pass**

Run: `cd backend && mvn test -pl aick-mmp-shared -Dtest=AESEncryptionUtilTest -DfailIfNoTests=false`
Expected: All 4 new tests + existing tests pass

- [ ] **Step 4: Commit**

```bash
git add backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/util/AESEncryptionUtil.java
git add backend/aick-mmp-shared/src/test/java/com/aick/mmp/shared/util/AESEncryptionUtilTest.java
git commit -m "feat: add camera credential key and methods to AESEncryptionUtil"
```

---

### Task 2: Create CameraPasswordEncryptor JPA AttributeConverter

**Files:**
- Create: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/converter/CameraPasswordEncryptor.java`
- Test: `backend/aick-mmp-shared/src/test/java/com/aick/mmp/shared/converter/CameraPasswordEncryptorTest.java`

- [ ] **Step 1: Create the AttributeConverter**

```java
package com.aick.mmp.shared.converter;

import com.aick.mmp.shared.util.AESEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that automatically encrypts camera passwords
 * when writing to the database and decrypts when reading.
 * 
 * Leverages AESEncryptionUtil with the dedicated camera-credential-key.
 * Apply via @Convert(CameraPasswordEncryptor.class) on the password field.
 */
@Converter
@Component
@Slf4j
public class CameraPasswordEncryptor implements AttributeConverter<String, String> {

    @Setter
    private static AESEncryptionUtil encryptionUtil;

    @Autowired
    public void initEncryptionUtil(AESEncryptionUtil util) {
        CameraPasswordEncryptor.setEncryptionUtil(util);
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        // Skip re-encryption if already encrypted (for migration idempotency)
        if (isAlreadyEncrypted(plaintext)) {
            return plaintext;
        }
        return encryptionUtil.encryptCameraPassword(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        // If not encrypted (plaintext, e.g., during migration), return as-is
        if (!isAlreadyEncrypted(ciphertext)) {
            return ciphertext;
        }
        return encryptionUtil.decryptCameraPassword(ciphertext);
    }

    /**
     * Detect if a value is already AES-256-GCM encrypted.
     * Encrypted output is Base64 of [12-byte IV + ciphertext], so minimum length
     * is 16 characters (Base64 of 12 bytes = 16 chars) plus additional ciphertext.
     * Plaintext passwords are typically shorter and won't match this pattern reliably,
     * so we check: can it be Base64 decoded AND does it start with a valid IV length.
     */
    private boolean isAlreadyEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        // AES-256-GCM encrypted + Base64: minimum 28 chars (12 IV + 16 min ciphertext)
        // Most plaintext passwords are under 50 chars, but encrypted values are always 28+
        if (value.length() < 28) {
            return false;
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(value);
            // Decoded must have at least IV (12) + GCM tag (16) = 28 bytes
            return decoded.length >= 28;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Write and run unit test**

```java
package com.aick.mmp.shared.converter;

import com.aick.mmp.shared.util.AESEncryptionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraPasswordEncryptorTest {

    private static CameraPasswordEncryptor encryptor;

    @BeforeAll
    static void setup() {
        AESEncryptionUtil util = new AESEncryptionUtil(
            "test-encryption-key-32bytes!!!",
            "test-camera-key-32bytes!!!!"
        );
        encryptor = new CameraPasswordEncryptor();
        encryptor.setEncryptionUtil(util);
        CameraPasswordEncryptor.setEncryptionUtil(util);
    }

    @Test
    void testEncryptThenDecrypt() {
        String plaintext = "admin123!";
        String encrypted = encryptor.convertToDatabaseColumn(plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertTrue(encrypted.length() >= 28); // Base64(12 IV + min ciphertext)

        String decrypted = encryptor.convertToEntityAttribute(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testNullHandling() {
        assertNull(encryptor.convertToDatabaseColumn(null));
        assertNull(encryptor.convertToEntityAttribute(null));
    }

    @Test
    void testEmptyString() {
        String encrypted = encryptor.convertToDatabaseColumn("");
        assertNotNull(encrypted); // encryptCameraPassword returns "" for empty
        String decrypted = encryptor.convertToEntityAttribute(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    void testIdempotentEncryption() {
        // Second convertToDatabaseColumn call on already encrypted value
        // should return as-is (detected by isAlreadyEncrypted)
        String ciphertext = encryptor.convertToDatabaseColumn("mypassword");
        String doubleEncrypted = encryptor.convertToDatabaseColumn(ciphertext);
        assertEquals(ciphertext, doubleEncrypted);
    }

    @Test
    void testPlaintextPassthrough() {
        // A short string (< 28 chars) should be treated as plaintext
        String decrypted = encryptor.convertToEntityAttribute("short");
        assertEquals("short", decrypted);
    }
}
```

Run: `cd backend && mvn test -pl aick-mmp-shared -Dtest=CameraPasswordEncryptorTest -DfailIfNoTests=false`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/converter/CameraPasswordEncryptor.java
git add backend/aick-mmp-shared/src/test/java/com/aick/mmp/shared/converter/CameraPasswordEncryptorTest.java
git commit -m "feat: add CameraPasswordEncryptor JPA AttributeConverter"
```

---

### Task 3: Annotate Camera entity password field

**Files:**
- Modify: `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Camera.java`

- [ ] **Step 1: Add @Convert annotation to password field**

Change the `password` field in `Camera.java`:

```java
// Replace:
@Column(name = "password")
private String password;

// With:
@Column(name = "password", length = 512)
@Convert(converter = CameraPasswordEncryptor.class)
private String password;
```

Length increased to 512 because AES-256-GCM encrypted + Base64 encoded passwords are longer than plaintext.

- [ ] **Step 2: Verify the build compiles**

Run: `cd backend && mvn compile -pl aick-mmp-shared -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Camera.java
git commit -m "feat: add @Convert(CameraPasswordEncryptor.class) to Camera.password"
```

---

### Task 4: Configure camera-credential-key in all environments

**Files:**
- Modify: `backend/aick-mmp-central/src/main/resources/application.yml`
- Modify: `backend/aick-mmp-edge/src/main/resources/application.yml`
- Create: `backend/aick-mmp-central/src/main/resources/application-dev.example.yml` (or modify if exists)
- Modify: `backend/aick-mmp-central/src/test/resources/application-test.yml`

- [ ] **Step 1: Add camera-credential-key to central application.yml**

After the existing `security.encryption.secret-key` line:

```yaml
security:
  encryption:
    secret-key: ${ENCRYPTION_SECRET_KEY:mmp-encryption-key-change-in-production}
    camera-credential-key: ${CAMERA_CREDENTIAL_KEY:mmp-camera-encryption-key!!}
```

- [ ] **Step 2: Add camera-credential-key to edge application.yml**

After the existing `security.encryption.secret-key` line:

```yaml
security:
  encryption:
    secret-key: ${ENCRYPTION_SECRET_KEY:mmp-encryption-key-change-in-prod}
    camera-credential-key: ${CAMERA_CREDENTIAL_KEY:mmp-camera-encryption-key!!}
```

- [ ] **Step 3: Add camera-credential-key to test configuration**

In `application-test.yml`:

```yaml
security:
  encryption:
    key: test-encryption-key-32bytes!!!
    camera-credential-key: test-camera-key-32bytes!!!!
```

- [ ] **Step 4: Commit**

```bash
git add backend/aick-mmp-central/src/main/resources/application.yml
git add backend/aick-mmp-edge/src/main/resources/application.yml
git add backend/aick-mmp-central/src/test/resources/application-test.yml
git commit -m "chore: configure camera-credential-key in all environments"
```

---

### Task 5: Mask password in management API responses

Currently `convertToDto()` (line 397) uses `modelMapper.map(camera, CameraDTO.class)` which copies all fields including the decrypted password. Management APIs (`getAllCameras`, `getCameraById`, etc.) should return `******` instead.

Edge node API (`getCamerasByEdgeNode`) must return the real password (decrypted automatically by the converter).

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java`
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/dto/CameraDTO.java`

- [ ] **Step 1: Add a transient flag to distinguish management vs edge-node calls**

The simplest approach: add a method `convertToDtoWithMask()` in the service implementation that masks password after conversion.

In `CameraServiceImpl.java`, add:

```java
@Override
public CameraDTO convertToDto(Camera camera) {
    CameraDTO dto = modelMapper.map(camera, CameraDTO.class);

    // Calculate uptime percentage
    if (camera.getCreatedAt() != null && camera.getLastActiveTime() != null) {
        double uptimePercentage = calculateUptimePercentage(camera);
        dto.setUptimePercentage(uptimePercentage);
    }

    // Set region name
    if (camera.getRegionId() != null) {
        regionRepository.findById(camera.getRegionId())
            .ifPresent(region -> dto.setRegionName(region.getName()));
    }
    return dto;
}

/**
 * Convert to DTO with password masked for management API responses.
 */
private CameraDTO convertToDtoMasked(Camera camera) {
    CameraDTO dto = convertToDto(camera);
    dto.setPassword("******");
    return dto;
}
```

Now update all management API methods to use `convertToDtoMasked`:

- `getCameras()` (line 91-92): Change `.map(this::convertToDto)` to `.map(this::convertToDtoMasked)`
- `getCameraById()` (line 96-99): Change `return convertToDto(camera)` to `return convertToDtoMasked(camera)`
- `createCamera()` (line 127): Change `return convertToDto(savedCamera)` to `return convertToDtoMasked(savedCamera)`
- `updateCamera()` (line 173): Change `return convertToDto(savedCamera)` to `return convertToDtoMasked(savedCamera)`
- Keep `getCamerasByEdgeNode()` using `convertToDto` (returns real password)

```java
// Updated methods:

@Override
public Page<CameraDTO> getCameras(GetCamerasRequestDTO request) {
    // ... existing spec building code unchanged ...
    return cameraRepository.findAll(spec, request.getPageable())
            .map(this::convertToDtoMasked);
}

@Override
public CameraDTO getCameraById(Long id) {
    Camera camera = cameraRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
    return convertToDtoMasked(camera);
}

@Override
@Transactional
public CameraDTO createCamera(CameraDTO cameraDTO) {
    // ... existing create logic unchanged ...
    Camera savedCamera = cameraRepository.save(camera);
    return convertToDtoMasked(savedCamera);
}

@Override
@Transactional
public CameraDTO updateCamera(Long id, CameraDTO cameraDTO) {
    // ... existing update logic unchanged ...
    Camera savedCamera = cameraRepository.save(existingCamera);
    return convertToDtoMasked(savedCamera);
}

// getCamerasByEdgeNode stays on convertToDto (returns decrypted password):
@Override
public List<CameraDTO> getCamerasByEdgeNode(Long edgeNodeId) {
    log.info("Fetching cameras for edge node: {}", edgeNodeId);
    return cameraRepository.findByEdgeNodeId(edgeNodeId).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
}
```

Also update other methods that convert and return to management users:
- `getAllCameras()` (in CameraService interface)
- `getCamerasByStatus()`
- `getAllOnlineCameras()` / `getOnlineCamerasByEdgeNode()`
- `restoreCamera()`
- `getDeletedCameras()`

These should all use `convertToDtoMasked` since they serve management UI.

- [ ] **Step 2: Handle password in updateCamera for "don't change" semantics**

In `updateCamera()`, add logic to skip password update if the DTO sends `null` or `"******"`:

After line 151 (`existingCamera.setRegionId(cameraDTO.getRegionId());`), add:

```java
// Only update password if a new value is provided (not masked placeholder)
if (cameraDTO.getPassword() != null && !"******".equals(cameraDTO.getPassword())) {
    existingCamera.setPassword(cameraDTO.getPassword());
}
// Also update username if provided
if (cameraDTO.getUsername() != null) {
    existingCamera.setUsername(cameraDTO.getUsername());
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java
git commit -m "feat: mask camera password in management API responses"
```

---

### Task 6: Create Redis cache for decrypted camera passwords

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraCredentialCacheService.java`
- Create: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/CameraCredentialCacheServiceTest.java`

- [ ] **Step 1: Create the cache service**

```java
package com.aick.mmp.central.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis cache for decrypted camera passwords.
 * 
 * Decrypting camera passwords (AES-256-GCM) on every edge node API call
 * adds overhead. This cache stores decrypted results with a 1-hour TTL
 * and supports immediate invalidation when credentials are updated.
 * 
 * Cache key pattern: camera:pwd:decrypted:{cameraId}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CameraCredentialCacheService {

    private static final String CACHE_KEY_PREFIX = "camera:pwd:decrypted:";
    private static final long CACHE_TTL_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Get cached decrypted password for a camera.
     * @return cached password, or null if not cached
     */
    public String getDecryptedPassword(Long cameraId) {
        String cacheKey = buildKey(cameraId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for camera credential: {}", cameraId);
        }
        return cached;
    }

    /**
     * Cache a decrypted password.
     */
    public void cacheDecryptedPassword(Long cameraId, String plaintext) {
        String cacheKey = buildKey(cameraId);
        redisTemplate.opsForValue().set(cacheKey, plaintext, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Cached decrypted password for camera: {} (TTL: {}h)", cameraId, CACHE_TTL_HOURS);
    }

    /**
     * Invalidate cached password for a camera.
     * Called when camera credentials are updated or camera is deleted.
     */
    public void invalidateCamera(Long cameraId) {
        String cacheKey = buildKey(cameraId);
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated cached password for camera: {}", cameraId);
    }

    /**
     * Batch invalidate for multiple cameras.
     */
    public void invalidateCameras(java.util.List<Long> cameraIds) {
        if (cameraIds == null || cameraIds.isEmpty()) return;
        java.util.List<String> keys = cameraIds.stream()
                .map(this::buildKey)
                .toList();
        redisTemplate.delete(keys);
        log.debug("Invalidated cached passwords for {} cameras", cameraIds.size());
    }

    private String buildKey(Long cameraId) {
        return CACHE_KEY_PREFIX + cameraId;
    }
}
```

- [ ] **Step 2: Write and run unit test**

```java
package com.aick.mmp.central.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    void testCacheAndGet() {
        when(valueOperations.get("camera:pwd:decrypted:1")).thenReturn("admin123");

        String result = cacheService.getDecryptedPassword(1L);
        assertEquals("admin123", result);
    }

    @Test
    void testCacheMiss() {
        when(valueOperations.get("camera:pwd:decrypted:99")).thenReturn(null);

        String result = cacheService.getDecryptedPassword(99L);
        assertNull(result);
    }

    @Test
    void testCacheDecryptedPassword() {
        cacheService.cacheDecryptedPassword(1L, "supersecret");
        verify(valueOperations).set(
            eq("camera:pwd:decrypted:1"),
            eq("supersecret"),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    void testInvalidate() {
        cacheService.invalidateCamera(1L);
        verify(redisTemplate).delete("camera:pwd:decrypted:1");
    }

    @Test
    void testInvalidateMultiple() {
        cacheService.invalidateCameras(java.util.List.of(1L, 2L, 3L));
        verify(redisTemplate).delete(java.util.List.of(
            "camera:pwd:decrypted:1",
            "camera:pwd:decrypted:2",
            "camera:pwd:decrypted:3"
        ));
    }
}
```

Run: `cd backend && mvn test -pl aick-mmp-central -Dtest=CameraCredentialCacheServiceTest -DfailIfNoTests=false`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraCredentialCacheService.java
git add backend/aick-mmp-central/src/test/java/com/aick/mmp/central/service/CameraCredentialCacheServiceTest.java
git commit -m "feat: add Redis cache for decrypted camera passwords"
```

---

### Task 7: Integrate cache into edge node API with invalidation

**Files:**
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java`
- Modify: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraService.java`

- [ ] **Step 1: Inject cache service into CameraServiceImpl**

Add field and constructor parameter:

```java
// New imports at top:
import com.aick.mmp.central.service.CameraCredentialCacheService;

// Add field:
private final CameraCredentialCacheService credentialCacheService;

// Constructor already has @RequiredArgsConstructor, add final field
```

Since `CameraServiceImpl` uses `@RequiredArgsConstructor`, just add the final field:

```java
private final CameraCredentialCacheService credentialCacheService;
```

- [ ] **Step 2: Integrate cache into getCamerasByEdgeNode**

Modify `getCamerasByEdgeNode()` to check cache before JPA read:

```java
@Override
public List<CameraDTO> getCamerasByEdgeNode(Long edgeNodeId) {
    log.info("Fetching cameras for edge node: {}", edgeNodeId);
    List<Camera> cameras = cameraRepository.findByEdgeNodeId(edgeNodeId);
    return cameras.stream()
            .map(camera -> {
                CameraDTO dto = convertToDto(camera);
                // Try cache for password
                String cached = credentialCacheService.getDecryptedPassword(camera.getId());
                if (cached != null) {
                    dto.setPassword(cached);
                }
                // Cache the decrypted password for next time
                if (dto.getPassword() != null) {
                    credentialCacheService.cacheDecryptedPassword(camera.getId(), dto.getPassword());
                }
                return dto;
            })
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: Add cache invalidation on updateCamera**

In `updateCamera()`, after saving, invalidate cache if username or password changed:

After line `Camera savedCamera = cameraRepository.save(existingCamera);`:

```java
// Invalidate credential cache if username or password was updated
if (cameraDTO.getUsername() != null || 
    (cameraDTO.getPassword() != null && !"******".equals(cameraDTO.getPassword()))) {
    credentialCacheService.invalidateCamera(id);
}
```

- [ ] **Step 4: Add cache invalidation in updateCameraCredentials**

```java
@Override
public void updateCameraCredentials(Long id, String username, String password) {
    Camera camera = cameraRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + id));
    camera.setUsername(username);
    camera.setPassword(password);
    camera.setUpdatedAt(LocalDateTime.now());
    cameraRepository.save(camera);
    // Invalidate credential cache
    credentialCacheService.invalidateCamera(id);
}
```

- [ ] **Step 5: Add cache invalidation on deleteCamera**

In `deleteCamera()` after soft-delete:

```java
// Invalidate credential cache
credentialCacheService.invalidateCamera(id);
```

And in `batchDeleteCameras()`:

```java
@Override
public void batchDeleteCameras(List<Long> cameraIds) {
    log.info("Batch deleting {} cameras", cameraIds.size());
    for (Long id : cameraIds) {
        try {
            deleteCamera(id);
        } catch (Exception e) {
            log.error("Failed to delete camera {}: {}", id, e.getMessage());
        }
    }
    // Bulk invalidate cache
    credentialCacheService.invalidateCameras(cameraIds);
}
```

- [ ] **Step 6: Build and verify compilation**

Run: `cd backend && mvn compile -pl aick-mmp-central -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/CameraService.java
git commit -m "feat: integrate camera credential cache with invalidation"
```

---

### Task 8: Flyway Java-based migration for existing passwords

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/migration/V20260517__EncryptCameraPasswords.java`

- [ ] **Step 1: Create the Java-based Flyway migration**

```java
package com.aick.mmp.central.migration;

import com.aick.mmp.shared.util.AESEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;
import java.util.Map;

/**
 * Flyway Java-based migration to encrypt existing plaintext camera passwords.
 * 
 * Idempotent: detects already-encrypted passwords (Base64 decoded length >= 28 bytes)
 * and skips them. Safe to re-run.
 * 
 * Depends on AESEncryptionUtil with camera-credential-key being configured.
 * This migration runs BEFORE the application context is fully initialized,
 * so AESEncryptionUtil is instantiated directly here.
 */
@Slf4j
public class V20260517__EncryptCameraPasswords extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(
            new SingleConnectionDataSource(context.getConnection(), true)
        );

        // Read all cameras with potentially plaintext passwords
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, password FROM cameras WHERE password IS NOT NULL AND password != ''"
        );

        if (rows.isEmpty()) {
            log.info("No camera passwords to migrate");
            return;
        }

        // Initialize the encryption util with the default/dev key.
        // In production, Flyway runs during startup BEFORE Spring context is ready,
        // so we read the key from the environment variable directly.
        String cameraKey = System.getenv("CAMERA_CREDENTIAL_KEY");
        if (cameraKey == null || cameraKey.isEmpty()) {
            cameraKey = "mmp-camera-encryption-key!!"; // dev default
            log.warn("CAMERA_CREDENTIAL_KEY not set, using dev default. " +
                     "Set this env var in production before deploying this migration.");
        }

        AESEncryptionUtil encryptionUtil = new AESEncryptionUtil(
            "dummy-main-key-not-used-here", // main key not needed for camera ops
            cameraKey
        );

        int encryptedCount = 0;
        int skippedCount = 0;

        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String password = (String) row.get("password");

            // Skip if already encrypted (Base64 decode length >= 28 bytes)
            if (isAlreadyEncrypted(password)) {
                skippedCount++;
                continue;
            }

            // Encrypt the plaintext password
            String encrypted = encryptionUtil.encryptCameraPassword(password);
            jdbc.update(
                "UPDATE cameras SET password = ? WHERE id = ?",
                encrypted, id
            );
            encryptedCount++;
            log.debug("Encrypted password for camera: {}", id);
        }

        log.info("Camera password migration complete: {} encrypted, {} already encrypted, {} total",
                encryptedCount, skippedCount, rows.size());
    }

    private boolean isAlreadyEncrypted(String value) {
        if (value == null || value.length() < 28) return false;
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(value);
            return decoded.length >= 28;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `cd backend && mvn compile -pl aick-mmp-central -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/migration/V20260517__EncryptCameraPasswords.java
git commit -m "feat: add Flyway migration to encrypt existing camera passwords"
```

---

### Task 9: End-to-end verification

- [ ] **Step 1: Run all tests**

Run: `cd backend && mvn test -pl aick-mmp-shared,aick-mmp-central -DfailIfNoTests=false`
Expected: All tests pass (existing + new)

- [ ] **Step 2: Verify in dev environment**

Start infra and application:
```bash
docker-compose up -d mysql redis
cd backend && mvn spring-boot:run -pl aick-mmp-central
```

- [ ] **Step 3: Create a camera and verify encrypted storage**

```bash
# Create camera with password
curl -X POST http://localhost:8080/api/cameras \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name":"test-cam","protocol":"RTSP","connectionUrl":"rtsp://192.168.1.100:554/stream1","username":"admin","password":"test123!","regionId":1}'

# Verify password is masked in management API
curl http://localhost:8080/api/cameras/1 \
  -H "Authorization: Bearer <token>"
# Expected: password = "******"

# Verify password is decrypted for edge node API
curl http://localhost:8080/api/cameras/edge-node/1 \
  -H "X-Access-Key: <ak>" \
  -H "X-Signature: <sig>" \
  -H "X-Timestamp: <ts>"
# Expected: password = "test123!" (real value)
```

- [ ] **Step 4: Direct DB check**

```bash
docker exec -it aick-mmp-mysql mysql -uroot -proot aick_mmp \
  -e "SELECT id, username, password FROM cameras WHERE id = 1;"
# Expected: password column shows Base64 encoded ciphertext (not "test123!")
```

- [ ] **Step 5: Commit final**

```bash
git add -A && git commit -m "feat: complete camera credential encryption implementation"
```

---

## Spec Coverage Check

| Design Decision | Covered By |
|----------------|-----------|
| AES-256-GCM encrypted at rest | Task 1 (encryptCameraPassword) + Task 2 (Converter) |
| Independent camera-credential-key | Task 1 (separate SecretKey field) + Task 4 (config) |
| JPA AttributeConverter, business code unaffected | Task 2 (Converter) + Task 3 (Entity annotation) |
| Management API returns `******` | Task 5 (convertToDtoMasked) |
| Edge node API returns plaintext | Task 5 (getCamerasByEdgeNode unchanged) |
| Redis cache 1h TTL | Task 6 (CameraCredentialCacheService) |
| Cache invalidation on credential update | Task 7 (inject into update/delete) |
| Flyway migration for existing passwords | Task 8 (Java-based migration) |
| Password rotation without restart | Task 3+5+7 (Converter + cache invalidation) |
| Migration idempotent | Task 8 (isAlreadyEncrypted check) + Task 2 (Converter same check) |
