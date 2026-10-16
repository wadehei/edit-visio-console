package com.visualbuilder.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体 — 仅记录，不继承 BaseEntity（SQL 无 updated_by/updated_time/deleted）。
 */
@Data
@TableName("sys_log")
public class SysLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

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
    private LocalDateTime createdTime;
}
