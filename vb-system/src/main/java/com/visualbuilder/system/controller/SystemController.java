package com.visualbuilder.system.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import com.visualbuilder.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PutMapping("/configs/{configKey}")
    public R<Void> updateConfig(
            @PathVariable String configKey,
            @RequestParam String configValue) {
        systemService.updateConfig(configKey, configValue);
        return R.ok();
    }

    @Operation(summary = "操作日志")
    @GetMapping("/logs")
    public R<List<SysLog>> logs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(systemService.getLogs(module, pageNum, pageSize));
    }

    @Operation(summary = "登录日志")
    @GetMapping("/login-logs")
    public R<List<LoginLog>> loginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(systemService.getLoginLogs(username, pageNum, pageSize));
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        return R.ok(systemService.healthCheck());
    }
}
