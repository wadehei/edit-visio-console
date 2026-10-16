package com.visualbuilder.page.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.page.dto.PageVO;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.page.mapper.PageVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PageServiceImpl 单元测试 — 聚焦批量删除和核心业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageServiceImpl 页面管理")
class PageServiceImplTest {

    @Mock
    private PageMapper pageMapper;

    @Mock
    private PageVersionMapper pageVersionMapper;

    private PageServiceImpl pageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pageService = new PageServiceImpl(pageVersionMapper, objectMapper);
        ReflectionTestUtils.setField(pageService, "baseMapper", pageMapper);
    }

    @Test
    @DisplayName("批量删除 - 空列表抛出 PARAM_ERROR")
    void batchDeletePages_emptyList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> pageService.batchDeletePages(Collections.emptyList()));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("批量删除 - null 列表抛出 PARAM_ERROR")
    void batchDeletePages_nullList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> pageService.batchDeletePages(null));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("批量删除 - 超过50条抛出 PARAM_ERROR")
    void batchDeletePages_tooManyItems() {
        Long[] bigArray = new Long[51];
        for (int i = 0; i < 51; i++) {
            bigArray[i] = (long) i;
        }
        List<Long> bigList = Arrays.asList(bigArray);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pageService.batchDeletePages(bigList));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
        assertTrue(ex.getMessage().contains("50"));
    }

    @Test
    @DisplayName("页面不存在时抛出 PAGE_NOT_FOUND")
    void getPageDetail_notFound() {
        when(pageMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pageService.getPageDetail(999L));
        assertEquals(ErrorCode.PAGE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("获取页面详情成功")
    void getPageDetail_success() {
        Page page = createMockPage(1L, 100L, "测试页面");
        when(pageMapper.selectById(1L)).thenReturn(page);

        PageVO result = pageService.getPageDetail(1L);

        assertNotNull(result);
        assertEquals("测试页面", result.getTitle());
        assertEquals(100L, result.getProjectId());
    }

    @Test
    @DisplayName("删除不存在的页面抛出异常")
    void deletePage_notFound() {
        when(pageMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pageService.deletePage(999L));
        assertEquals(ErrorCode.PAGE_NOT_FOUND, ex.getCode());
    }

    private Page createMockPage(Long pageId, Long projectId, String name) {
        Page page = new Page();
        page.setId(pageId);
        page.setProjectId(projectId);
        page.setName(name);
        page.setPagePath("/test-page");
        page.setPageType("h5");
        page.setSchemaVersion(0);
        page.setStatus(1);
        page.setIsHome(0);
        page.setUserId(1L);
        page.setCreatedTime(LocalDateTime.now());
        page.setUpdatedTime(LocalDateTime.now());
        return page;
    }
}
