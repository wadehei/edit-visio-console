package com.visualbuilder.system.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.system.entity.SysDept;
import com.visualbuilder.system.service.AdminDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台 - 部门管理")
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
public class AdminDeptController {

    private final AdminDeptService adminDeptService;

    @Operation(summary = "部门列表")
    @RequiresRole("SUPER_ADMIN")
    @GetMapping
    public R<List<SysDept>> list() {
        return R.ok(adminDeptService.getDeptTree());
    }

    @Operation(summary = "部门详情")
    @RequiresRole("SUPER_ADMIN")
    @GetMapping("/{id}")
    public R<SysDept> detail(@PathVariable Long id) {
        return R.ok(adminDeptService.getById(id));
    }

    @Operation(summary = "创建部门")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "部门管理", action = "创建部门")
    @PostMapping
    public R<Void> create(@RequestBody SysDept dept) {
        adminDeptService.createDept(dept);
        return R.ok();
    }

    @Operation(summary = "更新部门")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "部门管理", action = "更新部门")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        adminDeptService.updateDept(id, dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "部门管理", action = "删除部门")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminDeptService.removeById(id);
        return R.ok();
    }
}
