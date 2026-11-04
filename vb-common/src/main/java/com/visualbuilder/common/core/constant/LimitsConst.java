package com.visualbuilder.common.core.constant;

/**
 * 业务限额常量 — 统一管理列表上限、分页上限与批量操作上限，
 * 避免各 Service 各自定义魔数导致口径不一致。
 */
public final class LimitsConst {

    private LimitsConst() {
    }

    /** 分页接口默认单页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 分页接口单页最大条数 — 限制响应体积与缓存 key 基数 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 无分页列表接口的单次最大返回条数 — 避免全表返回 */
    public static final int MAX_LIST_SIZE = 500;

    /** 项目成员列表上限 */
    public static final int MAX_MEMBER_LIST_SIZE = 200;

    /** 单次批量操作（删除、添加成员等）最大条数 */
    public static final int MAX_BATCH_SIZE = 50;
}
