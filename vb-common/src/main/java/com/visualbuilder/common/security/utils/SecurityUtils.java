package com.visualbuilder.common.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 — 获取当前登录用户信息
 */
public final class SecurityUtils {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private SecurityUtils() {}

    public static void setContext(Long userId, String username) {
        CONTEXT.set(new UserContext(userId, username));
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        // 优先从 ThreadLocal 获取
        UserContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx.userId();
        }
        // 回退到 SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
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
     * 判断是否已登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    private record UserContext(Long userId, String username) {}
}
