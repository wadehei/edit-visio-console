package com.visualbuilder.system.controller;

import com.visualbuilder.common.core.constant.SystemRole;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.system.dto.PermissionCreateDTO;
import com.visualbuilder.system.dto.PermissionUpdateDTO;
import com.visualbuilder.system.entity.SysPermission;
import com.visualbuilder.system.service.AdminPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台 - 权限/菜单管理")
@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "权限树")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @GetMapping("/tree")
    public R<List<SysPermission>> tree() {
        return R.ok(adminPermissionService.getPermissionTree());
    }

    @Operation(summary = "权限详情")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @GetMapping("/{id}")
    public R<SysPermission> detail(@PathVariable Long id) {
        return R.ok(adminPermissionService.getById(id));
    }

    @Operation(summary = "创建权限")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "权限管理", action = "创建权限")
    @PostMapping
    public R<Void> create(@Valid @RequestBody PermissionCreateDTO dto) {
        adminPermissionService.createPermission(dto);
        return R.ok();
    }

    @Operation(summary = "更新权限")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "权限管理", action = "更新权限")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateDTO dto) {
        adminPermissionService.updatePermission(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除权限")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "权限管理", action = "删除权限")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminPermissionService.deletePermission(id);
        return R.ok();
    }
}
