package com.visualbuilder.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体 — 仅记录，不继承 BaseEntity（SQL 无 created_by/updated_by/updated_time/deleted）。
 */
@Data
@TableName("sys_login_log")
public class LoginLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private Integer loginType;
    private Integer status;
    private String ipAddress;
    private String userAgent;
    private String message;
    private LocalDateTime loginTime;
}
