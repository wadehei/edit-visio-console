package com.visualbuilder.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_project", autoResultMap = true)
public class Project extends BaseEntity {

    private String name;

    private String description;

    private String coverUrl;

    private Long userId;

    private Integer status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> settings;

    private LocalDateTime publishedAt;
}
