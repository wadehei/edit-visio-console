package com.visualbuilder.common.util;

import com.visualbuilder.common.core.constant.LimitsConst;

/**
 * 分页参数归一化工具 — 统一各接口对 page / pageSize 的边界钳制，
 * 避免非法值（0、负数、超大值）直达数据库。
 */
public final class PageUtils {

    private PageUtils() {
    }

    /** 页码归一化：null 或小于 1 一律归为第 1 页 */
    public static int normalizePage(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    /** 每页条数归一化：null 或小于 1 取默认值，超过上限则截断 */
    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return LimitsConst.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, LimitsConst.MAX_PAGE_SIZE);
    }
}
