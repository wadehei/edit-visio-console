package com.visualbuilder.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板分类实体。
 */
@Data
@TableName("biz_template_category")
public class TemplateCategory implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
