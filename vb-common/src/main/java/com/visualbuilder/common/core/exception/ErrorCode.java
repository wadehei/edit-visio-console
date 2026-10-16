package com.visualbuilder.common.core.exception;

/**
 * 统一错误码常量 — 消除各模块魔法数字。
 *
 * <pre>
 * 1xxxx  — 通用/参数校验
 * 2xxxx  — 用户/认证/权限
 * 3xxxx  — 项目管理
 * 4xxxx  — 页面/组件/模板
 * 5xxxx  — 系统/内部错误
 * </pre>
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ─── 通用 / 参数校验 ─────────────────────────────────────────
    public static final int PARAM_ERROR         = 10001;
    public static final int DATA_NOT_FOUND      = 10002;
    public static final int DATA_ALREADY_EXISTS = 10003;
    public static final int FILE_EMPTY          = 10004;
    public static final int FILE_TYPE_NOT_ALLOWED = 10005;
    public static final int FILE_SIZE_EXCEEDED  = 10006;

    // ─── 用户 / 认证 / 权限 ──────────────────────────────────────
    public static final int USER_NOT_FOUND      = 20001;
    public static final int USERNAME_EXISTS     = 20002;
    public static final int PASSWORD_WRONG      = 20003;
    public static final int TOKEN_EXPIRED       = 20004;
    public static final int TOKEN_INVALID       = 20005;
    public static final int UNAUTHORIZED        = 20006;
    public static final int FORBIDDEN           = 20007;
    public static final int ACCOUNT_DISABLED    = 20008;

    // ─── 项目管理 ────────────────────────────────────────────────
    public static final int PROJECT_NOT_FOUND   = 30001;
    public static final int PROJECT_MEMBER_EXISTS = 30002;

    // ─── 页面 / 组件 / 模板 ──────────────────────────────────────
    public static final int PAGE_NOT_FOUND      = 40001;
    public static final int COMPONENT_NOT_FOUND = 40002;
    public static final int TEMPLATE_NOT_FOUND  = 40003;
    public static final int RESOURCE_NOT_FOUND  = 40004;
    public static final int PUBLISH_FAILED      = 40005;

    // ─── 系统 / 内部错误 ─────────────────────────────────────────
    public static final int INTERNAL_ERROR      = 50000;
    public static final int STORAGE_ERROR       = 50001;
    public static final int THIRD_PARTY_ERROR   = 50002;
    public static final int TOO_MANY_REQUESTS   = 50003;
}
