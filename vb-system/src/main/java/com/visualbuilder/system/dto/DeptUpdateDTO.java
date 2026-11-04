package com.visualbuilder.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptUpdateDTO {

    @Schema(description = "部门名称")
    @Size(max = 50, message = "部门名称最多50个字符")
    private String deptName;

    @Schema(description = "负责人")
    @Size(max = 50, message = "负责人名称过长")
    private String leader;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号过长")
    private String phone;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：1=正常,0=禁用")
    private Integer status;
}
