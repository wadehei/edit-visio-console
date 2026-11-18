package com.visualbuilder.user.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.dto.AdminUserCreateDTO;
import com.visualbuilder.user.dto.AdminUserUpdateDTO;
import com.visualbuilder.user.dto.AdminUserVO;
import com.visualbuilder.user.entity.SysUser;

public interface AdminUserService extends BaseService<SysUser> {
    PageResult<AdminUserVO> listUsers(int page, int pageSize, String keyword, Integer status);
    /** 查询用户详情（脱敏 VO），不存在时抛出业务异常 */
    AdminUserVO getUserDetail(Long userId);
    void createUser(AdminUserCreateDTO dto);
    void updateUser(Long userId, AdminUserUpdateDTO dto);
    void resetPassword(Long userId, String newPassword);
    void updateStatus(Long userId, Integer status);
    /** 删除用户，并级联清理用户-角色关联 */
    void deleteUser(Long userId);
}
