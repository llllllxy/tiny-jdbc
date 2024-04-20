package org.tinycloud.jdbc.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 主键字段标记注解
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
     * 标记主键的策略类型
     */
    IdType idType() default IdType.INPUT;

    /**
     * <p>
     * 若 idType 类型是 sequence， value 则代表的是sequence 序列的 sql 内容
     * </p>
     * 例如：  <br/>
     * Oracle、DM -> SELECT SEQ_USER_ID.NEXTVAL FROM DUAL    <br/>
     * PostgreSQL、Kingbase、H2、Lealone --> select nextval('SEQ_USER_ID')   <br/>
     * DB2 -> values nextval for SEQ_USER_ID   <br/>
     * SAP_HANA -> SELECT SEQ_USER_ID.NEXTVAL FROM DUMMY    <br/>
     * Firebird -> SELECT next value for SEQ_USER_ID from rdb$database
     */
    String value() default "";
}
