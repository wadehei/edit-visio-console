package com.visualbuilder.common.security.annotation;

import java.lang.annotation.*;

/**
 * 接口权限校验注解
 * 使用示例: @RequiresPermission("system:user:create")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    /**
     * 所需权限标识，如 "system:user:create"
     */
    String[] value();

    /**
     * 逻辑关系：AND / OR
     */
    Logical logical() default Logical.AND;
}
