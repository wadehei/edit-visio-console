package com.visualbuilder.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.project.dto.ProjectCreateDTO;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.entity.Project;
import com.visualbuilder.project.mapper.ProjectMapper;
import com.visualbuilder.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends BaseServiceImpl<ProjectMapper, Project>
        implements ProjectService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO createProject(ProjectCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "用户未登录");
        }

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setCoverUrl(dto.getCoverImage());
        project.setUserId(userId);
        project.setStatus(0);
        project.setSettings(dto.getSettings());
        save(project);

        return toVO(project);
    }

    @Override
    public ProjectVO getProjectDetail(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(40005, "项目不存在");
        }
        return toVO(project);
    }

    @Override
    public PageResult<ProjectVO> listProjects(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Project::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(Project::getStatus, status);
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            wrapper.eq(Project::getUserId, userId);
        }

        wrapper.orderByDesc(Project::getUpdatedTime);

        Page<Project> pageParam = new Page<>(page, pageSize);
        Page<Project> result = page(pageParam, wrapper);

        var voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateProject(Long projectId, ProjectCreateDTO dto) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(40005, "项目不存在");
        }

        if (StringUtils.hasText(dto.getName())) {
            project.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getCoverImage() != null) {
            project.setCoverUrl(dto.getCoverImage());
        }
        if (dto.getSettings() != null) {
            project.setSettings(dto.getSettings());
        }

        updateById(project);
        return toVO(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(40005, "项目不存在");
        }
        removeById(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO copyProject(Long projectId) {
        Project source = getById(projectId);
        if (source == null) {
            throw new BusinessException(40005, "源项目不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "用户未登录");
        }

        Project copy = new Project();
        copy.setName(source.getName() + " (副本)");
        copy.setDescription(source.getDescription());
        copy.setCoverUrl(source.getCoverUrl());
        copy.setUserId(userId);
        copy.setStatus(0);
        copy.setSettings(source.getSettings());
        save(copy);

        return toVO(copy);
    }

    private ProjectVO toVO(Project project) {
        return ProjectVO.builder()
                .projectId(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .coverImage(project.getCoverUrl())
                .status(project.getStatus())
                .settings(project.getSettings())
                .createdAt(project.getCreatedTime())
                .updatedAt(project.getUpdatedTime())
                .build();
    }
}
