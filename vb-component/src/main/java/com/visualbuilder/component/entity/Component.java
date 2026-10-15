package com.visualbuilder.component.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_component", autoResultMap = true)
public class Component extends BaseEntity {
    private Long categoryId;
    private String type;
    private String name;
    private String description;
    private String thumbnail;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String schemaDefine;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String defaultProps;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String defaultStyles;
    private Integer isSystem;
    private Long userId;
    private Integer sortOrder;
    private Integer status;
}
