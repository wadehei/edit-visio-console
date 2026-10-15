package com.visualbuilder.page.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PageSchemaDTO {

    @NotNull(message = "schema 内容不能为空")
    private Map<String, Object> schema;

    private Boolean isAutoSave;
}
