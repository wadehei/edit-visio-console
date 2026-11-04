package com.visualbuilder.user.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.dto.RoleCreateDTO;
import com.visualbuilder.user.dto.RoleUpdateDTO;
import com.visualbuilder.user.entity.SysRole;

import java.util.List;

public interface AdminRoleService extends BaseService<SysRole> {
    PageResult<SysRole> listRoles(int page, int pageSize, String keyword);
    void createRole(RoleCreateDTO dto);
    void updateRole(Long roleId, RoleUpdateDTO dto);
    void assignPermissions(Long roleId, List<Long> permissionIds);
}
