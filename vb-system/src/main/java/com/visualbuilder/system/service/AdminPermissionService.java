package com.visualbuilder.system.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.system.dto.PermissionCreateDTO;
import com.visualbuilder.system.dto.PermissionUpdateDTO;
import com.visualbuilder.system.entity.SysPermission;

import java.util.List;

public interface AdminPermissionService extends BaseService<SysPermission> {
    List<SysPermission> getPermissionTree();
    void createPermission(PermissionCreateDTO dto);
    void updatePermission(Long permissionId, PermissionUpdateDTO dto);
}
