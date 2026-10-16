package com.visualbuilder.common.security.utils;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUtils 安全工具类")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityUtils.clearContext();
    }

    @Test
    @DisplayName("未设置 context → userId 为 null")
    void noContext_userIdNull() {
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertTrue(SecurityUtils.getCurrentRoles().isEmpty());
        assertTrue(SecurityUtils.getCurrentPermissions().isEmpty());
    }

    @Test
    @DisplayName("setContext(userId, username) → 基础信息可用")
    void basicContext() {
        SecurityUtils.setContext(42L, "testuser");
        assertEquals(42L, SecurityUtils.getCurrentUserId());
        assertEquals("testuser", SecurityUtils.getCurrentUsername());
        assertTrue(SecurityUtils.isAuthenticated());
    }

    @Test
    @DisplayName("setContext with roles/permissions → 角色权限可用")
    void fullContext() {
        SecurityUtils.setContext(1L, "admin",
                List.of("SUPER_ADMIN", "USER"),
                List.of("system:config:read", "system:config:write"));

        assertEquals(1L, SecurityUtils.getCurrentUserId());
        assertEquals(List.of("SUPER_ADMIN", "USER"), SecurityUtils.getCurrentRoles());
        assertEquals(List.of("system:config:read", "system:config:write"), SecurityUtils.getCurrentPermissions());
    }

    @Test
    @DisplayName("clearContext → 所有信息清除")
    void clearContext_works() {
        SecurityUtils.setContext(1L, "admin", List.of("ADMIN"), List.of("perm:a"));
        SecurityUtils.clearContext();
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertTrue(SecurityUtils.getCurrentRoles().isEmpty());
    }

    @Test
    @DisplayName("isAuthenticated 未设置 → false")
    void isAuthenticated_noContext() {
        assertFalse(SecurityUtils.isAuthenticated());
    }

    @Test
    @DisplayName("null roles/permissions → 返回空列表不抛异常")
    void nullRolesAndPerms() {
        SecurityUtils.setContext(1L, "user", null, null);
        assertNotNull(SecurityUtils.getCurrentRoles());
        assertNotNull(SecurityUtils.getCurrentPermissions());
        assertTrue(SecurityUtils.getCurrentRoles().isEmpty());
    }
}
