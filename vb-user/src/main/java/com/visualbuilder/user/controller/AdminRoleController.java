package com.visualbuilder.user.controller;

import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.constant.SystemRole;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.user.dto.RoleCreateDTO;
import com.visualbuilder.user.dto.RoleUpdateDTO;
import com.visualbuilder.user.entity.SysRole;
import com.visualbuilder.user.service.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台 - 角色管理")
@RestController
@RequestMapping("/api/v1/admin/roles")
@Validated
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "角色列表（分页）")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @GetMapping
    public R<PageResult<SysRole>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false)
            @Size(max = LimitsConst.MAX_TEXT_LENGTH,
                    message = "关键字最多 " + LimitsConst.MAX_TEXT_LENGTH + " 个字符") String keyword) {
        return R.ok(adminRoleService.listRoles(page, pageSize, keyword));
    }

    @Operation(summary = "角色详情")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @GetMapping("/{id}")
    public R<SysRole> detail(@PathVariable Long id) {
        return R.ok(adminRoleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "角色管理", action = "创建角色")
    @PostMapping
    public R<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        adminRoleService.createRole(dto);
        return R.ok();
    }

    @Operation(summary = "更新角色")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "角色管理", action = "更新角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        adminRoleService.updateRole(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "角色管理", action = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return R.ok();
    }

    @Operation(summary = "分配角色权限")
    @RequiresRole(SystemRole.SUPER_ADMIN)
    @OpLog(module = "角色管理", action = "分配权限")
    @PutMapping("/{id}/permissions")
    public R<Void> assignPermissions(@PathVariable Long id,
                                     @RequestBody
                                     @Size(max = LimitsConst.MAX_BATCH_SIZE,
                                             message = "单次最多分配 " + LimitsConst.MAX_BATCH_SIZE + " 项权限")
                                     List<Long> permissionIds) {
        adminRoleService.assignPermissions(id, permissionIds);
        return R.ok();
    }
}
