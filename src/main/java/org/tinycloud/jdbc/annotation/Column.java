package org.tinycloud.jdbc.annotation;

import java.lang.annotation.*;

/**
 * 表字段注解类
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {
    String value() default "";

    boolean primaryKey() default false;

    boolean autoIncrement() default false;
}
