package com.aick.mmp.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignatureUtil
 */
class SignatureUtilTest {

    private SignatureUtil signatureUtil;
    private static final String SECRET_KEY = "test-secret-key-for-signature";

    @BeforeEach
    void setUp() {
        signatureUtil = new SignatureUtil();
        // Set timestamp tolerance to 5 minutes
        ReflectionTestUtils.setField(signatureUtil, "timestampToleranceSeconds", 300L);
    }

    @Test
    @DisplayName("Should build string to sign correctly")
    void testBuildStringToSign() {
        String method = "POST";
        String path = "/api/edge/register";
        String timestamp = "2026-04-05T10:00:00Z";

        String result = signatureUtil.buildStringToSign(method, path, timestamp);

        assertEquals("POST\n/api/edge/register\n2026-04-05T10:00:00Z", result);
    }

    @Test
    @DisplayName("Should convert method to uppercase")
    void testBuildStringToSignUppercase() {
        String result = signatureUtil.buildStringToSign("get", "/api/test", "2026-04-05T10:00:00Z");
        assertEquals("GET\n/api/test\n2026-04-05T10:00:00Z", result);
    }

    @Test
    @DisplayName("Should compute consistent signature for same input")
    void testComputeSignatureConsistency() {
        String stringToSign = "GET\n/api/test\n2026-04-05T10:00:00Z";

        String signature1 = signatureUtil.computeSignature(stringToSign, SECRET_KEY);
        String signature2 = signatureUtil.computeSignature(stringToSign, SECRET_KEY);

        assertEquals(signature1, signature2);
    }

    @Test
    @DisplayName("Should produce different signatures for different inputs")
    void testDifferentInputsDifferentSignatures() {
        String stringToSign1 = "GET\n/api/test1\n2026-04-05T10:00:00Z";
        String stringToSign2 = "GET\n/api/test2\n2026-04-05T10:00:00Z";

        String signature1 = signatureUtil.computeSignature(stringToSign1, SECRET_KEY);
        String signature2 = signatureUtil.computeSignature(stringToSign2, SECRET_KEY);

        assertNotEquals(signature1, signature2);
    }

    @Test
    @DisplayName("Should produce different signatures for different keys")
    void testDifferentKeysDifferentSignatures() {
        String stringToSign = "GET\n/api/test\n2026-04-05T10:00:00Z";

        String signature1 = signatureUtil.computeSignature(stringToSign, "key1");
        String signature2 = signatureUtil.computeSignature(stringToSign, "key2");

        assertNotEquals(signature1, signature2);
    }

    @Test
    @DisplayName("Should verify valid signature")
    void testVerifyValidSignature() {
        String stringToSign = "POST\n/api/edge/register\n2026-04-05T10:00:00Z";
        String signature = signatureUtil.computeSignature(stringToSign, SECRET_KEY);

        boolean result = signatureUtil.verifySignature(stringToSign, signature, SECRET_KEY);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void testVerifyInvalidSignature() {
        String stringToSign = "POST\n/api/edge/register\n2026-04-05T10:00:00Z";

        boolean result = signatureUtil.verifySignature(stringToSign, "invalid-signature", SECRET_KEY);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject tampered string to sign")
    void testVerifyTamperedString() {
        String originalStringToSign = "POST\n/api/edge/register\n2026-04-05T10:00:00Z";
        String signature = signatureUtil.computeSignature(originalStringToSign, SECRET_KEY);

        String tamperedStringToSign = "POST\n/api/edge/config\n2026-04-05T10:00:00Z";

        boolean result = signatureUtil.verifySignature(tamperedStringToSign, signature, SECRET_KEY);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle null parameters in verify")
    void testVerifyNullParameters() {
        assertFalse(signatureUtil.verifySignature(null, "sig", "key"));
        assertFalse(signatureUtil.verifySignature("str", null, "key"));
        assertFalse(signatureUtil.verifySignature("str", "sig", null));
    }

    @Test
    @DisplayName("Should validate current timestamp as valid")
    void testIsTimestampValidCurrentTime() {
        String currentTimestamp = signatureUtil.getCurrentTimestamp();

        boolean result = signatureUtil.isTimestampValid(currentTimestamp);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate timestamp within tolerance")
    void testIsTimestampValidWithinTolerance() {
        // 2 minutes ago should be valid
        String twoMinutesAgo = Instant.now().minusSeconds(120).toString();

        boolean result = signatureUtil.isTimestampValid(twoMinutesAgo);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject timestamp outside tolerance")
    void testIsTimestampInvalidOutsideTolerance() {
        // 10 minutes ago should be invalid (tolerance is 5 minutes)
        String tenMinutesAgo = Instant.now().minusSeconds(600).toString();

        boolean result = signatureUtil.isTimestampValid(tenMinutesAgo);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject future timestamp outside tolerance")
    void testIsTimestampInvalidFutureOutsideTolerance() {
        // 10 minutes in the future should be invalid
        String tenMinutesFuture = Instant.now().plusSeconds(600).toString();

        boolean result = signatureUtil.isTimestampValid(tenMinutesFuture);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject null timestamp")
    void testIsTimestampNull() {
        assertFalse(signatureUtil.isTimestampValid(null));
    }

    @Test
    @DisplayName("Should reject empty timestamp")
    void testIsTimestampEmpty() {
        assertFalse(signatureUtil.isTimestampValid(""));
    }

    @Test
    @DisplayName("Should reject invalid timestamp format")
    void testIsTimestampInvalidFormat() {
        assertFalse(signatureUtil.isTimestampValid("not-a-timestamp"));
        assertFalse(signatureUtil.isTimestampValid("2026-04-05 10:00:00"));
    }

    @Test
    @DisplayName("Should generate current timestamp in correct format")
    void testGetCurrentTimestamp() {
        String timestamp = signatureUtil.getCurrentTimestamp();

        assertNotNull(timestamp);
        assertTrue(timestamp.contains("T"));
        assertTrue(timestamp.endsWith("Z"));
        assertTrue(signatureUtil.isTimestampValid(timestamp));
    }

    @Test
    @DisplayName("Should handle path with query parameters")
    void testBuildStringToSignWithQueryParams() {
        String result = signatureUtil.buildStringToSign("GET", "/api/cameras?page=1&size=10", "2026-04-05T10:00:00Z");

        assertEquals("GET\n/api/cameras?page=1&size=10\n2026-04-05T10:00:00Z", result);
    }

    @Test
    @DisplayName("Should handle DELETE method")
    void testDeleteMethod() {
        String result = signatureUtil.buildStringToSign("DELETE", "/api/cameras/1", "2026-04-05T10:00:00Z");

        assertEquals("DELETE\n/api/cameras/1\n2026-04-05T10:00:00Z", result);
    }

    @Test
    @DisplayName("Should handle PUT method")
    void testPutMethod() {
        String result = signatureUtil.buildStringToSign("PUT", "/api/cameras/1", "2026-04-05T10:00:00Z");

        assertEquals("PUT\n/api/cameras/1\n2026-04-05T10:00:00Z", result);
    }
}
