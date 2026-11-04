package com.visualbuilder.common.security.utils;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

/**
 * 安全工具类 — 获取当前登录用户信息
 */
public final class SecurityUtils {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private SecurityUtils() {}

    public static void setContext(Long userId, String username) {
        CONTEXT.set(new UserContext(userId, username, Collections.emptyList(), Collections.emptyList()));
    }

    public static void setContext(Long userId, String username,
                                  List<String> roles, List<String> permissions) {
        CONTEXT.set(new UserContext(userId, username,
                roles != null ? roles : Collections.emptyList(),
                permissions != null ? permissions : Collections.emptyList()));
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx.userId();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /**
     * 获取当前用户ID，未登录直接抛出 UNAUTHORIZED 业务异常。
     * 统一各业务层的「未登录」守卫，避免重复判断。
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return userId;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        UserContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx.username();
        }
        return null;
    }

    /**
     * 获取当前用户角色列表
     */
    public static List<String> getCurrentRoles() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.roles() : Collections.emptyList();
    }

    /**
     * 获取当前用户权限列表
     */
    public static List<String> getCurrentPermissions() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.permissions() : Collections.emptyList();
    }

    /**
     * 判断是否已登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    private record UserContext(Long userId, String username,
                               List<String> roles, List<String> permissions) {}
}
