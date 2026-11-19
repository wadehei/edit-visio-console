package com.visualbuilder.common.security;

import com.visualbuilder.common.core.constant.RedisKeyConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户角色/权限缓存的版本控制。
 *
 * <p>角色、权限或用户角色关系变更时，只需自增全局版本号即可让所有用户的缓存整体失效，
 * 避免按用户逐个清理（跨模块枚举受影响用户 / KEYS 扫描）。旧缓存不再被读取，到期自然淘汰。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthCacheVersion {

    private final StringRedisTemplate redisTemplate;

    /** 读取当前版本号，读取失败时按 0 处理（该次退化为直查数据库） */
    public long current() {
        try {
            String version = redisTemplate.opsForValue().get(RedisKeyConst.USER_AUTH_VERSION_KEY);
            return version == null ? 0L : Long.parseLong(version);
        } catch (Exception e) {
            log.warn("读取用户权限缓存版本失败，按 0 处理: {}", e.getMessage());
            return 0L;
        }
    }

    /** 使所有用户的角色/权限缓存整体失效 */
    public void bump() {
        try {
            redisTemplate.opsForValue().increment(RedisKeyConst.USER_AUTH_VERSION_KEY);
        } catch (Exception e) {
            log.warn("失效用户角色权限缓存失败: {}", e.getMessage());
        }
    }
}
