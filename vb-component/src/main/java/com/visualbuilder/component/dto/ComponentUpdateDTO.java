package com.visualbuilder.component.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComponentUpdateDTO {

    @Schema(description = "组件名称")
    @Size(max = 100, message = "组件名称最多100个字符")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "组件描述")
    @Size(max = 500, message = "组件描述过长")
    private String description;

    @Schema(description = "缩略图URL")
    @Size(max = 500, message = "缩略图URL过长")
    private String thumbnail;

    @Schema(description = "组件Schema定义")
    private String schemaDefine;

    @Schema(description = "默认属性")
    private String defaultProps;

    @Schema(description = "默认样式")
    private String defaultStyles;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：1=正常,0=禁用")
    private Integer status;
}
