package com.visualbuilder.user.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.user.dto.*;
import com.visualbuilder.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "个人信息/修改密码")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> getProfile() {
        return R.ok(userService.getCurrentUserInfo());
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/me")
    public R<Void> updateProfile(@RequestBody UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public R<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return R.ok();
    }
}
