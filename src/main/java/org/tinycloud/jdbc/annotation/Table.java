package org.tinycloud.jdbc.annotation;

import java.lang.annotation.*;

/**
 * 表名注解类
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    String value() default "";
}
