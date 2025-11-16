package org.tinycloud.jdbc.page;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 分页SQL提供器，用于提供分页查询的SQL语句和参数
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-07-18 10:58
 */
public class PagingSQLProvider {
    /**
     * 要执行的SQL
     */
    private String sql;

    /**
     * 要绑定到SQL的参数
     */
    private Object[] parameters;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public static PagingSQLProvider create(String sql, List<Object> parameters) {
        PagingSQLProvider pagingSQLProvider = new PagingSQLProvider();
        pagingSQLProvider.setSql(sql);
        pagingSQLProvider.setParameters(parameters.toArray());
        return pagingSQLProvider;
    }

    public static PagingSQLProvider create(String sql, Object... parameters) {
        PagingSQLProvider pagingSQLProvider = new PagingSQLProvider();
        pagingSQLProvider.setSql(sql);
        pagingSQLProvider.setParameters(parameters);
        return pagingSQLProvider;
    }

    @Override
    public String toString() {
        return "PagingSQLProvider{" +
                "sql='" + sql + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }
}
