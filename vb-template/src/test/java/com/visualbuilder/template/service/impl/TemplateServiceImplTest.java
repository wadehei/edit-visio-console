package com.visualbuilder.template.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.project.service.ProjectMemberService;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.mapper.TemplateMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TemplateServiceImpl 单元测试 — 聚焦应用模板时的目标项目 edit 权限校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateServiceImpl 模板应用")
class TemplateServiceImplTest {

    @Mock
    private TemplateMapper templateMapper;

    @Mock
    private PageMapper pageMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ProjectMemberService projectMemberService;

    private TemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateServiceImpl(pageMapper, redisTemplate, new ObjectMapper(), projectMemberService);
        ReflectionTestUtils.setField(templateService, "baseMapper", templateMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clearContext();
    }

    private Template mockTemplate(Long id) {
        Template template = new Template();
        template.setId(id);
        template.setName("营销模板");
        template.setPageSchema("{\"children\":[]}");
        template.setStatus(1);
        return template;
    }

    @Test
    @DisplayName("模板不存在抛出 TEMPLATE_NOT_FOUND")
    void applyTemplate_templateNotFound() {
        when(templateMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.applyTemplate(1L, 100L));
        assertEquals(ErrorCode.TEMPLATE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("未登录应用模板抛出 UNAUTHORIZED")
    void applyTemplate_unauthenticated() {
        when(templateMapper.selectById(1L)).thenReturn(mockTemplate(1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.applyTemplate(1L, 100L));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("目标项目无 edit 权限应用模板抛出 FORBIDDEN")
    void applyTemplate_forbidden() {
        when(templateMapper.selectById(1L)).thenReturn(mockTemplate(1L));
        SecurityUtils.setContext(2L, "outsider");
        when(projectMemberService.hasProjectRole(100L, 2L, "edit")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.applyTemplate(1L, 100L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    @DisplayName("具备 edit 权限时成功应用模板并写入页面")
    void applyTemplate_editAllowed() {
        when(templateMapper.selectById(1L)).thenReturn(mockTemplate(1L));
        SecurityUtils.setContext(2L, "editor");
        when(projectMemberService.hasProjectRole(100L, 2L, "edit")).thenReturn(true);

        templateService.applyTemplate(1L, 100L);

        verify(pageMapper).insert(any(com.visualbuilder.page.entity.Page.class));
        verify(templateMapper).incrementUseCount(1L);
    }
}
