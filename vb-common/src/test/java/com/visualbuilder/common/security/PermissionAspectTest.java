package com.visualbuilder.common.security;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.annotation.Logical;
import com.visualbuilder.common.security.annotation.RequiresPermission;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.common.security.utils.SecurityUtils;
import org.junit.jupiter.api.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PermissionAspect 权限校验逻辑单元测试。
 */
@DisplayName("PermissionAspect 权限切面")
class PermissionAspectTest {

    private PermissionAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new PermissionAspect();
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clearContext();
    }

    // ─── 权限校验 ────────────────────────────────────────

    @Test
    @DisplayName("未登录用户触发权限校验 → 抛出 UNAUTHORIZED")
    void checkPermission_notLoggedIn() {
        // userId 为 null
        assertThrows(BusinessException.class, () ->
                aspect.checkPermission(null, mockPermission("test:perm", Logical.AND)));
    }

    @Test
    @DisplayName("已登录 + 拥有权限 → 通过")
    void checkPermission_hasPermission() {
        SecurityUtils.setContext(1L, "user", List.of(), List.of("system:user:list", "system:user:create"));
        assertDoesNotThrow(() ->
                aspect.checkPermission(null, mockPermission("system:user:list", Logical.AND)));
    }

    @Test
    @DisplayName("已登录 + 缺少权限 → 抛出 FORBIDDEN")
    void checkPermission_lacksPermission() {
        SecurityUtils.setContext(1L, "user", List.of(), List.of("system:user:list"));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.checkPermission(null, mockPermission("system:user:delete", Logical.AND)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("AND 逻辑 + 缺少一个权限 → FORBIDDEN")
    void checkPermission_andLogical_missingOne() {
        SecurityUtils.setContext(1L, "user", List.of(), List.of("perm:a"));
        assertThrows(BusinessException.class, () ->
                aspect.checkPermission(null, mockPermissionLogical(new String[]{"perm:a", "perm:b"}, Logical.AND)));
    }

    @Test
    @DisplayName("OR 逻辑 + 有其中一个权限 → 通过")
    void checkPermission_orLogical_hasOne() {
        SecurityUtils.setContext(1L, "user", List.of(), List.of("perm:b"));
        assertDoesNotThrow(() ->
                aspect.checkPermission(null, mockPermissionLogical(new String[]{"perm:a", "perm:b"}, Logical.OR)));
    }

    @Test
    @DisplayName("超级管理员 → 任何权限都通过")
    void checkPermission_superAdmin() {
        SecurityUtils.setContext(1L, "admin", List.of("SUPER_ADMIN"), List.of());
        assertDoesNotThrow(() ->
                aspect.checkPermission(null, mockPermission("any:permission:here", Logical.AND)));
    }

    // ─── 角色校验 ────────────────────────────────────────

    @Test
    @DisplayName("未登录用户触发角色校验 → 抛出 UNAUTHORIZED")
    void checkRole_notLoggedIn() {
        assertThrows(BusinessException.class, () ->
                aspect.checkRole(null, mockRole("ADMIN", Logical.AND)));
    }

    @Test
    @DisplayName("已登录 + 拥有角色 → 通过")
    void checkRole_hasRole() {
        SecurityUtils.setContext(1L, "user", List.of("ADMIN", "EDITOR"), List.of());
        assertDoesNotThrow(() ->
                aspect.checkRole(null, mockRole("ADMIN", Logical.AND)));
    }

    @Test
    @DisplayName("已登录 + 缺少角色 → 抛出 FORBIDDEN")
    void checkRole_lacksRole() {
        SecurityUtils.setContext(1L, "user", List.of("USER"), List.of());
        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.checkRole(null, mockRole("ADMIN", Logical.AND)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("AND 角色 + 缺少一个 → FORBIDDEN")
    void checkRole_andLogical_missingOne() {
        SecurityUtils.setContext(1L, "user", List.of("ADMIN"), List.of());
        assertThrows(BusinessException.class, () ->
                aspect.checkRole(null, mockRoleLogical(new String[]{"ADMIN", "SUPER"}, Logical.AND)));
    }

    @Test
    @DisplayName("OR 角色 + 有其中一个 → 通过")
    void checkRole_orLogical_hasOne() {
        SecurityUtils.setContext(1L, "user", List.of("EDITOR"), List.of());
        assertDoesNotThrow(() ->
                aspect.checkRole(null, mockRoleLogical(new String[]{"ADMIN", "EDITOR"}, Logical.OR)));
    }

    // ─── Mock helpers ────────────────────────────────────

    private RequiresPermission mockPermission(String perm, Logical logical) {
        return mockPermissionLogical(new String[]{perm}, logical);
    }

    private RequiresPermission mockPermissionLogical(String[] perms, Logical logical) {
        return new RequiresPermission() {
            @Override public Class<? extends Annotation> annotationType() { return RequiresPermission.class; }
            @Override public String[] value() { return perms; }
            @Override public Logical logical() { return logical; }
        };
    }

    private RequiresRole mockRole(String role, Logical logical) {
        return mockRoleLogical(new String[]{role}, logical);
    }

    private RequiresRole mockRoleLogical(String[] roles, Logical logical) {
        return new RequiresRole() {
            @Override public Class<? extends Annotation> annotationType() { return RequiresRole.class; }
            @Override public String[] value() { return roles; }
            @Override public Logical logical() { return logical; }
        };
    }
}
