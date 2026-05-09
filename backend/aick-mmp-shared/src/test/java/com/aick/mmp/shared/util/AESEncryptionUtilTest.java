package com.aick.mmp.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AESEncryptionUtil
 */
class AESEncryptionUtilTest {

    private AESEncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        // Use a 32-byte key for AES-256
        encryptionUtil = new AESEncryptionUtil("0123456789abcdef0123456789abcdef");
    }

    @Test
    @DisplayName("Should encrypt and decrypt plain text correctly")
    void testEncryptAndDecrypt() {
        String originalText = "Hello, this is a secret message!";

        String encrypted = encryptionUtil.encrypt(originalText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Should produce different ciphertext for same plaintext (due to random IV)")
    void testDifferentCiphertextForSamePlaintext() {
        String originalText = "Same message";

        String encrypted1 = encryptionUtil.encrypt(originalText);
        String encrypted2 = encryptionUtil.encrypt(originalText);

        // Different IVs should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);

        // But both should decrypt to the same plaintext
        assertEquals(originalText, encryptionUtil.decrypt(encrypted1));
        assertEquals(originalText, encryptionUtil.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Should handle empty string")
    void testEncryptEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.encrypt("");
        });
    }

    @Test
    @DisplayName("Should handle null input")
    void testEncryptNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.encrypt(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            encryptionUtil.decrypt(null);
        });
    }

    @Test
    @DisplayName("Should handle Chinese characters")
    void testChineseCharacters() {
        String chineseText = "你好，世界！这是一个测试。";

        String encrypted = encryptionUtil.encrypt(chineseText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(chineseText, decrypted);
    }

    @Test
    @DisplayName("Should handle special characters")
    void testSpecialCharacters() {
        String specialText = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        String encrypted = encryptionUtil.encrypt(specialText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(specialText, decrypted);
    }

    @Test
    @DisplayName("Should handle long text")
    void testLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Long text block ").append(i).append(". ");
        }
        String longText = sb.toString();

        String encrypted = encryptionUtil.encrypt(longText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(longText, decrypted);
    }

    @Test
    @DisplayName("Should handle JSON-like text")
    void testJsonText() {
        String jsonText = "{\"accessKey\":\"ak_test123\",\"secretKey\":\"sk_secret456\"}";

        String encrypted = encryptionUtil.encrypt(jsonText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(jsonText, decrypted);
    }

    @Test
    @DisplayName("Should handle short key (padding)")
    void testShortKey() {
        AESEncryptionUtil shortKeyUtil = new AESEncryptionUtil("shortkey");
        String originalText = "Test message";

        String encrypted = shortKeyUtil.encrypt(originalText);
        String decrypted = shortKeyUtil.decrypt(encrypted);

        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Should reject invalid ciphertext")
    void testInvalidCiphertext() {
        // Use valid Base64 but wrong content (wrong IV/correct format)
        // This will fail decryption but won't trigger Base64 decode error
        assertThrows(RuntimeException.class, () -> {
            encryptionUtil.decrypt("dGVzdC10ZXN0LXRlc3QtdGVzdA=="); // "test-test-test-test" in Base64
        });
    }

    @Test
    @DisplayName("Should handle whitespace-only text")
    void testWhitespaceText() {
        String whitespaceText = "   \t\n\r   ";

        String encrypted = encryptionUtil.encrypt(whitespaceText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(whitespaceText, decrypted);
    }
}
