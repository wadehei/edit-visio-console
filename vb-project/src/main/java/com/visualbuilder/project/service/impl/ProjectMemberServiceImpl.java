package com.visualbuilder.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.project.entity.ProjectMember;
import com.visualbuilder.project.mapper.ProjectMemberMapper;
import com.visualbuilder.project.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl extends BaseServiceImpl<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

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
    public void updatePermission(Long projectId, Long userId, String permission) {
        ProjectMember member = this.getOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "成员不存在");
        }
        member.setPermission(permission);
        this.updateById(member);
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
}
