package com.aick.mmp.shared.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;
    
    private Key getSigningKey() {
        // 检查密钥是否已注入
        if (jwtSecret == null) {
            throw new IllegalStateException("JWT密钥未注入，请检查配置");
        }
        
        if (jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT密钥为空，请检查配置文件中的security.jwt.secret参数");
        }
        
        byte[] keyBytes = jwtSecret.getBytes();
        // 确保密钥长度符合HS512算法要求（至少512位，即64字节）
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT密钥长度不足64字节，请检查配置文件中的security.jwt.secret参数，当前长度: " + keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        try {
            // 确保密钥已正确注入
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT密钥未正确配置，当前值: " + (jwtSecret == null ? "null" : "empty"));
            }
            
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);

            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            System.err.println("生成JWT令牌失败，密钥信息: " + getSecretInfo());
            e.printStackTrace();
            throw new RuntimeException("生成JWT令牌失败: " + e.getMessage(), e);
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            // 添加更详细的错误信息
            throw new RuntimeException("无法解析JWT令牌，密钥长度: " + (jwtSecret != null ? jwtSecret.getBytes().length : "null") + 
                "，令牌: " + token, e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | 
                 UnsupportedJwtException | IllegalArgumentException ex) {
            // 添加日志记录
            System.err.println("JWT验证失败: " + ex.getMessage() + 
                "，密钥长度: " + (jwtSecret != null ? jwtSecret.getBytes().length : "null"));
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    // 添加获取当前密钥信息的方法，用于调试
    public String getSecretInfo() {
        if (jwtSecret == null) {
            return "密钥为null";
        }
        return "密钥长度: " + jwtSecret.getBytes().length + "，密钥预览: " + 
            (jwtSecret.length() > 10 ? jwtSecret.substring(0, 10) + "..." : jwtSecret);
    }
}