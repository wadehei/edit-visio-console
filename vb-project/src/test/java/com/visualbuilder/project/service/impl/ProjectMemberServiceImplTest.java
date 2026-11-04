package com.visualbuilder.project.service.impl;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.project.entity.Project;
import com.visualbuilder.project.entity.ProjectMember;
import com.visualbuilder.project.mapper.ProjectMapper;
import com.visualbuilder.project.mapper.ProjectMemberMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * ProjectMemberServiceImpl 单元测试 — 聚焦成员管理的越权防护（仅项目所有者可管理成员）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMemberServiceImpl 项目成员管理")
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    private ProjectMemberServiceImpl memberService;

    @BeforeEach
    void setUp() {
        memberService = new ProjectMemberServiceImpl(projectMapper);
        ReflectionTestUtils.setField(memberService, "baseMapper", projectMemberMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clearContext();
    }

    private Project projectOwnedBy(Long projectId, Long ownerId) {
        Project project = new Project();
        project.setId(projectId);
        project.setUserId(ownerId);
        return project;
    }

    @Test
    @DisplayName("未登录添加成员抛出 UNAUTHORIZED")
    void addMember_unauthenticated() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.addMember(1L, 9L, "view"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("项目不存在时添加成员抛出 PROJECT_NOT_FOUND")
    void addMember_projectNotFound() {
        SecurityUtils.setContext(1L, "tester");
        when(projectMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.addMember(1L, 9L, "view"));
        assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("非项目所有者添加成员抛出 FORBIDDEN")
    void addMember_notOwner() {
        SecurityUtils.setContext(2L, "outsider");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.addMember(1L, 9L, "view"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("非项目所有者修改成员权限抛出 FORBIDDEN")
    void updatePermission_notOwner() {
        SecurityUtils.setContext(2L, "outsider");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.updatePermission(1L, 9L, "edit"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("非项目所有者移除成员抛出 FORBIDDEN")
    void removeMember_notOwner() {
        SecurityUtils.setContext(2L, "outsider");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.removeMember(1L, 9L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("非项目所有者转让所有权抛出 FORBIDDEN")
    void transferOwnership_notOwner() {
        SecurityUtils.setContext(2L, "outsider");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.transferOwnership(1L, 9L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("非项目成员查看成员列表抛出 FORBIDDEN")
    void listMembers_notMember() {
        SecurityUtils.setContext(2L, "outsider");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.listMembers(1L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("项目所有者可查看成员列表")
    void listMembers_ownerSuccess() {
        SecurityUtils.setContext(1L, "owner");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        when(projectMemberMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(memberService.listMembers(1L).isEmpty());
    }

    @Test
    @DisplayName("分配 owner 权限被白名单拒绝（PARAM_ERROR）")
    void addMember_ownerPermissionRejected() {
        SecurityUtils.setContext(1L, "owner");
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.addMember(1L, 9L, "owner"));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("项目所有者视为 owner，通过 edit 角色校验")
    void assertProjectRole_ownerPasses() {
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));

        assertDoesNotThrow(() -> memberService.assertProjectRole(1L, 1L, "edit"));
    }

    @Test
    @DisplayName("非项目成员未达最低角色抛出 FORBIDDEN")
    void assertProjectRole_nonMemberForbidden() {
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        when(projectMemberMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.assertProjectRole(1L, 9L, "edit"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("仅 view 权限的成员不满足 edit 校验")
    void hasProjectRole_viewOnlyFalse() {
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        ProjectMember member = new ProjectMember();
        member.setProjectId(1L);
        member.setUserId(9L);
        member.setPermission("view");
        when(projectMemberMapper.selectOne(any(), anyBoolean())).thenReturn(member);

        assertFalse(memberService.hasProjectRole(1L, 9L, "edit"));
    }

    @Test
    @DisplayName("edit 权限成员满足 edit 校验")
    void hasProjectRole_editTrue() {
        when(projectMapper.selectById(1L)).thenReturn(projectOwnedBy(1L, 1L));
        ProjectMember member = new ProjectMember();
        member.setProjectId(1L);
        member.setUserId(9L);
        member.setPermission("edit");
        when(projectMemberMapper.selectOne(any(), anyBoolean())).thenReturn(member);

        assertTrue(memberService.hasProjectRole(1L, 9L, "edit"));
    }
}
