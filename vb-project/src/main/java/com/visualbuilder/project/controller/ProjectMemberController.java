package com.visualbuilder.project.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.project.entity.ProjectMember;
import com.visualbuilder.project.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "项目成员管理")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(summary = "成员列表")
    @GetMapping
    public R<List<ProjectMember>> list(@PathVariable Long projectId) {
        return R.ok(projectMemberService.listMembers(projectId));
    }

    @Operation(summary = "添加成员")
    @OpLog(module = "项目管理", action = "添加成员")
    @PostMapping
    public R<Void> add(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String permission = body.getOrDefault("permission", "view").toString();
        projectMemberService.addMember(projectId, userId, permission);
        return R.ok();
    }

    @Operation(summary = "批量添加成员")
    @OpLog(module = "项目管理", action = "批量添加成员")
    @PostMapping("/batch")
    public R<Map<String, Object>> batchAdd(@PathVariable Long projectId,
                                           @RequestBody List<Map<String, Object>> members) {
        return R.ok(projectMemberService.batchAddMembers(projectId, members));
    }

    @Operation(summary = "修改成员权限")
    @OpLog(module = "项目管理", action = "修改权限")
    @PutMapping("/{userId}")
    public R<Void> updatePermission(@PathVariable Long projectId,
                                    @PathVariable Long userId,
                                    @RequestParam String permission) {
        projectMemberService.updatePermission(projectId, userId, permission);
        return R.ok();
    }

    @Operation(summary = "移除成员")
    @OpLog(module = "项目管理", action = "移除成员")
    @DeleteMapping("/{userId}")
    public R<Void> remove(@PathVariable Long projectId, @PathVariable Long userId) {
        projectMemberService.removeMember(projectId, userId);
        return R.ok();
    }

    @Operation(summary = "转让项目所有者")
    @OpLog(module = "项目管理", action = "转让所有者")
    @PostMapping("/transfer/{newOwnerId}")
    public R<Void> transferOwnership(@PathVariable Long projectId,
                                     @PathVariable Long newOwnerId) {
        projectMemberService.transferOwnership(projectId, newOwnerId);
        return R.ok();
    }

    @Operation(summary = "检查用户是否是成员")
    @GetMapping("/{userId}/check")
    public R<Boolean> isMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return R.ok(projectMemberService.isProjectMember(projectId, userId));
    }

    @Operation(summary = "获取成员权限")
    @GetMapping("/{userId}/permission")
    public R<String> getPermission(@PathVariable Long projectId, @PathVariable Long userId) {
        return R.ok(projectMemberService.getMemberPermission(projectId, userId));
    }
}
