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
     * 标记字段是否为主键
     */
    boolean primaryKey() default false;

    /**
     * 标记主键策略类型
     */
    IdType idType() default IdType.INPUT;
}
