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

    /**
     * 数据库字段对应
     */
    String value() default "";

     /**
     * 是否存在该字段，默认存在
     */
    boolean exist() default true;
}
