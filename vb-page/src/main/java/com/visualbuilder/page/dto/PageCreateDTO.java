package com.visualbuilder.page.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PageCreateDTO {

    @NotBlank(message = "页面标题不能为空")
    private String title;

    private String path;

    private Boolean isHome;

    private Long templateId;
}
