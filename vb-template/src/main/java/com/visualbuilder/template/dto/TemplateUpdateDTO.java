package com.visualbuilder.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TemplateUpdateDTO {

    @Schema(description = "模板名称")
    @Size(max = 100, message = "模板名称最多100个字符")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "模板描述")
    @Size(max = 500, message = "模板描述过长")
    private String description;

    @Schema(description = "封面图URL")
    @Size(max = 500, message = "封面URL过长")
    private String coverUrl;

    @Schema(description = "页面Schema")
    private String pageSchema;

    @Schema(description = "标签")
    @Size(max = 200, message = "标签过长")
    private String tags;

    @Schema(description = "状态：1=正常,0=禁用")
    private Integer status;
}
