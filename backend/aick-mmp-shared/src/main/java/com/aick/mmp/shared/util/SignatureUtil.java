package com.aick.mmp.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Utility for computing and verifying API request signatures.
 * Uses HMAC-SHA256 algorithm with simplified signature format.
 */
@Component
@Slf4j
public class SignatureUtil {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Timestamp tolerance in seconds (default: 5 minutes)
     */
    @Value("${security.signature.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;
    
    /**
     * Build the string to sign for signature computation.
     * Format: HTTP_METHOD + "\n" + REQUEST_PATH + "\n" + TIMESTAMP
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path Request path (e.g., /api/edge/register)
     * @param timestamp ISO-8601 timestamp
     * @return string to sign
     */
    public String buildStringToSign(String method, String path, String timestamp) {
        return method.toUpperCase() + "\n" + path + "\n" + timestamp;
    }
    
    /**
     * Compute the signature for the given string using HMAC-SHA256.
     *
     * @param stringToSign the string to sign
     * @param secretKey the secret key
     * @return Base64 encoded signature
     */
    public String computeSignature(String stringToSign, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            
            byte[] hmacBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute signature", e);
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
    
    /**
     * Verify that the provided signature matches the expected signature.
     *
     * @param stringToSign the string that was signed
     * @param providedSignature the signature provided in the request
     * @param secretKey the secret key used for signing
     * @return true if signatures match
     */
    public boolean verifySignature(String stringToSign, String providedSignature, String secretKey) {
        if (stringToSign == null || providedSignature == null || secretKey == null) {
            return false;
        }
        
        String expectedSignature = computeSignature(stringToSign, secretKey);
        return constantTimeEquals(expectedSignature, providedSignature);
    }
    
    /**
     * Check if the timestamp is within the allowed tolerance.
     *
     * @param timestamp ISO-8601 timestamp to check
     * @return true if timestamp is valid (within tolerance)
     */
    public boolean isTimestampValid(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return false;
        }
        
        try {
            Instant requestTime = Instant.from(TIMESTAMP_FORMATTER.parse(timestamp));
            Instant now = Instant.now();
            
            long diffSeconds = Math.abs(now.getEpochSecond() - requestTime.getEpochSecond());
            return diffSeconds <= timestampToleranceSeconds;
            
        } catch (Exception e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }
    
    /**
     * Get current timestamp in ISO-8601 format.
     *
     * @return current timestamp string
     */
    public String getCurrentTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
    
    /**
     * Get an expired timestamp (6 minutes ago) for testing.
     *
     * @return expired timestamp string
     */
    public String getExpiredTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now().minusSeconds(360)); // 6 minutes ago
    }
}
