package com.visualbuilder.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private Long tenantId;
    private String roleName;
    private String roleKey;
    private Integer sortOrder;
    private Integer dataScope;
    private Integer status;
    private Integer isSystem;
    private String remark;
}
