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

/**
 * AES-256-GCM encryption utility for securing sensitive data like API secret keys
 * and camera credentials.
 */
@Component
@Slf4j
public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String KEY_ALGORITHM = "AES";

    private final SecretKey secretKey;
    private final SecretKey cameraSecretKey;
    private final SecureRandom secureRandom;

    public AESEncryptionUtil(
            @Value("${security.encryption.secret-key:mmp-default-encryption-key-32bytes}") String encryptionKey,
            @Value("${security.encryption.camera-credential-key:mmp-default-camera-key-32bytes!}") String cameraEncryptionKey) {
        this.secretKey = new SecretKeySpec(ensureKeyLength(encryptionKey), KEY_ALGORITHM);
        this.cameraSecretKey = new SecretKeySpec(ensureKeyLength(cameraEncryptionKey), KEY_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt plaintext using AES-256-GCM with the main secret key.
     * The returned ciphertext includes the IV prepended.
     *
     * @param plaintext the text to encrypt
     * @return Base64 encoded ciphertext (IV + encrypted data)
     */
    public String encrypt(String plaintext) {
        return encryptWithKey(plaintext, secretKey);
    }

    /**
     * Decrypt ciphertext using AES-256-GCM with the main secret key.
     *
     * @param ciphertext Base64 encoded ciphertext (IV + encrypted data)
     * @return decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        return decryptWithKey(ciphertext, secretKey);
    }

    /**
     * Encrypt a camera password using AES-256-GCM with the camera credential key.
     * Returns null if the input is null, and empty string if the input is empty.
     *
     * @param plaintext the camera password to encrypt
     * @return Base64 encoded ciphertext, or null/empty for null/empty input
     */
    public String encryptCameraPassword(String plaintext) {
        if (plaintext == null) return null;
        if (plaintext.isEmpty()) return "";
        return encryptWithKey(plaintext, cameraSecretKey);
    }

    /**
     * Decrypt a camera password using AES-256-GCM with the camera credential key.
     * Returns null if the input is null, and empty string if the input is empty.
     *
     * @param ciphertext Base64 encoded ciphertext (IV + encrypted data)
     * @return decrypted plaintext, or null/empty for null/empty input
     */
    public String decryptCameraPassword(String ciphertext) {
        if (ciphertext == null) return null;
        if (ciphertext.isEmpty()) return "";
        return decryptWithKey(ciphertext, cameraSecretKey);
    }

    /**
     * Encrypt plaintext using AES-256-GCM with the given key.
     * The returned ciphertext includes the IV prepended.
     *
     * @param plaintext the text to encrypt
     * @param key       the AES secret key to use
     * @return Base64 encoded ciphertext (IV + encrypted data)
     */
    private String encryptWithKey(String plaintext, SecretKey key) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Return Base64 encoded
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt ciphertext using AES-256-GCM with the given key.
     *
     * @param ciphertext Base64 encoded ciphertext (IV + encrypted data)
     * @param key        the AES secret key to use
     * @return decrypted plaintext
     */
    private String decryptWithKey(String ciphertext, SecretKey key) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }

        try {
            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // Extract ciphertext
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encrypted);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Ensure the key is exactly 32 bytes for AES-256.
     * If shorter, pad with zeros. If longer, truncate.
     */
    private byte[] ensureKeyLength(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32]; // 256 bits

        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, result, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
        }

        return result;
    }
}
