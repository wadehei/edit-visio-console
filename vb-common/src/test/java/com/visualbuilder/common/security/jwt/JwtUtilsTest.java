package com.visualbuilder.common.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试。
 */
@DisplayName("JwtUtils JWT 工具类")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        setField(jwtUtils, "secret",
                "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-test-ok");
        setField(jwtUtils, "accessTokenExpire", 1800L);
        setField(jwtUtils, "refreshTokenExpire", 604800L);
    }

    @Test
    @DisplayName("生成 access token 不为空")
    void generateAccessToken() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of());
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("生成 refresh token 不为空")
    void generateRefreshToken() {
        String token = jwtUtils.generateRefreshToken(1L);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("从 token 解析 userId")
    void parseUserId() {
        String token = jwtUtils.generateAccessToken(42L, "testuser", Map.of());
        Long userId = jwtUtils.getUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("从 token 解析 username")
    void parseUsername() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of());
        assertEquals("admin", jwtUtils.getUsername(token));
    }

    @Test
    @DisplayName("自定义 claims 可解析")
    void customClaims() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of("role", "super"));
        assertEquals("super", jwtUtils.parseToken(token).get("role", String.class));
    }

    @Test
    @DisplayName("有效 token 校验通过")
    void validToken() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of());
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("无效 token 校验失败")
    void invalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("空/null token 校验失败")
    void emptyToken() {
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    @DisplayName("JTI 不为空（防重放）")
    void jtiPresent() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of());
        assertNotNull(jwtUtils.getJti(token));
    }

    @Test
    @DisplayName("剩余 TTL 为正数")
    void remainingTtl() {
        String token = jwtUtils.generateAccessToken(1L, "admin", Map.of());
        assertTrue(jwtUtils.getRemainingTtl(token) > 0);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败: " + fieldName, e);
        }
    }
}
