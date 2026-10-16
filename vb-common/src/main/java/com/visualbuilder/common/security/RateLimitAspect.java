package com.visualbuilder.common.security;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.annotation.RateLimit;
import com.visualbuilder.common.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 限流 AOP 切面 — 基于 Redis 滑动窗口计数器。
 *
 * <p>使用 Lua 脚本保证原子性：INCR + EXPIRE 在一次 EVAL 中完成。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    /** Lua 脚本：原子性 INCR + EXPIRE */
    private static final String LUA_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('INCR', key) " +
            "if current == 1 then redis.call('EXPIRE', key, window) end " +
            "return current";

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint point, RateLimit rateLimit) {
        String limitKey = buildKey(rateLimit);

        Long current = redisTemplate.execute(
                SCRIPT,
                Collections.singletonList(limitKey),
                (long) rateLimit.maxRequests(),
                (long) rateLimit.windowSeconds());

        if (current != null && current > rateLimit.maxRequests()) {
            log.warn("限流触发: key={}, current={}, limit={}", limitKey, current, rateLimit.maxRequests());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, rateLimit.message());
        }
    }

    private String buildKey(RateLimit rateLimit) {
        String identifier;

        switch (rateLimit.type()) {
            case USER -> {
                Long userId = SecurityUtils.getCurrentUserId();
                identifier = userId != null ? "u:" + userId : "ip:" + getClientIp();
            }
            case IP -> identifier = "ip:" + getClientIp();
            case GLOBAL -> identifier = "global";
            default -> identifier = "ip:" + getClientIp();
        }

        return "rate:" + rateLimit.key() + ":" + identifier;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";

        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
