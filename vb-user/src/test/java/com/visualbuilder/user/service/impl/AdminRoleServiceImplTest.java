package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.user.entity.SysRole;
import com.visualbuilder.user.entity.SysRolePermission;
import com.visualbuilder.user.mapper.SysRoleMapper;
import com.visualbuilder.user.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminRoleServiceImpl 单元测试 — 聚焦角色权限批量分配。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRoleServiceImpl 角色权限分配")
class AdminRoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRolePermissionMapper rolePermissionMapper;

    private AdminRoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new AdminRoleServiceImpl(rolePermissionMapper);
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    private SysRole role(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        return role;
    }

    @Test
    @DisplayName("角色不存在抛出 DATA_NOT_FOUND")
    void assignPermissions_roleNotFound() {
        when(roleMapper.selectById(9L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleService.assignPermissions(9L, List.of(1L)));
        assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getCode());
        verify(rolePermissionMapper, never()).batchInsert(any());
    }

    @Test
    @DisplayName("分配权限使用批量插入，不再循环单条插入")
    @SuppressWarnings("unchecked")
    void assignPermissions_batchInsert() {
        when(roleMapper.selectById(5L)).thenReturn(role(5L));

        roleService.assignPermissions(5L, List.of(1L, 2L, 3L));

        verify(rolePermissionMapper, never()).insert(any(SysRolePermission.class));
        ArgumentCaptor<List<SysRolePermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionMapper).batchInsert(captor.capture());
        List<SysRolePermission> relations = captor.getValue();
        assertEquals(3, relations.size());
        assertEquals(5L, relations.get(0).getRoleId());
        assertEquals(3L, relations.get(2).getPermissionId());
    }

    @Test
    @DisplayName("空权限列表仅删除旧关联，不执行批量插入")
    void assignPermissions_emptyList() {
        when(roleMapper.selectById(5L)).thenReturn(role(5L));

        roleService.assignPermissions(5L, Collections.emptyList());

        verify(rolePermissionMapper).delete(any());
        verify(rolePermissionMapper, never()).batchInsert(any());
    }
}
