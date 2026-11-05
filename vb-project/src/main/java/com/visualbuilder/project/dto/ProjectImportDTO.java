package com.visualbuilder.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 项目导入入参 — 对应导出接口产出的 JSON 结构，忽略 name/version 之外的未知字段。
 */
@Data
public class ProjectImportDTO {

    @Size(max = 100, message = "项目名称最多 100 个字符")
    private String name;

    @Size(max = 500, message = "项目描述最多 500 个字符")
    private String description;

    @Size(max = 500, message = "封面 URL 过长")
    private String coverUrl;

    private Integer status;

    private Map<String, Object> settings;
}
