package com.visualbuilder.project.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.project.dto.ProjectCreateDTO;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.entity.Project;

public interface ProjectService extends BaseService<Project> {

    ProjectVO createProject(ProjectCreateDTO dto);

    ProjectVO getProjectDetail(Long projectId);

    PageResult<ProjectVO> listProjects(int page, int pageSize, String keyword, Integer status);

    ProjectVO updateProject(Long projectId, ProjectCreateDTO dto);

    void deleteProject(Long projectId);

    ProjectVO copyProject(Long projectId);
}
