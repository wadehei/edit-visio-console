package com.visualbuilder.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    private String coverImage;

    private Map<String, Object> settings;
}
