package com.visualbuilder.common.security;

import java.util.List;

/**
 * 用户权限查询 SPI — 解耦 PermissionAspect 与具体用户模块。
 *
 * <p>由 vb-user 模块提供实现，运行时通过 Spring 注入。</p>
 */
public interface PermissionProvider {

    /** 查询用户拥有的角色标识列表（如 SUPER_ADMIN, USER） */
    List<String> getRoleKeys(Long userId);

    /** 查询用户拥有的权限标识列表（如 project:create, page:edit） */
    List<String> getPermissionKeys(Long userId);
}
