package com.visualbuilder.template.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_template", autoResultMap = true)
public class Template extends BaseEntity {
    private Long categoryId;
    private String name;
    private String description;
    private String coverUrl;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String pageSchema;
    private String tags;
    private Integer useCount;
    private Integer favoriteCount;
    private Integer isOfficial;
    private Long userId;
    private Integer status;
}
