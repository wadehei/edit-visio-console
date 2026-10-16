package com.visualbuilder.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.template.dto.TemplateApplyVO;
import com.visualbuilder.template.dto.TemplateDetailVO;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.mapper.TemplateMapper;
import com.visualbuilder.template.service.TemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends BaseServiceImpl<TemplateMapper, Template>
        implements TemplateService {

    private final PageMapper pageMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_LIST = "template:list:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public List<Template> listTemplates(Long categoryId, String keyword, String tag) {
        // 无过滤条件时使用缓存
        if (categoryId == null && !StringUtils.hasText(keyword) && !StringUtils.hasText(tag)) {
            try {
                String cached = redisTemplate.opsForValue().get(CACHE_KEY_LIST);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<Template>>() {});
                }
            } catch (Exception e) {
                log.warn("读取模板缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(categoryId != null, Template::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Template::getName, keyword)
                .like(StringUtils.hasText(tag), Template::getTags, tag)
                .eq(Template::getStatus, StatusConst.Template.ON_SHELF)
                .orderByDesc(Template::getUseCount);
        List<Template> result = this.list(wrapper);

        // 写入缓存
        if (categoryId == null && !StringUtils.hasText(keyword) && !StringUtils.hasText(tag)) {
            try {
                redisTemplate.opsForValue().set(CACHE_KEY_LIST,
                        objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入模板缓存失败: {}", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public TemplateDetailVO getTemplateDetail(Long templateId) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在");

        return TemplateDetailVO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .coverUrl(template.getCoverUrl())
                .pageSchema(template.getPageSchema())
                .tags(template.getTags())
                .useCount(template.getUseCount())
                .favoriteCount(template.getFavoriteCount())
                .isOfficial(template.getIsOfficial())
                .createdTime(template.getCreatedTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateApplyVO applyTemplate(Long templateId, Long targetProjectId) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在");

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        // 在目标项目中创建新页面，复制模板的 pageSchema
        Page newPage = new Page();
        newPage.setProjectId(targetProjectId);
        newPage.setName(template.getName() + " (来自模板)");
        newPage.setPagePath("/page-" + System.currentTimeMillis());
        newPage.setPageType("h5");
        newPage.setSchemaContent(template.getPageSchema());
        newPage.setSchemaVersion(1);
        newPage.setStatus(StatusConst.Page.EDITING);
        newPage.setIsHome(StatusConst.YesNo.NO);
        newPage.setUserId(userId);
        pageMapper.insert(newPage);

        // 递增模板使用次数 + 清除缓存
        template.setUseCount(template.getUseCount() + 1);
        this.updateById(template);
        evictCache();

        log.info("应用模板成功: templateId={}, targetProjectId={}, newPageId={}",
                templateId, targetProjectId, newPage.getId());

        return TemplateApplyVO.builder()
                .templateId(templateId)
                .targetProjectId(targetProjectId)
                .newPageId(newPage.getId())
                .build();
    }

    private void evictCache() {
        redisTemplate.delete(CACHE_KEY_LIST);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(Template template) {
        if (template.getStatus() == null) {
            template.setStatus(StatusConst.Template.ON_SHELF);
        }
        if (template.getUseCount() == null) template.setUseCount(0);
        if (template.getFavoriteCount() == null) template.setFavoriteCount(0);
        this.save(template);
        evictCache();
        log.info("创建模板: name={}", template.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long templateId, Template updates) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在");
        if (updates.getName() != null) template.setName(updates.getName());
        if (updates.getDescription() != null) template.setDescription(updates.getDescription());
        if (updates.getCoverUrl() != null) template.setCoverUrl(updates.getCoverUrl());
        if (updates.getPageSchema() != null) template.setPageSchema(updates.getPageSchema());
        if (updates.getTags() != null) template.setTags(updates.getTags());
        if (updates.getStatus() != null) template.setStatus(updates.getStatus());
        if (updates.getCategoryId() != null) template.setCategoryId(updates.getCategoryId());
        this.updateById(template);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        Template template = this.getById(templateId);
        if (template == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在");
        this.removeById(templateId);
        evictCache();
    }
}
