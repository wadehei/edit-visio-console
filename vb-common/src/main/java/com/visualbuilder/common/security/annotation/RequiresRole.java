package com.visualbuilder.common.security.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解
 * 使用示例: @RequiresRole("SUPER_ADMIN")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String[] value();
    Logical logical() default Logical.AND;
}
