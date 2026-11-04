package com.visualbuilder.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateDTO {

    @Schema(description = "角色名称")
    @Size(max = 50, message = "角色名称最多50个字符")
    private String roleName;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "数据范围")
    private Integer dataScope;

    @Schema(description = "状态：1=正常,0=禁用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
