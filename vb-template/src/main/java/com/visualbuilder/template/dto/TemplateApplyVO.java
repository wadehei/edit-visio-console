package com.visualbuilder.template.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateApplyVO {
    private Long templateId;
    private Long targetProjectId;
    private Long newPageId;
}
