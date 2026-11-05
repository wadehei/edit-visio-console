/*
 * @Author: songshaohui shaohui.song@hjlysz.com
 * @Date: 2026-11-04 10:29:20
 * @LastEditors: songshaohui shaohui.song@hjlysz.com
 * @LastEditTime: 2026-11-04 10:30:44
 * @FilePath: \edit-visio-console\vb-common\src\main\java\com\visualbuilder\common\core\constant\RedisKeyConst.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.visualbuilder.common.core.constant;

/**
 * Redis 缓存 Key 常量 — 统一管理跨模块使用的 Key 前缀，避免字面量重复与不一致。
 */
public final class RedisKeyConst {

    private RedisKeyConst() {}

    /** Token 黑名单（logout 后加入），拼接 jti */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 用户禁用标记（管理员禁用后即时生效），拼接 userId */
    public static final String USER_DISABLED_PREFIX = "user:disabled:";

    /** 发布任务并发锁，拼接 pageId */
    public static final String PUBLISH_LOCK_PREFIX = "publish:lock:";

    /** 刷新令牌（Refresh Token）缓存，拼接 userId */
    public static final String REFRESH_TOKEN_PREFIX = "token:refresh:";

    /** 登录失败计数，拼接登录标识（用户名/手机号） */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /** 账号锁定标记，拼接登录标识 */
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";

    public static String loginFail(String loginKey) {
        return LOGIN_FAIL_PREFIX + loginKey;
    }

    public static String loginLock(String loginKey) {
        return LOGIN_LOCK_PREFIX + loginKey;
    }

    public static String tokenBlacklist(String jti) {
        return TOKEN_BLACKLIST_PREFIX + jti;
    }

    public static String userDisabled(Long userId) {
        return USER_DISABLED_PREFIX + userId;
    }

    public static String publishLock(Long pageId) {
        return PUBLISH_LOCK_PREFIX + pageId;
    }

    public static String refreshToken(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
