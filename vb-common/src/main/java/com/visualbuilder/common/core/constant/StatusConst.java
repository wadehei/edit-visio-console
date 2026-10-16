package com.visualbuilder.common.core.constant;

/**
 * 状态常量 — 消除各模块 status 魔法数字。
 *
 * <p>每个内部类对应一张表/一个业务域的 status 字段取值。
 * 命名统一使用含义明确的常量，避免直接写 0/1/2。</p>
 */
public final class StatusConst {

    private StatusConst() {}

    // ─── 用户状态 (sys_user) ─────────────────────────────────────
    // 0禁用 1正常 2锁定
    public static final class User {
        private User() {}
        public static final int DISABLED = 0;
        public static final int NORMAL   = 1;
        public static final int LOCKED   = 2;
    }

    // ─── 角色 / 权限 / 部门 / 组件通用状态 ────────────────────────
    // 0禁用 1正常
    public static final class Enable {
        private Enable() {}
        public static final int DISABLED = 0;
        public static final int ENABLED  = 1;
    }

    // ─── 项目状态 (biz_project) ──────────────────────────────────
    // 0草稿 1已发布 2已归档
    public static final class Project {
        private Project() {}
        public static final int DRAFT     = 0;
        public static final int PUBLISHED = 1;
        public static final int ARCHIVED  = 2;
    }

    // ─── 页面状态 (biz_page) ─────────────────────────────────────
    // 0编辑中 1已发布
    public static final class Page {
        private Page() {}
        public static final int EDITING   = 0;
        public static final int PUBLISHED = 1;
    }

    // ─── 发布记录状态 (biz_publish_record) ────────────────────────
    // 0处理中 1成功 2失败
    public static final class Publish {
        private Publish() {}
        public static final int PENDING  = 0;
        public static final int SUCCESS  = 1;
        public static final int FAILED   = 2;
    }

    // ─── 模板状态 (biz_template) ─────────────────────────────────
    // 0待审核 1已上架 2已下架
    public static final class Template {
        private Template() {}
        public static final int PENDING_REVIEW = 0;
        public static final int ON_SHELF       = 1;
        public static final int OFF_SHELF      = 2;
    }

    // ─── 是 / 否 布尔标记 ────────────────────────────────────────
    // 用于 is_system, is_home, deleted, visible 等 TINYINT(1) 字段
    public static final class YesNo {
        private YesNo() {}
        public static final int NO  = 0;
        public static final int YES = 1;
    }

    // ─── 登录日志类型 (sys_login_log.login_type) ─────────────────
    // 1登录 2登出
    public static final class LoginType {
        private LoginType() {}
        public static final int LOGIN  = 1;
        public static final int LOGOUT = 2;
    }
}
