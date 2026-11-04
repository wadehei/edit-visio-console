package com.visualbuilder.user.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.dto.AdminUserCreateDTO;
import com.visualbuilder.user.dto.AdminUserUpdateDTO;
import com.visualbuilder.user.entity.SysUser;

public interface AdminUserService extends BaseService<SysUser> {
    PageResult<SysUser> listUsers(int page, int pageSize, String keyword, Integer status);
    void createUser(AdminUserCreateDTO dto);
    void updateUser(Long userId, AdminUserUpdateDTO dto);
    void resetPassword(Long userId, String newPassword);
    void updateStatus(Long userId, Integer status);
}
