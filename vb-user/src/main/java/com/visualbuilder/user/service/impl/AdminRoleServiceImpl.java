package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.entity.SysRolePermission;
import com.visualbuilder.user.mapper.SysRolePermissionMapper;
import com.visualbuilder.user.entity.SysRole;
import com.visualbuilder.user.mapper.SysRoleMapper;
import com.visualbuilder.user.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole>
        implements AdminRoleService {

    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<SysRole> listRoles(int page, int pageSize, String keyword) {
        Page<SysRole> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(keyword), SysRole::getRoleName, keyword)
                .orderByAsc(SysRole::getSortOrder);
        Page<SysRole> result = this.page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(SysRole role) {
        long count = this.count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, role.getRoleKey()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "角色标识已存在");
        }
        if (role.getStatus() == null) {
            role.setStatus(StatusConst.Enable.ENABLED);
        }
        this.save(role);
        log.info("创建角色: roleKey={}", role.getRoleKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long roleId, SysRole updates) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (StringUtils.hasText(updates.getRoleName())) role.setRoleName(updates.getRoleName());
        if (updates.getSortOrder() != null) role.setSortOrder(updates.getSortOrder());
        if (updates.getStatus() != null) role.setStatus(updates.getStatus());
        if (updates.getDataScope() != null) role.setDataScope(updates.getDataScope());
        if (updates.getRemark() != null) role.setRemark(updates.getRemark());
        this.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 删除旧权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));

        // 批量插入新权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        log.info("分配角色权限: roleId={}, permissionCount={}", roleId,
                permissionIds != null ? permissionIds.size() : 0);
    }
}
