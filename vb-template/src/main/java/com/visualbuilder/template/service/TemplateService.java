package com.visualbuilder.template.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.template.entity.Template;
import java.util.List;
import java.util.Map;

public interface TemplateService extends BaseService<Template> {
    List<Template> listTemplates(Long categoryId, String keyword, String tag);
    Map<String, Object> getTemplateDetail(Long templateId);
    Map<String, Object> applyTemplate(Long templateId, Long targetProjectId);
}
