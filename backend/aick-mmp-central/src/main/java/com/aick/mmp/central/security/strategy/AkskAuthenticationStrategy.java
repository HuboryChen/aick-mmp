package com.aick.mmp.central.security.strategy;

import com.aick.mmp.central.security.UnifiedPrincipal;
import com.aick.mmp.central.service.ApiKeyService;
import com.aick.mmp.central.service.SystemAppService;
import com.aick.mmp.shared.util.SignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AK/SK 认证策略 (API Key + Signature)
 * 优先级: 2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AkskAuthenticationStrategy implements AuthenticationStrategy {

    private final ApiKeyService apiKeyService;
    private final SystemAppService systemAppService;
    private final SignatureUtil signatureUtil;

    @Override
    public String getName() {
        return "AKSK";
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        String accessKey = request.getHeader("X-Access-Key");
        return accessKey != null;
    }

    @Override
    public Authentication authenticate(HttpServletRequest request) {
        String accessKey = request.getHeader("X-Access-Key");
        String signature = request.getHeader("X-Signature");
        String timestamp = request.getHeader("X-Timestamp");

        // Validate required headers
        if (accessKey == null) {
            log.debug("No access key header present");
            return null;
        }

        if (signature == null || timestamp == null) {
            log.debug("Missing signature or timestamp header");
            return null;
        }

        // Validate access key format
        if (!accessKey.startsWith("ak_")) {
            log.debug("Invalid access key format");
            return null;
        }

        try {
            // Validate timestamp
            if (!signatureUtil.isTimestampValid(timestamp)) {
                log.warn("Timestamp expired or invalid for access key: {}", accessKey);
                return null;
            }

            // Try API key authentication first
            Optional<String> secretKeyOpt = apiKeyService.getDecryptedSecretKey(accessKey);
            UnifiedPrincipal.IdentityType identityType = UnifiedPrincipal.IdentityType.SYSTEM_APP;
            log.debug("API key check result for {}: {}", accessKey, secretKeyOpt.isPresent() ? "found" : "not found");
            
            // If API key not found, try SystemApp
            if (secretKeyOpt.isEmpty()) {
                log.debug("Trying SystemApp authentication for: {}", accessKey);
                secretKeyOpt = systemAppService.getDecryptedAppSecretByKey(accessKey);
                identityType = UnifiedPrincipal.IdentityType.SYSTEM_APP;
                log.debug("SystemApp check result for {}: {}", accessKey, secretKeyOpt.isPresent() ? "found" : "not found");
                if (secretKeyOpt.isEmpty()) {
                    log.warn("Access key not found in ApiKey or SystemApp: {}", accessKey);
                    log.warn("SystemApp exists in DB? Checking database...");
                }
            }
            
            if (secretKeyOpt.isEmpty()) {
                log.warn("Invalid access key (not found in ApiKey or SystemApp): {}", accessKey);
                return null;
            }

            // Build and verify signature
            String method = request.getMethod();
            String path = request.getRequestURI();
            String stringToSign = signatureUtil.buildStringToSign(method, path, timestamp);
            String secretKey = secretKeyOpt.get();

            if (!signatureUtil.verifySignature(stringToSign, signature, secretKey)) {
                log.warn("Invalid signature for access key: {}", accessKey);
                return null;
            }

            // Create unified principal based on key type
            UnifiedPrincipal principal = UnifiedPrincipal.builder()
                    .identityId(accessKey)
                    .identityType(identityType)
                    .authMethod(UnifiedPrincipal.AuthMethod.API_KEY)
                    .build();

            // Create authentication token
            Authentication authentication = new com.aick.mmp.central.security.UnifiedAuthenticationToken(
                    principal,
                    accessKey,
                    getAuthorities(principal)
            );

            log.debug("AK/SK authentication successful for: {} (type: {})", accessKey, identityType);
            return authentication;

        } catch (Exception e) {
            log.error("AK/SK authentication failed", e);
            return null;
        }
    }

    private Set<org.springframework.security.core.GrantedAuthority> getAuthorities(UnifiedPrincipal principal) {
        Set<org.springframework.security.core.GrantedAuthority> authorities = new HashSet<>();

        if (principal.getRole() != null) {
            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + principal.getRole()));
        }

        if (principal.getPermissions() != null) {
            authorities.addAll(principal.getPermissions().stream()
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet()));
        }

        return authorities;
    }

    @Override
    public String getErrorMessage() {
        return "Invalid API key or signature";
    }
}
