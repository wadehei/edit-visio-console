package com.visualbuilder.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PageCreateDTO {

    @NotBlank(message = "页面标题不能为空")
    @Size(max = 100, message = "页面标题最多 100 个字符")
    private String title;

    @Pattern(regexp = "^$|^/[a-zA-Z0-9\\-_/]*$", message = "路径格式不正确，应以 / 开头")
    @Size(max = 200, message = "路径最多 200 个字符")
    private String path;

    private Boolean isHome;

    private Long templateId;
}
