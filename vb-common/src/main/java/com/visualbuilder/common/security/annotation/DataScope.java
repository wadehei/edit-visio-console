package com.visualbuilder.common.security.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解（加在 Service/Mapper 方法上）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    String userAlias() default "u";
    String deptAlias() default "d";
}
