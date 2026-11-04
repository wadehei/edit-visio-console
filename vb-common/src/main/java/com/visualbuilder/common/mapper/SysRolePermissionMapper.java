package com.visualbuilder.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visualbuilder.common.core.entity.SysRolePermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-权限关联 Mapper（sys_role_permission），由 vb-user 与 vb-system 共用。
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量插入角色权限关联，避免循环单条插入带来的性能损耗。
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_permission (role_id, permission_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.roleId}, #{item.permissionId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<SysRolePermission> list);
}
