package com.visualbuilder.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色权限关联实体。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private Long permissionId;
}
