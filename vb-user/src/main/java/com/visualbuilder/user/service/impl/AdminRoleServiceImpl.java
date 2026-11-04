package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.dto.RoleCreateDTO;
import com.visualbuilder.user.dto.RoleUpdateDTO;
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
import java.util.stream.Collectors;

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
    public void createRole(RoleCreateDTO dto) {
        long count = this.count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "角色标识已存在");
        }
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setSortOrder(dto.getSortOrder());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusConst.Enable.ENABLED);
        role.setRemark(dto.getRemark());
        role.setTenantId(0L);
        try {
            this.save(role);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发创建同一角色标识时，依赖数据库唯一约束兜底
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "角色标识已存在");
        }
        log.info("创建角色: roleKey={}", role.getRoleKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long roleId, RoleUpdateDTO dto) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (dto.getRoleName() != null) role.setRoleName(dto.getRoleName());
        if (dto.getSortOrder() != null) role.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        if (dto.getDataScope() != null) role.setDataScope(dto.getDataScope());
        if (dto.getRemark() != null) role.setRemark(dto.getRemark());
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
            List<SysRolePermission> relations = permissionIds.stream()
                    .map(permId -> {
                        SysRolePermission rp = new SysRolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permId);
                        return rp;
                    })
                    .collect(Collectors.toList());
            rolePermissionMapper.batchInsert(relations);
        }
        log.info("分配角色权限: roleId={}, permissionCount={}", roleId,
                permissionIds != null ? permissionIds.size() : 0);
    }
}
