package org.tinycloud.jdbc.interceptor;

/**
 * SQL类型枚举类
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
public enum SqlType {
    /**
     * 未知执行方法
     */
    UNKNOWN,
    /**
     * 新增、修改、删除 SQL语句
     */
    UPDATE,
    /**
     * 查询 SQL语句
     */
    QUERY,
    /**
     * DDL、存储过程 SQL语句
     */
    EXECUTE
}
