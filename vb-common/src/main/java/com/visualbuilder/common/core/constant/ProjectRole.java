/*
 * @Author: songshaohui shaohui.song@hjlysz.com
 * @Date: 2026-11-04 16:03:40
 * @LastEditors: songshaohui shaohui.song@hjlysz.com
 * @LastEditTime: 2026-11-04 16:49:16
 * @FilePath: \edit-visio-console\vb-common\src\main\java\com\visualbuilder\common\core\constant\ProjectRole.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.visualbuilder.common.core.constant;

/**
 * 项目角色常量 — 与 {@code project_member.permission} 取值保持一致。
 * 等级：{@link #VIEW} &lt; {@link #EDIT} &lt; {@link #ADMIN} &lt; {@link #OWNER}。
 */
public final class ProjectRole {

    private ProjectRole() {
    }

    /** 只读 */
    public static final String VIEW = "view";

    /** 可编辑 */
    public static final String EDIT = "edit";

    /** 项目管理员 */
    public static final String ADMIN = "admin";

    /** 项目所有者（仅通过转让所有权获得） */
    public static final String OWNER = "owner";
}
