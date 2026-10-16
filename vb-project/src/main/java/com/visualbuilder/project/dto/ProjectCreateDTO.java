package com.visualbuilder.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class ProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最多 100 个字符")
    private String name;

    @Size(max = 500, message = "项目描述最多 500 个字符")
    private String description;

    @Size(max = 500, message = "封面 URL 过长")
    private String coverImage;

    private Map<String, Object> settings;
}
