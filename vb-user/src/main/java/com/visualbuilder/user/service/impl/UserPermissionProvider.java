package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.security.PermissionProvider;
import com.visualbuilder.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户权限查询实现 — 委托 SysUserMapper 的 SQL 查询。
 */
@Service
@RequiredArgsConstructor
public class UserPermissionProvider implements PermissionProvider {

    private final SysUserMapper sysUserMapper;

    @Override
    public List<String> getRoleKeys(Long userId) {
        if (userId == null) return List.of();
        return sysUserMapper.selectRoleKeysByUserId(userId);
    }

    @Override
    public List<String> getPermissionKeys(Long userId) {
        if (userId == null) return List.of();
        return sysUserMapper.selectPermissionKeysByUserId(userId);
    }
}
