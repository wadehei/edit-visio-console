package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.security.PermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户权限查询实现 — 复用 {@link UserAuthCacheService} 的带缓存查询，避免每处重复连表。
 */
@Service
@RequiredArgsConstructor
public class UserPermissionProvider implements PermissionProvider {

    private final UserAuthCacheService userAuthCacheService;

    @Override
    public List<String> getRoleKeys(Long userId) {
        if (userId == null) return List.of();
        return userAuthCacheService.getRoleKeys(userId);
    }

    @Override
    public List<String> getPermissionKeys(Long userId) {
        if (userId == null) return List.of();
        return userAuthCacheService.getPermissionKeys(userId);
    }
}
