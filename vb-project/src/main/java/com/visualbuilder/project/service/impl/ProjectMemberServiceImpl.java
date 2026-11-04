package com.visualbuilder.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.project.dto.BatchResultVO;
import com.visualbuilder.project.dto.ProjectMemberAddDTO;
import com.visualbuilder.project.entity.Project;
import com.visualbuilder.project.entity.ProjectMember;
import com.visualbuilder.project.mapper.ProjectMapper;
import com.visualbuilder.project.mapper.ProjectMemberMapper;
import com.visualbuilder.project.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl extends BaseServiceImpl<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

    private final ProjectMapper projectMapper;

    /** 允许分配的成员权限（owner 只能通过转让所有权获得） */
    private static final Set<String> ASSIGNABLE_PERMISSIONS = Set.of("view", "edit", "admin");

    /** 角色等级：数值越大权限越高 */
    private static final Map<String, Integer> ROLE_LEVELS = Map.of(
            "view", 1, "edit", 2, "admin", 3, "owner", 4);

    /** 无任何角色 */
    private static final int NO_ROLE = 0;

    /** 成员列表上限 — 未分页接口的硬上限，避免全表返回 */
    private static final int MAX_MEMBER_LIST_SIZE = 200;

    @Override
    public List<ProjectMember> listMembers(Long projectId) {
        // 未登录一律拒绝；仅项目所有者或成员可查看成员列表
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        if (!isProjectOwnerOrMember(projectId, currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该项目成员");
        }
        return this.list(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByDesc(ProjectMember::getCreatedTime)
                .last("LIMIT " + MAX_MEMBER_LIST_SIZE));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long projectId, Long userId, String permission) {
        assertProjectOwner(projectId);
        insertMember(projectId, userId, permission);
    }

    private void insertMember(Long projectId, Long userId, String permission) {
        // 检查是否已是成员
        long count = this.count(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "该用户已是项目成员");
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setPermission(normalizeAssignablePermission(permission));
        member.setCreatedTime(LocalDateTime.now());
        this.save(member);
        log.info("添加项目成员: projectId={}, userId={}, permission={}", projectId, userId, member.getPermission());
    }

    /**
     * 校验当前用户是否为项目所有者；仅项目所有者可管理成员。
     */
    private void assertProjectOwner(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        }
        if (!currentUserId.equals(project.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅项目所有者可管理成员");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchAddMembers(Long projectId, List<ProjectMemberAddDTO> members) {
        if (members == null || members.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "成员列表不能为空");
        }
        if (members.size() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量添加不能超过50人");
        }

        assertProjectOwner(projectId);

        int success = 0;
        int skipped = 0;

        for (ProjectMemberAddDTO dto : members) {
            try {
                insertMember(projectId, dto.getUserId(), dto.getPermission());
                success++;
            } catch (Exception e) {
                skipped++;
                log.warn("批量添加成员失败: userId={}, error={}", dto.getUserId(), e.getMessage());
            }
        }

        return BatchResultVO.builder()
                .total(members.size())
                .success(success)
                .skipped(skipped)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(Long projectId, Long userId, String permission) {
        assertProjectOwner(projectId);
        ProjectMember member = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "成员不存在");
        }
        member.setPermission(normalizeAssignablePermission(permission));
        this.updateById(member);
        log.info("更新成员权限: projectId={}, userId={}, permission={}", projectId, userId, permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long projectId, Long userId) {
        assertProjectOwner(projectId);
        int rows = this.baseMapper.delete(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "成员不存在");
        }
        log.info("移除项目成员: projectId={}, userId={}", projectId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwnership(Long projectId, Long newOwnerId) {
        assertProjectOwner(projectId);
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        }

        // 检查新所有者是否是成员
        ProjectMember newOwnerMember = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, newOwnerId));
        if (newOwnerMember == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "目标用户不是项目成员");
        }

        // 更新项目所有者
        Long oldOwnerId = project.getUserId();
        project.setUserId(newOwnerId);
        projectMapper.updateById(project);

        // 更新成员角色
        newOwnerMember.setPermission("owner");
        this.updateById(newOwnerMember);

        // 将原所有者降级为 admin
        if (!oldOwnerId.equals(newOwnerId)) {
            ProjectMember oldOwnerMember = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getUserId, oldOwnerId));
            if (oldOwnerMember != null) {
                oldOwnerMember.setPermission("admin");
                this.updateById(oldOwnerMember);
            }
        }

        log.info("转让项目所有者: projectId={}, from={}, to={}", projectId, oldOwnerId, newOwnerId);
    }

    @Override
    public boolean isProjectMember(Long projectId, Long userId) {
        long count = this.count(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        return count > 0;
    }

    @Override
    public boolean isProjectOwnerOrMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        if (project != null && userId.equals(project.getUserId())) {
            return true;
        }
        return isProjectMember(projectId, userId);
    }

    @Override
    public String getMemberPermission(Long projectId, Long userId) {
        ProjectMember member = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        return member != null ? member.getPermission() : null;
    }

    @Override
    public void assertProjectRole(Long projectId, Long userId, String minRole) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        }
        if (resolveRoleLevel(project, userId) < requiredLevel(minRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "项目权限不足，需要 " + requiredLevelName(minRole) + " 及以上权限");
        }
    }

    @Override
    public boolean hasProjectRole(Long projectId, Long userId, String minRole) {
        if (projectId == null || userId == null) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        return resolveRoleLevel(project, userId) >= requiredLevel(minRole);
    }

    /** 解析用户在项目中的角色等级；项目所有者视为 owner */
    private int resolveRoleLevel(Project project, Long userId) {
        if (userId.equals(project.getUserId())) {
            return ROLE_LEVELS.get("owner");
        }
        String permission = getMemberPermission(project.getId(), userId);
        return permission != null ? ROLE_LEVELS.getOrDefault(permission.toLowerCase(), NO_ROLE) : NO_ROLE;
    }

    /** 解析最低角色要求，默认 view */
    private static int requiredLevel(String minRole) {
        return ROLE_LEVELS.getOrDefault(minRole != null ? minRole.toLowerCase() : "view", ROLE_LEVELS.get("view"));
    }

    private static String requiredLevelName(String minRole) {
        return minRole != null ? minRole.toLowerCase() : "view";
    }

    /**
     * 校验并规范化待分配的成员权限：为空默认 view；仅允许 view/edit/admin。
     */
    private String normalizeAssignablePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return "view";
        }
        String normalized = permission.trim().toLowerCase();
        if (!ASSIGNABLE_PERMISSIONS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权限取值非法，仅允许 view/edit/admin");
        }
        return normalized;
    }
}
