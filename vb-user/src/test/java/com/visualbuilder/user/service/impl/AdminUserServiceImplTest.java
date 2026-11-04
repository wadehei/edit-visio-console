package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminUserServiceImpl 单元测试 — 聚焦删除用户时的关联清理。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl 用户删除")
class AdminUserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    private AdminUserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new AdminUserServiceImpl(passwordEncoder, redisTemplate, userRoleMapper);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("删除用户时级联清理用户-角色关联")
    void deleteUser_cleansUserRole() {
        SysUser user = new SysUser();
        user.setId(7L);
        when(userMapper.selectById(7L)).thenReturn(user);

        userService.deleteUser(7L);

        verify(userRoleMapper).delete(any());
        verify(userMapper).deleteById(7L);
    }

    @Test
    @DisplayName("删除不存在的用户抛出 USER_NOT_FOUND")
    void deleteUser_notFound() {
        when(userMapper.selectById(9L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.deleteUser(9L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        verify(userRoleMapper, never()).delete(any());
    }
}
