package com.visualbuilder.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateDTO {

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最多50个字符")
    private String roleName;

    @Schema(description = "角色标识")
    @NotBlank(message = "角色标识不能为空")
    @Size(max = 50, message = "角色标识最多50个字符")
    private String roleKey;

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
