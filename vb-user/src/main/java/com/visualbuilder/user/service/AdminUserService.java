package com.visualbuilder.user.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.entity.SysUser;

public interface AdminUserService extends BaseService<SysUser> {
    PageResult<SysUser> listUsers(int page, int pageSize, String keyword, Integer status);
    void createUser(SysUser user);
    void updateUser(Long userId, SysUser user);
    void resetPassword(Long userId, String newPassword);
    void updateStatus(Long userId, Integer status);
}
