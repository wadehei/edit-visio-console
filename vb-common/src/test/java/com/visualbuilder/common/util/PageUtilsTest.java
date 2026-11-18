package com.visualbuilder.common.util;

import com.visualbuilder.common.core.constant.LimitsConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("分页参数归一化")
class PageUtilsTest {

    @Test
    @DisplayName("页码：null 或小于 1 归一为第 1 页")
    void normalizePage_lowerBound() {
        assertEquals(1, PageUtils.normalizePage(null));
        assertEquals(1, PageUtils.normalizePage(0));
        assertEquals(1, PageUtils.normalizePage(-5));
    }

    @Test
    @DisplayName("页码：合法值原样返回")
    void normalizePage_normal() {
        assertEquals(1, PageUtils.normalizePage(1));
        assertEquals(3, PageUtils.normalizePage(3));
    }

    @Test
    @DisplayName("页码：超过上限钳到最大页码，避免深分页")
    void normalizePage_upperBound() {
        assertEquals(LimitsConst.MAX_PAGE_NUM, PageUtils.normalizePage(LimitsConst.MAX_PAGE_NUM));
        assertEquals(LimitsConst.MAX_PAGE_NUM, PageUtils.normalizePage(LimitsConst.MAX_PAGE_NUM + 1));
        assertEquals(LimitsConst.MAX_PAGE_NUM, PageUtils.normalizePage(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("每页条数：null 或小于 1 取默认值")
    void normalizePageSize_lowerBound() {
        assertEquals(LimitsConst.DEFAULT_PAGE_SIZE, PageUtils.normalizePageSize(null));
        assertEquals(LimitsConst.DEFAULT_PAGE_SIZE, PageUtils.normalizePageSize(0));
    }

    @Test
    @DisplayName("每页条数：超过上限截断")
    void normalizePageSize_upperBound() {
        assertEquals(20, PageUtils.normalizePageSize(20));
        assertEquals(LimitsConst.MAX_PAGE_SIZE, PageUtils.normalizePageSize(LimitsConst.MAX_PAGE_SIZE + 1));
    }
}
