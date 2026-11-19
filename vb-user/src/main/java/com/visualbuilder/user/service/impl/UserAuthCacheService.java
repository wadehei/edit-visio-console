package com.visualbuilder.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.security.UserAuthCacheVersion;
import com.visualbuilder.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户角色/权限缓存 — 登录、获取当前用户信息、刷新令牌等场景复用同一份查询结果。
 *
 * <p>角色与权限一并缓存于单个 Key，避免一次请求两次 Redis 往返；Key 带全局版本号前缀，
 * 角色/权限变更时通过 {@link UserAuthCacheVersion#bump()} 整体失效。
 * Redis 不可用时降级为直查数据库，不影响主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthCacheService {

    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserAuthCacheVersion cacheVersion;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** 缓存 TTL = 基础值 + 0~10% 随机抖动，避免大量 key 在同一时刻集中过期形成缓存雪崩 */
    private static Duration jitteredCacheTtl() {
        return CACHE_TTL.plusSeconds(ThreadLocalRandom.current().nextLong(CACHE_TTL.getSeconds() / 10 + 1));
    }

    public List<String> getRoleKeys(Long userId) {
        return snapshot(userId).roles();
    }

    public List<String> getPermissionKeys(Long userId) {
        return snapshot(userId).permissions();
    }

    private Snapshot snapshot(Long userId) {
        String key = RedisKeyConst.userAuth(cacheVersion.current(), userId);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, Snapshot.class);
            }
        } catch (Exception e) {
            log.warn("读取用户角色权限缓存失败，回退数据库: userId={}, err={}", userId, e.getMessage());
        }

        Snapshot snapshot = new Snapshot(
                sysUserMapper.selectRoleKeysByUserId(userId),
                sysUserMapper.selectPermissionKeysByUserId(userId));

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot), jitteredCacheTtl());
        } catch (Exception e) {
            log.warn("写入用户角色权限缓存失败: userId={}, err={}", userId, e.getMessage());
        }
        return snapshot;
    }

    /** 用户角色与权限的快照 */
    public record Snapshot(List<String> roles, List<String> permissions) {
    }
}
