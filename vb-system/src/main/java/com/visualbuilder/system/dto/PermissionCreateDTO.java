package com.visualbuilder.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionCreateDTO {

    @Schema(description = "权限名称")
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称最多50个字符")
    private String permissionName;

    @Schema(description = "权限标识")
    @NotBlank(message = "权限标识不能为空")
    @Size(max = 100, message = "权限标识最多100个字符")
    private String permissionKey;

    @Schema(description = "父权限ID")
    private Long parentId;

    @Schema(description = "权限类型")
    private Integer permissionType;

    @Schema(description = "路由路径")
    @Size(max = 200, message = "路由路径过长")
    private String path;

    @Schema(description = "组件路径")
    @Size(max = 200, message = "组件路径过长")
    private String component;

    @Schema(description = "图标名称")
    @Size(max = 50, message = "图标名称过长")
    private String icon;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "是否可见：1=可见,0=隐藏")
    private Integer visible;

    @Schema(description = "状态：1=正常,0=禁用")
    private Integer status;

    @Schema(description = "请求方法")
    @Size(max = 20, message = "请求方法过长")
    private String apiMethod;

    @Schema(description = "API路径")
    @Size(max = 200, message = "API路径过长")
    private String apiPath;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
