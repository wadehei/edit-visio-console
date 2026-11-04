package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.system.dto.DeptCreateDTO;
import com.visualbuilder.system.dto.DeptUpdateDTO;
import com.visualbuilder.system.entity.SysDept;
import com.visualbuilder.system.mapper.SysDeptMapper;
import com.visualbuilder.system.service.AdminDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void createDept(DeptCreateDTO dto) {
        SysDept dept = new SysDept();
        dept.setDeptName(dto.getDeptName());
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setSortOrder(dto.getSortOrder());
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusConst.Enable.ENABLED);
        dept.setTenantId(0L);
        this.save(dept);
        log.info("创建部门: name={}", dept.getDeptName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long deptId, DeptUpdateDTO dto) {
        SysDept dept = this.getById(deptId);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "部门不存在");
        }
        if (dto.getDeptName() != null) dept.setDeptName(dto.getDeptName());
        if (dto.getSortOrder() != null) dept.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) dept.setStatus(dto.getStatus());
        if (dto.getLeader() != null) dept.setLeader(dto.getLeader());
        if (dto.getPhone() != null) dept.setPhone(dto.getPhone());
        this.updateById(dept);
    }
}
