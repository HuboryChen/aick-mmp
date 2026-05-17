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
 * Uses AESEncryptionUtil with the dedicated camera-credential-key.
 * Apply via @Convert(CameraPasswordEncryptor.class) on the password field.
 */
@Converter
@Component
@Slf4j
public class CameraPasswordEncryptor implements AttributeConverter<String, String> {

    private static volatile AESEncryptionUtil encryptionUtil;

    @Autowired
    public void initEncryptionUtil(AESEncryptionUtil util) {
        if (util == null) {
            throw new IllegalArgumentException("AESEncryptionUtil must not be null");
        }
        CameraPasswordEncryptor.encryptionUtil = util;
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
     * Encrypted output is Base64 of [12-byte IV + ciphertext + tag], so minimum
     * decoded length is 28 bytes (12 IV + 16 min ciphertext/tag).
     */
    private boolean isAlreadyEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        // AES-256-GCM encrypted + Base64: minimum 28 chars for smallest values
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
