package com.visualbuilder.common.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-权限关联实体（sys_role_permission）。
 * 由 vb-user 与 vb-system 共用，故统一置于公共模块，避免重复定义。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private Long permissionId;
}
