package com.visualbuilder.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire:1800}")
    private long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800}")
    private long refreshTokenExpire;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret 未配置或长度不足！请设置环境变量 JWT_SECRET（至少 32 字符）");
        }
        if (secret.contains("dev-secret") || secret.contains("default-secret")) {
            log.warn("⚠️  警告: JWT Secret 使用了开发默认值，生产环境务必通过环境变量 JWT_SECRET 覆盖！");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpire * 1000))
                .id(java.util.UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenType", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpire * 1000))
                .id(java.util.UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token格式错误: {}", e.getMessage());
        }
        return false;
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String getJti(String token) {
        return parseToken(token).getId();
    }

    public long getRemainingTtl(String token) {
        Date expiration = parseToken(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public long getAccessTokenExpire() {
        return accessTokenExpire;
    }
}
