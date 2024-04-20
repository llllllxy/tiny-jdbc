package org.tinycloud.jdbc.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/20 15:40
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {
    /**
     * 标记主键策略类型
     */
    IdType idType() default IdType.INPUT;

    /**
     * 若 idType 类型是 sequence， value 则代表的是
     * sequence 序列的 sql 内容
     * 例如：select SEQ_USER_ID.nextval as id from dual
     */
    String value() default "";
}
