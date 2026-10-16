package com.visualbuilder.project.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.project.dto.ProjectCreateDTO;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "项目管理")
public class ProjectController {

    private final ProjectService projectService;

    @OpLog(module = "项目管理", action = "创建项目")
    @Operation(summary = "创建项目")
    @PostMapping
    public R<ProjectVO> createProject(@Valid @RequestBody ProjectCreateDTO dto) {
        return R.ok(projectService.createProject(dto));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{projectId}")
    public R<ProjectVO> getProjectDetail(@PathVariable Long projectId) {
        return R.ok(projectService.getProjectDetail(projectId));
    }

    @Operation(summary = "项目列表")
    @GetMapping
    public R<PageResult<ProjectVO>> listProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(projectService.listProjects(page, pageSize, keyword, status));
    }

    @OpLog(module = "项目管理", action = "更新项目")
    @Operation(summary = "更新项目")
    @PutMapping("/{projectId}")
    public R<ProjectVO> updateProject(@PathVariable Long projectId,
                                      @Valid @RequestBody ProjectCreateDTO dto) {
        return R.ok(projectService.updateProject(projectId, dto));
    }

    @OpLog(module = "项目管理", action = "删除项目")
    @Operation(summary = "删除项目")
    @DeleteMapping("/{projectId}")
    public R<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return R.ok();
    }

    @OpLog(module = "项目管理", action = "批量删除项目")
    @Operation(summary = "批量删除项目")
    @DeleteMapping("/batch")
    public R<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        return R.ok(projectService.batchDeleteProjects(ids));
    }

    @OpLog(module = "项目管理", action = "复制项目")
    @Operation(summary = "复制项目")
    @PostMapping("/{projectId}/copy")
    public R<ProjectVO> copyProject(@PathVariable Long projectId) {
        return R.ok(projectService.copyProject(projectId));
    }

    @Operation(summary = "回收站列表")
    @GetMapping("/trash")
    public R<PageResult<ProjectVO>> listTrash(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(projectService.listTrash(page, pageSize));
    }

    @OpLog(module = "项目管理", action = "恢复项目")
    @Operation(summary = "从回收站恢复")
    @PostMapping("/{projectId}/restore")
    public R<Void> restoreProject(@PathVariable Long projectId) {
        projectService.restoreProject(projectId);
        return R.ok();
    }

    @OpLog(module = "项目管理", action = "永久删除项目")
    @Operation(summary = "永久删除")
    @DeleteMapping("/{projectId}/permanent")
    public R<Void> permanentDelete(@PathVariable Long projectId) {
        projectService.permanentDeleteProject(projectId);
        return R.ok();
    }

    @Operation(summary = "导出项目配置")
    @GetMapping("/{projectId}/export")
    public R<Map<String, Object>> exportProject(@PathVariable Long projectId) {
        return R.ok(projectService.exportProject(projectId));
    }

    @OpLog(module = "项目管理", action = "导入项目")
    @Operation(summary = "导入项目配置")
    @PostMapping("/import")
    public R<ProjectVO> importProject(@RequestBody Map<String, Object> projectData) {
        return R.ok(projectService.importProject(projectData));
    }
}
