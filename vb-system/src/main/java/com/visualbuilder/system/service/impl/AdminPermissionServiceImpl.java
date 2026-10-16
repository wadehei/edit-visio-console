package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.system.entity.SysPermission;
import com.visualbuilder.system.mapper.SysPermissionMapper;
import com.visualbuilder.system.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public void createPermission(SysPermission permission) {
        if (!StringUtils.hasText(permission.getPermissionName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权限名称不能为空");
        }
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(StatusConst.Enable.ENABLED);
        }
        this.save(permission);
        log.info("创建权限: name={}", permission.getPermissionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(Long permissionId, SysPermission updates) {
        SysPermission permission = this.getById(permissionId);
        if (permission == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "权限不存在");
        }
        if (StringUtils.hasText(updates.getPermissionName())) permission.setPermissionName(updates.getPermissionName());
        if (updates.getSortOrder() != null) permission.setSortOrder(updates.getSortOrder());
        if (updates.getStatus() != null) permission.setStatus(updates.getStatus());
        if (updates.getVisible() != null) permission.setVisible(updates.getVisible());
        if (updates.getPath() != null) permission.setPath(updates.getPath());
        if (updates.getComponent() != null) permission.setComponent(updates.getComponent());
        if (updates.getIcon() != null) permission.setIcon(updates.getIcon());
        if (updates.getApiMethod() != null) permission.setApiMethod(updates.getApiMethod());
        if (updates.getApiPath() != null) permission.setApiPath(updates.getApiPath());
        if (updates.getRemark() != null) permission.setRemark(updates.getRemark());
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
        // 注意：SysPermission 没有 children 字段，树结构通过排序 parentId 实现
        // 前端可根据 parentId 自行构建树
        return children;
    }
}
