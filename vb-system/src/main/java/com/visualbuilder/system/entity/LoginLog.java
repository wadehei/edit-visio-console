package com.visualbuilder.system.entity;

import com.visualbuilder.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class LoginLog extends BaseEntity {
    private Long userId;
    private String username;
    private String loginType;
    private Integer status;
    private String ipAddress;
    private String userAgent;
    private String message;
}
