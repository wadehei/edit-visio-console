package com.visualbuilder.common.security;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint point, RequiresPermission requiresPermission) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String[] requiredPermissions = requiresPermission.value();
        Logical logical = requiresPermission.logical();
        List<String> userPermissions = SecurityUtils.getCurrentPermissions();

        log.debug("权限校验: userId={}, required={}, logical={}, userPerms={}",
                userId, Arrays.toString(requiredPermissions), logical, userPermissions);

        // 超级管理员拥有所有权限
        if (SecurityUtils.getCurrentRoles().contains("SUPER_ADMIN")) {
            return;
        }

        Set<String> permSet = Set.copyOf(userPermissions);
        boolean hasPermission = checkAccess(requiredPermissions, logical, permSet::contains);

        if (!hasPermission) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "权限不足，需要: " + Arrays.toString(requiredPermissions));
        }
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint point, RequiresRole requiresRole) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String[] requiredRoles = requiresRole.value();
        Logical logical = requiresRole.logical();
        List<String> userRoles = SecurityUtils.getCurrentRoles();

        log.debug("角色校验: userId={}, required={}, logical={}, userRoles={}",
                userId, Arrays.toString(requiredRoles), logical, userRoles);

        Set<String> roleSet = Set.copyOf(userRoles);
        boolean hasRole = checkAccess(requiredRoles, logical, roleSet::contains);

        if (!hasRole) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "角色不足，需要: " + Arrays.toString(requiredRoles));
        }
    }

    /**
     * 通用权限/角色匹配逻辑
     */
    private boolean checkAccess(String[] required, Logical logical, java.util.function.Predicate<String> checker) {
        if (logical == Logical.AND) {
            return Arrays.stream(required).allMatch(checker);
        } else {
            return Arrays.stream(required).anyMatch(checker);
        }
    }
}
