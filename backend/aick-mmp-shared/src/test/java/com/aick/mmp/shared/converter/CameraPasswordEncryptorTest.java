package com.aick.mmp.shared.converter;

import com.aick.mmp.shared.util.AESEncryptionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CameraPasswordEncryptor JPA AttributeConverter.
 */
class CameraPasswordEncryptorTest {

    private static CameraPasswordEncryptor encryptor;

    @BeforeAll
    static void setup() throws Exception {
        AESEncryptionUtil util = new AESEncryptionUtil(
                "test-encryption-key-32bytes!!!",
                "test-camera-key-32bytes!!!!"
        );
        encryptor = new CameraPasswordEncryptor();
        // Use reflection to set the private static field
        java.lang.reflect.Field field = CameraPasswordEncryptor.class.getDeclaredField("encryptionUtil");
        field.setAccessible(true);
        field.set(null, util);
    }

    @Test
    @DisplayName("Should encrypt then decrypt back to original password")
    void testEncryptThenDecrypt() {
        String password = "MyCameraP@ssw0rd!";

        String encrypted = encryptor.convertToDatabaseColumn(password);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        assertNotEquals(password, encrypted, "Encrypted value should differ from plaintext");
        assertTrue(encrypted.length() >= 28, "Encrypted Base64 should be at least 28 characters");
        assertEquals(password, decrypted, "Decrypted value should match original password");
    }

    @Test
    @DisplayName("Should return null for null input in both directions")
    void testNullHandling() {
        assertNull(encryptor.convertToDatabaseColumn(null));
        assertNull(encryptor.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("Should return empty string for empty input in both directions")
    void testEmptyString() {
        assertEquals("", encryptor.convertToDatabaseColumn(""));
        assertEquals("", encryptor.convertToEntityAttribute(""));
    }

    @Test
    @DisplayName("Should not re-encrypt an already encrypted value (idempotent)")
    void testIdempotentEncryption() {
        String password = "SomePassword123";

        String firstEncryption = encryptor.convertToDatabaseColumn(password);
        String secondEncryption = encryptor.convertToDatabaseColumn(firstEncryption);

        assertEquals(firstEncryption, secondEncryption,
                "Encrypting an already-encrypted value should return it as-is");
    }

    @Test
    @DisplayName("Should pass through short plaintext values unchanged when reading")
    void testPlaintextPassthrough() {
        String shortValue = "short";

        String result = encryptor.convertToEntityAttribute(shortValue);

        assertEquals(shortValue, result,
                "Short non-encrypted value should pass through unchanged");
    }

    @Test
    @DisplayName("Should handle various password complexities")
    void testVariousPasswords() {
        String[] passwords = {
                "a",
                "123456",
                "P@ssw0rd!",
                "a very long password with spaces and special chars !@#$%^&*()",
                "你好世界",
                ""
        };

        for (String password : passwords) {
            String encrypted = encryptor.convertToDatabaseColumn(password);
            String decrypted = encryptor.convertToEntityAttribute(encrypted);

            if (password.isEmpty()) {
                assertEquals("", encrypted);
                assertEquals("", decrypted);
            } else {
                assertNotEquals(password, encrypted, "Encrypted value should differ from plaintext for: " + password);
                assertEquals(password, decrypted, "Decrypted value should match original for: " + password);
            }
        }
    }

    @Test
    @DisplayName("Should handle non-Base64 input gracefully in convertToEntityAttribute")
    void testInvalidBase64Input() {
        String invalidValue = "This is not base64!!! and it is longer than 28 characters...";

        String result = encryptor.convertToEntityAttribute(invalidValue);

        // Should pass through as-is since it's not valid encrypted data
        assertEquals(invalidValue, result);
    }

    @Test
    @DisplayName("Should produce different ciphertext each time for same plaintext (random IV)")
    void testNonDeterministicEncryption() {
        String password = "SamePassword";

        String encrypted1 = encryptor.convertToDatabaseColumn(password);
        String encrypted2 = encryptor.convertToDatabaseColumn(password);

        assertNotEquals(encrypted1, encrypted2,
                "Same password should produce different ciphertext each time due to random IV");

        assertEquals(password, encryptor.convertToEntityAttribute(encrypted1));
        assertEquals(password, encryptor.convertToEntityAttribute(encrypted2));
    }
}
