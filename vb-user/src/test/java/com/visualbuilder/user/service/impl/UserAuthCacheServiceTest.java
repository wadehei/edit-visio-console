package com.visualbuilder.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.security.UserAuthCacheVersion;
import com.visualbuilder.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserAuthCacheService 单元测试 — 覆盖缓存命中、未命中回填与 Redis 故障降级三条路径。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthCacheService 用户角色权限缓存")
class UserAuthCacheServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private UserAuthCacheVersion cacheVersion;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserAuthCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new UserAuthCacheService(sysUserMapper, redisTemplate, objectMapper, cacheVersion);
    }

    @Test
    @DisplayName("缓存未命中时查库并回填缓存")
    void cacheMiss_queriesDbAndBackfills() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(cacheVersion.current()).thenReturn(0L);
        when(valueOps.get("user:auth:0:7")).thenReturn(null);
        when(sysUserMapper.selectRoleKeysByUserId(7L)).thenReturn(List.of("USER"));
        when(sysUserMapper.selectPermissionKeysByUserId(7L)).thenReturn(List.of("page:view"));

        assertEquals(List.of("USER"), cacheService.getRoleKeys(7L));
        assertEquals(List.of("page:view"), cacheService.getPermissionKeys(7L));

        verify(valueOps, atLeastOnce()).set(eq("user:auth:0:7"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("缓存命中时直接返回，不查库")
    void cacheHit_skipsDatabase() throws Exception {
        String json = objectMapper.writeValueAsString(
                new UserAuthCacheService.Snapshot(List.of("ADMIN"), List.of("perm:a")));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(cacheVersion.current()).thenReturn(3L);
        when(valueOps.get("user:auth:3:7")).thenReturn(json);

        assertEquals(List.of("ADMIN"), cacheService.getRoleKeys(7L));
        assertEquals(List.of("perm:a"), cacheService.getPermissionKeys(7L));

        verify(sysUserMapper, never()).selectRoleKeysByUserId(any());
        verify(sysUserMapper, never()).selectPermissionKeysByUserId(any());
    }

    @Test
    @DisplayName("读取缓存异常时降级为直查数据库")
    void redisReadFailure_fallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(cacheVersion.current()).thenReturn(0L);
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(sysUserMapper.selectRoleKeysByUserId(7L)).thenReturn(List.of("USER"));
        when(sysUserMapper.selectPermissionKeysByUserId(7L)).thenReturn(List.of());

        assertEquals(List.of("USER"), cacheService.getRoleKeys(7L));
        assertEquals(List.of(), cacheService.getPermissionKeys(7L));
    }

    @Test
    @DisplayName("写入缓存异常时仍返回数据库结果")
    void redisWriteFailure_stillReturnsDatabaseResult() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(cacheVersion.current()).thenReturn(0L);
        when(valueOps.get(anyString())).thenReturn(null);
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(sysUserMapper.selectRoleKeysByUserId(7L)).thenReturn(List.of("USER"));
        when(sysUserMapper.selectPermissionKeysByUserId(7L)).thenReturn(List.of());

        assertDoesNotThrow(() -> assertEquals(List.of("USER"), cacheService.getRoleKeys(7L)));
    }
}
