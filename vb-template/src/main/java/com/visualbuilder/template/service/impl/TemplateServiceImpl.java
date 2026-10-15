package com.visualbuilder.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.mapper.TemplateMapper;
import com.visualbuilder.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends BaseServiceImpl<TemplateMapper, Template>
        implements TemplateService {

    @Override
    public List<Template> listTemplates(Long categoryId, String keyword, String tag) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(categoryId != null, Template::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Template::getName, keyword)
                .like(StringUtils.hasText(tag), Template::getTags, tag)
                .eq(Template::getStatus, 1)
                .orderByDesc(Template::getUseCount);
        return this.list(wrapper);
    }

    @Override
    public Map<String, Object> getTemplateDetail(Long templateId) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(40005, "模板不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", template.getId());
        result.put("name", template.getName());
        result.put("description", template.getDescription());
        result.put("coverUrl", template.getCoverUrl());
        result.put("pageSchema", template.getPageSchema());
        result.put("tags", template.getTags());
        result.put("useCount", template.getUseCount());
        result.put("favoriteCount", template.getFavoriteCount());
        result.put("isOfficial", template.getIsOfficial());
        result.put("createdTime", template.getCreatedTime());
        return result;
    }

    @Override
    public Map<String, Object> applyTemplate(Long templateId, Long targetProjectId) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(40005, "模板不存在");

        // TODO: implement template apply logic - copy pageSchema to target project
        log.info("应用模板: templateId={}, targetProjectId={}", templateId, targetProjectId);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("targetProjectId", targetProjectId);
        result.put("status", "TODO");
        result.put("message", "模板应用功能待实现");
        return result;
    }
}
