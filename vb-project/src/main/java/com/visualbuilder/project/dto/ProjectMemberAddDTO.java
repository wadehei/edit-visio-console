package com.visualbuilder.project.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectMemberAddDTO {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 成员权限，取值：view / edit / admin；为空时默认 view。
     * owner 只能通过转让所有权获得，不可通过此处分配。
     * 兼容旧的 role 字段名（历史前端可能传 role）。
     */
    @Schema(description = "成员权限：view/edit/admin，为空默认 view")
    @JsonAlias("role")
    @Pattern(regexp = "(?i)view|edit|admin", message = "权限取值非法，仅允许 view/edit/admin")
    @Size(max = 20, message = "权限标识长度不能超过20个字符")
    private String permission;
}
