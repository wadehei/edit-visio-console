package com.visualbuilder.common.security;

import com.visualbuilder.common.core.constant.RedisKeyConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserAuthCacheVersion 单元测试 — 版本读取优先级与 Redis 故障降级。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthCacheVersion 用户权限缓存版本")
class UserAuthCacheVersionTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private UserAuthCacheVersion cacheVersion;

    @BeforeEach
    void setUp() {
        cacheVersion = new UserAuthCacheVersion(redisTemplate);
    }

    @Test
    @DisplayName("版本号缺省时按 0 处理")
    void current_defaultsToZeroWhenAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConst.USER_AUTH_VERSION_KEY)).thenReturn(null);

        assertEquals(0L, cacheVersion.current());
    }

    @Test
    @DisplayName("已写入版本号时按存储值解析")
    void current_parsesStoredValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConst.USER_AUTH_VERSION_KEY)).thenReturn("5");

        assertEquals(5L, cacheVersion.current());
    }

    @Test
    @DisplayName("Redis 读取异常时降级为 0")
    void current_fallsBackOnError() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertEquals(0L, cacheVersion.current());
    }

    @Test
    @DisplayName("bump 自增版本号使缓存整体失效")
    void bump_incrementsVersion() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cacheVersion.bump();

        verify(valueOps).increment(RedisKeyConst.USER_AUTH_VERSION_KEY);
    }

    @Test
    @DisplayName("Redis 写入异常时 bump 不抛出")
    void bump_swallowsErrors() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> cacheVersion.bump());
    }
}
