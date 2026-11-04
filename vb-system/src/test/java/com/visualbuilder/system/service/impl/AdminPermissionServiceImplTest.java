package com.visualbuilder.system.service.impl;

import com.visualbuilder.system.entity.SysPermission;
import com.visualbuilder.system.mapper.SysPermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AdminPermissionServiceImpl 单元测试 — 聚焦权限树递归构建。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPermissionServiceImpl 权限树")
class AdminPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper permissionMapper;

    private AdminPermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new AdminPermissionServiceImpl();
        ReflectionTestUtils.setField(permissionService, "baseMapper", permissionMapper);
    }

    private SysPermission perm(Long id, Long parentId, String name) {
        SysPermission p = new SysPermission();
        p.setId(id);
        p.setParentId(parentId);
        p.setPermissionName(name);
        p.setStatus(1);
        return p;
    }

    @Test
    @DisplayName("按 parentId 递归构建完整多级权限树")
    void getPermissionTree_buildsNestedTree() {
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                perm(1L, 0L, "系统管理"),
                perm(2L, 1L, "用户管理"),
                perm(3L, 2L, "新增用户"),
                perm(4L, 0L, "内容管理")));

        List<SysPermission> tree = permissionService.getPermissionTree();

        assertEquals(2, tree.size());
        SysPermission root = tree.get(0);
        assertEquals(1L, root.getId());
        assertEquals(1, root.getChildren().size());
        SysPermission child = root.getChildren().get(0);
        assertEquals(2L, child.getId());
        assertEquals(1, child.getChildren().size());
        assertEquals(3L, child.getChildren().get(0).getId());
    }

    @Test
    @DisplayName("无根节点时返回空树")
    void getPermissionTree_emptyWhenNoRoot() {
        when(permissionMapper.selectList(any())).thenReturn(List.of(perm(2L, 1L, "孤儿节点")));

        assertTrue(permissionService.getPermissionTree().isEmpty());
    }
}
