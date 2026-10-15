package com.visualbuilder.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private Long tenantId;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Long deptId;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateDate;
    private String remark;
}
