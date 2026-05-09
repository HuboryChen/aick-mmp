package com.aick.mmp.shared.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        if (jwtSecret == null) {
            throw new IllegalStateException("JWT密钥未注入，请检查配置");
        }

        if (jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT密钥为空，请检查配置文件中的security.jwt.secret参数");
        }

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT密钥长度不足64字节，请检查配置文件中的security.jwt.secret参数，当前长度: " + keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, jwtExpiration);
    }

    public String generateToken(String username, long expiration) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT密钥未正确配置，当前值: " + (jwtSecret == null ? "null" : "empty"));
            }

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            System.err.println("生成JWT令牌失败，密钥信息: " + getSecretInfo());
            e.printStackTrace();
            throw new RuntimeException("生成JWT令牌失败: " + e.getMessage(), e);
        }
    }

    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiryDate = claims.getExpiration();
            return expiryDate.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("无法解析JWT令牌，密钥长度: " + (jwtSecret != null ? jwtSecret.getBytes().length : "null") +
                    "，令牌: " + token, e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException |
                 UnsupportedJwtException | IllegalArgumentException ex) {
            System.err.println("JWT验证失败: " + ex.getMessage() +
                    "，密钥长度: " + (jwtSecret != null ? jwtSecret.getBytes().length : "null"));
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getSecretInfo() {
        if (jwtSecret == null) {
            return "密钥为null";
        }
        return "密钥长度: " + jwtSecret.getBytes().length + "，密钥预览: " +
                (jwtSecret.length() > 10 ? jwtSecret.substring(0, 10) + "..." : jwtSecret);
    }
}
