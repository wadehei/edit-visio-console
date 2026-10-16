package com.visualbuilder.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl extends BaseServiceImpl<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

    private final ProjectMapper projectMapper;

    @Override
    public List<ProjectMember> listMembers(Long projectId) {
        return this.list(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByDesc(ProjectMember::getCreatedTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long projectId, Long userId, String permission) {
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
        member.setPermission(permission != null ? permission : "view");
        member.setCreatedTime(LocalDateTime.now());
        this.save(member);
        log.info("添加项目成员: projectId={}, userId={}, permission={}", projectId, userId, member.getPermission());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchAddMembers(Long projectId, List<Map<String, Object>> members) {
        if (members == null || members.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "成员列表不能为空");
        }
        if (members.size() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量添加不能超过50人");
        }

        int success = 0;
        int skipped = 0;

        for (Map<String, Object> memberData : members) {
            try {
                Long userId = Long.valueOf(memberData.get("userId").toString());
                String permission = memberData.getOrDefault("permission", "view").toString();
                addMember(projectId, userId, permission);
                success++;
            } catch (Exception e) {
                skipped++;
                log.warn("批量添加成员失败: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", members.size());
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(Long projectId, Long userId, String permission) {
        ProjectMember member = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "成员不存在");
        }
        member.setPermission(permission);
        this.updateById(member);
        log.info("更新成员权限: projectId={}, userId={}, permission={}", projectId, userId, permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long projectId, Long userId) {
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
    public String getMemberPermission(Long projectId, Long userId) {
        ProjectMember member = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        return member != null ? member.getPermission() : null;
    }
}
