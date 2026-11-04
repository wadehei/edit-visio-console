package com.visualbuilder.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateDTO {

    @Schema(description = "昵称")
    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    @Schema(description = "邮箱")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @Schema(description = "头像URL")
    @Size(max = 500, message = "头像URL过长")
    private String avatar;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
