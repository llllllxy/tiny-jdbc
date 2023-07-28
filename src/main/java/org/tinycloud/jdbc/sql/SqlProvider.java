package org.tinycloud.jdbc.sql;


import java.util.List;

/**
 *
 * sql信息实体类
 * @author liuxingyu01
 * @since  2023-07-28-16:49
 **/
public class SqlProvider {

    // 要执行的SQL
    private String sql;

    // 要绑定到SQL的参数
    private List<Object> parameters;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }
}
