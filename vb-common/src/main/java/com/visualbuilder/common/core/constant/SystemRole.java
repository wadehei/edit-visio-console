package com.visualbuilder.common.core.constant;

/**
 * 系统级角色常量 — 用于 {@code @RequiresRole} 等注解与鉴权判断。
 */
public final class SystemRole {

    private SystemRole() {
    }

    /** 超级管理员 */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
}
