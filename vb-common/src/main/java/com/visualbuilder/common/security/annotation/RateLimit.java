package com.visualbuilder.common.security.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解 — 基于 Redis 滑动窗口。
 *
 * <pre>
 * // 每个用户每 60 秒最多 5 次
 * &#64;RateLimit(key = "login", maxRequests = 5, windowSeconds = 60)
 * &#64;PostMapping("/login")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 限流 key 前缀（实际 key = prefix + userId/IP） */
    String key();

    /** 窗口内最大请求数 */
    int maxRequests() default 10;

    /** 时间窗口（秒） */
    int windowSeconds() default 60;

    /** 限流维度 */
    LimitType type() default LimitType.USER;

    /** 超限提示信息 */
    String message() default "请求过于频繁，请稍后再试";

    enum LimitType {
        /** 按用户限流（未登录按 IP） */
        USER,
        /** 按 IP 限流 */
        IP,
        /** 全局限流（不区分用户/IP） */
        GLOBAL
    }
}
