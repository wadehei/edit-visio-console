package com.visualbuilder.system.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.system.dto.HealthCheckVO;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import com.visualbuilder.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "获取系统配置")
    @GetMapping("/configs")
    public R<List<SysConfig>> configs() {
        return R.ok(systemService.getConfigs());
    }

    @Operation(summary = "更新系统配置")
    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "系统管理", action = "更新系统配置")
    @PutMapping("/configs/{configKey}")
    public R<Void> updateConfig(
            @PathVariable String configKey,
            @RequestParam String configValue) {
        systemService.updateConfig(configKey, configValue);
        return R.ok();
    }

    @Operation(summary = "操作日志（分页）")
    @GetMapping("/logs")
    public R<PageResult<SysLog>> logs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(systemService.getLogs(module, page, pageSize));
    }

    @Operation(summary = "登录日志（分页）")
    @GetMapping("/login-logs")
    public R<PageResult<LoginLog>> loginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(systemService.getLoginLogs(username, page, pageSize));
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public R<HealthCheckVO> health() {
        return R.ok(systemService.healthCheck());
    }
}
