package com.visualbuilder.project.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.project.dto.ProjectCreateDTO;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public R<ProjectVO> createProject(@Valid @RequestBody ProjectCreateDTO dto) {
        return R.ok(projectService.createProject(dto));
    }

    @GetMapping("/{projectId}")
    public R<ProjectVO> getProjectDetail(@PathVariable Long projectId) {
        return R.ok(projectService.getProjectDetail(projectId));
    }

    @GetMapping
    public R<PageResult<ProjectVO>> listProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(projectService.listProjects(page, pageSize, keyword, status));
    }

    @PutMapping("/{projectId}")
    public R<ProjectVO> updateProject(@PathVariable Long projectId,
                                      @Valid @RequestBody ProjectCreateDTO dto) {
        return R.ok(projectService.updateProject(projectId, dto));
    }

    @DeleteMapping("/{projectId}")
    public R<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return R.ok();
    }

    @PostMapping("/{projectId}/copy")
    public R<ProjectVO> copyProject(@PathVariable Long projectId) {
        return R.ok(projectService.copyProject(projectId));
    }
}
