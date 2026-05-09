package com.aick.mmp.edge.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Utility for computing API request signatures on the Edge node side.
 * Uses HMAC-SHA256 algorithm with simplified signature format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeSignatureUtil {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
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
     * Get current timestamp in ISO-8601 format.
     *
     * @return current timestamp string
     */
    public String getCurrentTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }
    
    /**
     * Sign a request with the given secret key.
     *
     * @param method HTTP method
     * @param path Request path
     * @param secretKey Secret key
     * @return Signature string
     */
    public String signRequest(String method, String path, String secretKey) {
        String timestamp = getCurrentTimestamp();
        String stringToSign = buildStringToSign(method, path, timestamp);
        return computeSignature(stringToSign, secretKey);
    }

    /**
     * Sign a request with the given secret key and timestamp.
     * This method ensures the timestamp used for signing matches the X-Timestamp header.
     *
     * @param method HTTP method
     * @param path Request path
     * @param secretKey Secret key
     * @param timestamp ISO-8601 timestamp to use for signing
     * @return Signature string
     */
    public String signRequest(String method, String path, String secretKey, String timestamp) {
        String stringToSign = buildStringToSign(method, path, timestamp);
        return computeSignature(stringToSign, secretKey);
    }
}
