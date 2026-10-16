package com.visualbuilder.common.security.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 标注在 Controller 方法上，自动记录操作到 sys_log 表。
 *
 * <pre>
 * &#64;OpLog(module = "用户管理", action = "删除用户")
 * &#64;DeleteMapping("/{id}")
 * public R&lt;Void&gt; deleteUser(@PathVariable Long id) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {
    /** 模块名称 */
    String module() default "";

    /** 操作描述 */
    String action() default "";
}
