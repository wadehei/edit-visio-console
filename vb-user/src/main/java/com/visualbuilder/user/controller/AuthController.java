package com.visualbuilder.user.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.user.dto.*;
import com.visualbuilder.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理", description = "登录/注册/登出/刷新Token")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        // 注册后自动登录
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLogin(dto.getUsername());
        loginDTO.setPassword(dto.getPassword());
        return R.ok(userService.login(loginDTO));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return R.ok();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return R.ok(userService.refreshToken(refreshToken));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> getCurrentUser() {
        return R.ok(userService.getCurrentUserInfo());
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
