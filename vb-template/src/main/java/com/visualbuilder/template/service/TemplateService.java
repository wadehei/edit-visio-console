package com.visualbuilder.template.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.template.dto.TemplateApplyVO;
import com.visualbuilder.template.dto.TemplateDetailVO;
import com.visualbuilder.template.entity.Template;
import java.util.List;

public interface TemplateService extends BaseService<Template> {
    List<Template> listTemplates(Long categoryId, String keyword, String tag);
    TemplateDetailVO getTemplateDetail(Long templateId);
    TemplateApplyVO applyTemplate(Long templateId, Long targetProjectId);
    void createTemplate(Template template);
    void updateTemplate(Long templateId, Template template);
    void deleteTemplate(Long templateId);
}
