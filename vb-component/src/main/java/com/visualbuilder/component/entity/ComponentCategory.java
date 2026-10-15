package com.visualbuilder.component.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_component_category")
public class ComponentCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private String icon;
    private Integer sortOrder;
    @TableLogic
    private Integer deleted;
}
