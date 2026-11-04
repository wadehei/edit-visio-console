package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.system.dto.PermissionCreateDTO;
import com.visualbuilder.system.dto.PermissionUpdateDTO;
import com.visualbuilder.system.entity.SysPermission;
import com.visualbuilder.system.mapper.SysPermissionMapper;
import com.visualbuilder.system.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl extends BaseServiceImpl<SysPermissionMapper, SysPermission>
        implements AdminPermissionService {

    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> all = this.list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, StatusConst.Enable.ENABLED)
                .orderByAsc(SysPermission::getSortOrder));
        return buildTree(all, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionCreateDTO dto) {
        SysPermission permission = new SysPermission();
        permission.setPermissionName(dto.getPermissionName());
        permission.setPermissionKey(dto.getPermissionKey());
        permission.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        permission.setPermissionType(dto.getPermissionType());
        permission.setPath(dto.getPath());
        permission.setComponent(dto.getComponent());
        permission.setIcon(dto.getIcon());
        permission.setSortOrder(dto.getSortOrder());
        permission.setVisible(dto.getVisible());
        permission.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusConst.Enable.ENABLED);
        permission.setApiMethod(dto.getApiMethod());
        permission.setApiPath(dto.getApiPath());
        permission.setRemark(dto.getRemark());
        this.save(permission);
        log.info("创建权限: name={}", permission.getPermissionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(Long permissionId, PermissionUpdateDTO dto) {
        SysPermission permission = this.getById(permissionId);
        if (permission == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "权限不存在");
        }
        if (dto.getPermissionName() != null) permission.setPermissionName(dto.getPermissionName());
        if (dto.getSortOrder() != null) permission.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        if (dto.getVisible() != null) permission.setVisible(dto.getVisible());
        if (dto.getPath() != null) permission.setPath(dto.getPath());
        if (dto.getComponent() != null) permission.setComponent(dto.getComponent());
        if (dto.getIcon() != null) permission.setIcon(dto.getIcon());
        if (dto.getApiMethod() != null) permission.setApiMethod(dto.getApiMethod());
        if (dto.getApiPath() != null) permission.setApiPath(dto.getApiPath());
        if (dto.getRemark() != null) permission.setRemark(dto.getRemark());
        this.updateById(permission);
    }

    /**
     * 将扁平列表构建为树结构（通过 parentId 关联）
     */
    private List<SysPermission> buildTree(List<SysPermission> all, Long rootParentId) {
        Map<Long, List<SysPermission>> groupByParent = all.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() != null ? p.getParentId() : 0L));
        return buildChildren(groupByParent, rootParentId);
    }

    private List<SysPermission> buildChildren(Map<Long, List<SysPermission>> groupByParent, Long parentId) {
        List<SysPermission> children = groupByParent.getOrDefault(parentId, new ArrayList<>());
        // 递归填充子节点，构建完整树形结构
        for (SysPermission child : children) {
            child.setChildren(buildChildren(groupByParent, child.getId()));
        }
        return children;
    }
}
