package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.system.entity.SysDept;
import com.visualbuilder.system.mapper.SysDeptMapper;
import com.visualbuilder.system.service.AdminDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept>
        implements AdminDeptService {

    @Override
    public List<SysDept> getDeptTree() {
        return this.list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, StatusConst.Enable.ENABLED)
                .orderByAsc(SysDept::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(SysDept dept) {
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(StatusConst.Enable.ENABLED);
        }
        if (dept.getTenantId() == null) {
            dept.setTenantId(0L);
        }
        this.save(dept);
        log.info("创建部门: name={}", dept.getDeptName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long deptId, SysDept updates) {
        SysDept dept = this.getById(deptId);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "部门不存在");
        }
        if (StringUtils.hasText(updates.getDeptName())) dept.setDeptName(updates.getDeptName());
        if (updates.getSortOrder() != null) dept.setSortOrder(updates.getSortOrder());
        if (updates.getStatus() != null) dept.setStatus(updates.getStatus());
        if (updates.getLeader() != null) dept.setLeader(updates.getLeader());
        if (updates.getPhone() != null) dept.setPhone(updates.getPhone());
        this.updateById(dept);
    }
}
