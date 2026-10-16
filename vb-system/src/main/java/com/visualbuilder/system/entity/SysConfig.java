package com.visualbuilder.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体 — 不继承 BaseEntity（SQL 无 created_by/created_time/deleted）。
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;
    private String configValue;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedTime;
}
