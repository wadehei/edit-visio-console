package com.visualbuilder.system.entity;

import com.visualbuilder.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLog extends BaseEntity {
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String requestParams;
    private Integer status;
    private String errorMsg;
    private String ipAddress;
    private Long costTime;
}
