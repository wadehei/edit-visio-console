package com.visualbuilder.user.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台 - 用户管理")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表（分页）")
    @RequiresRole("SUPER_ADMIN")
    @GetMapping
    public R<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(adminUserService.listUsers(page, pageSize, keyword, status));
    }

    @Operation(summary = "用户详情")
    @RequiresRole("SUPER_ADMIN")
    @GetMapping("/{id}")
    public R<SysUser> detail(@PathVariable Long id) {
        SysUser user = adminUserService.getById(id);
        if (user != null) user.setPassword(null);
        return R.ok(user);
    }

    @Operation(summary = "创建用户")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "用户管理", action = "创建用户")
    @PostMapping
    public R<Void> create(@RequestBody SysUser user) {
        adminUserService.createUser(user);
        return R.ok();
    }

    @Operation(summary = "更新用户信息")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "用户管理", action = "更新用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        adminUserService.updateUser(id, user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "用户管理", action = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminUserService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "用户管理", action = "重置密码")
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        adminUserService.resetPassword(id, newPassword);
        return R.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "用户管理", action = "更新状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateStatus(id, status);
        return R.ok();
    }
}
