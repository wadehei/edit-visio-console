package com.visualbuilder.project.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.project.dto.ProjectCreateDTO;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.entity.Project;

import java.util.List;
import java.util.Map;

public interface ProjectService extends BaseService<Project> {

    ProjectVO createProject(ProjectCreateDTO dto);

    ProjectVO getProjectDetail(Long projectId);

    PageResult<ProjectVO> listProjects(int page, int pageSize, String keyword, Integer status);

    ProjectVO updateProject(Long projectId, ProjectCreateDTO dto);

    void deleteProject(Long projectId);

    Map<String, Object> batchDeleteProjects(List<Long> ids);

    ProjectVO copyProject(Long projectId);

    /** 回收站列表（已软删除的项目） */
    PageResult<ProjectVO> listTrash(int page, int pageSize);

    /** 从回收站恢复 */
    void restoreProject(Long projectId);

    /** 永久删除 */
    void permanentDeleteProject(Long projectId);
}
