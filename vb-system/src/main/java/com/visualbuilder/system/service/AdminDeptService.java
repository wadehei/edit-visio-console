package com.visualbuilder.system.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.system.entity.SysDept;

import java.util.List;

public interface AdminDeptService extends BaseService<SysDept> {
    List<SysDept> getDeptTree();
    void createDept(SysDept dept);
    void updateDept(Long deptId, SysDept dept);
}
