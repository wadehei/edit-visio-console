package com.visualbuilder.common.security;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.security.annotation.Logical;
import com.visualbuilder.common.security.annotation.RequiresPermission;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint point, RequiresPermission requiresPermission) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "未登录");
        }

        // TODO: 从数据库/缓存查询用户权限列表
        // 目前仅做登录校验，后续完善权限比对
        String[] requiredPermissions = requiresPermission.value();
        Logical logical = requiresPermission.logical();

        log.debug("权限校验: userId={}, permissions={}, logical={}",
                userId, Arrays.toString(requiredPermissions), logical);

        // 占位：实际权限比对逻辑待完善（需从 Redis 获取用户权限缓存）
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint point, RequiresRole requiresRole) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "未登录");
        }

        String[] requiredRoles = requiresRole.value();
        log.debug("角色校验: userId={}, roles={}", userId, Arrays.toString(requiredRoles));

        // 占位：实际角色比对逻辑待完善
    }
}
