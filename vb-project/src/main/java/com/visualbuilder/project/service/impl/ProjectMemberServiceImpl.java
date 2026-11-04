package com.visualbuilder.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.constant.ProjectRole;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl extends BaseServiceImpl<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

    private final ProjectMapper projectMapper;

    /** 允许分配的成员权限（owner 只能通过转让所有权获得） */
    private static final Set<String> ASSIGNABLE_PERMISSIONS =
            Set.of(ProjectRole.VIEW, ProjectRole.EDIT, ProjectRole.ADMIN);

    /** 角色等级：数值越大权限越高 */
    private static final Map<String, Integer> ROLE_LEVELS = Map.of(
            ProjectRole.VIEW, 1,
            ProjectRole.EDIT, 2,
            ProjectRole.ADMIN, 3,
            ProjectRole.OWNER, 4);

    /** 无任何角色 */
    private static final int NO_ROLE = 0;

    @Override
    public List<ProjectMember> listMembers(Long projectId) {
        // 未登录一律拒绝；仅项目所有者或成员可查看成员列表
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!isProjectOwnerOrMember(projectId, currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该项目成员");
        }
        return this.list(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByDesc(ProjectMember::getCreatedTime)
                .last("LIMIT " + LimitsConst.MAX_MEMBER_LIST_SIZE));
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
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        String role = baseMapper.selectEffectiveRole(projectId, currentUserId);
        if (role == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        }
        if (!ProjectRole.OWNER.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅项目所有者可管理成员");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchAddMembers(Long projectId, List<ProjectMemberAddDTO> members) {
        if (members == null || members.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "成员列表不能为空");
        }
        if (members.size() > LimitsConst.MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "单次批量添加不能超过" + LimitsConst.MAX_BATCH_SIZE + "人");
        }

        assertProjectOwner(projectId);

        // 预加载项目已有成员，避免逐条 count 查询（N+1）
        Set<Long> existingUserIds = this.list(new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId))
                .stream()
                .map(ProjectMember::getUserId)
                .collect(Collectors.toSet());

        List<ProjectMember> toInsert = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (ProjectMemberAddDTO dto : members) {
            Long userId = dto.getUserId();
            // 用户 ID 为空、入参重复、已是成员：一律跳过（不中断整批）
            if (userId == null || !seen.add(userId) || existingUserIds.contains(userId)) {
                continue;
            }
            String permission;
            try {
                permission = normalizeAssignablePermission(dto.getPermission());
            } catch (BusinessException e) {
                // 权限非法：跳过该条（纯内存校验，不触碰数据库事务）
                log.warn("批量添加成员跳过: projectId={}, userId={}, reason={}",
                        projectId, userId, e.getMessage());
                continue;
            }
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(userId);
            member.setPermission(permission);
            member.setCreatedTime(now);
            toInsert.add(member);
        }

        if (!toInsert.isEmpty()) {
            baseMapper.batchInsert(toInsert);
        }

        int success = toInsert.size();
        log.info("批量添加项目成员: projectId={}, total={}, success={}", projectId, members.size(), success);
        return BatchResultVO.builder()
                .total(members.size())
                .success(success)
                .skipped(members.size() - success)
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
        newOwnerMember.setPermission(ProjectRole.OWNER);
        this.updateById(newOwnerMember);

        // 将原所有者降级为 admin
        if (!oldOwnerId.equals(newOwnerId)) {
            ProjectMember oldOwnerMember = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getUserId, oldOwnerId));
            if (oldOwnerMember != null) {
                oldOwnerMember.setPermission(ProjectRole.ADMIN);
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
        String role = baseMapper.selectEffectiveRole(projectId, userId);
        return role != null && !role.isEmpty();
    }

    @Override
    public List<Long> listProjectIdsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getUserId, userId))
                .stream()
                .map(ProjectMember::getProjectId)
                .distinct()
                .toList();
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
        String role = baseMapper.selectEffectiveRole(projectId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        }
        if (roleLevel(role) < requiredLevel(minRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "项目权限不足，需要 " + requiredLevelName(minRole) + " 及以上权限");
        }
    }

    @Override
    public boolean hasProjectRole(Long projectId, Long userId, String minRole) {
        if (projectId == null || userId == null) {
            return false;
        }
        return roleLevel(baseMapper.selectEffectiveRole(projectId, userId)) >= requiredLevel(minRole);
    }

    /** 解析角色字符串等级；null/空串（项目不存在或非成员）视为无权限 */
    private static int roleLevel(String role) {
        if (role == null || role.isEmpty()) {
            return NO_ROLE;
        }
        return ROLE_LEVELS.getOrDefault(role.toLowerCase(), NO_ROLE);
    }

    /** 解析最低角色要求，默认 view */
    private static int requiredLevel(String minRole) {
        return ROLE_LEVELS.getOrDefault(
                minRole != null ? minRole.toLowerCase() : ProjectRole.VIEW,
                ROLE_LEVELS.get(ProjectRole.VIEW));
    }

    private static String requiredLevelName(String minRole) {
        return minRole != null ? minRole.toLowerCase() : ProjectRole.VIEW;
    }

    /**
     * 校验并规范化待分配的成员权限：为空默认 view；仅允许 view/edit/admin。
     */
    private String normalizeAssignablePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return ProjectRole.VIEW;
        }
        String normalized = permission.trim().toLowerCase();
        if (!ASSIGNABLE_PERMISSIONS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权限取值非法，仅允许 view/edit/admin");
        }
        return normalized;
    }
}
