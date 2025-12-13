package org.tinycloud.jdbc.interceptor;


import java.util.HashMap;
import java.util.Map;

/**
 * 自定义拦截器参数
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
public class SqlInvocation {
    private String sql;
    private Object[] args;
    private SqlType sqlType;
    private Map<String, Object> metadata;

    public SqlInvocation() {
        this.metadata = new HashMap<>(0);
    }

    public SqlInvocation(String sql, Object[] args, SqlType sqlType) {
        this.sql = sql;
        this.args = args;
        this.sqlType = sqlType;
        this.metadata = new HashMap<>(0);
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void putMetadata(String key, Object value) {
        if (this.metadata != null) {
            this.metadata.put(key, value);
        }
    }

    public Object getMetadata(String key) {
        if (this.metadata != null) {
            return this.metadata.get(key);
        }
        return null;
    }
}
