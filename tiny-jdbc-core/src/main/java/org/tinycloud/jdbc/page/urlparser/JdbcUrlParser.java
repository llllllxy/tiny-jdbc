package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 连接池 jdbcUrl 解析器接口
 */
public abstract class JdbcUrlParser<T extends DataSource> {
    protected Class<?> dataSourceClass;

    public JdbcUrlParser() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        dataSourceClass = (Class<?>) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
    }

    /**
     * 从数据源中获取 jdbcUrl
     */
    public abstract String getJdbcUrl(T dataSource);

    /**
     * 根据数据源类型，解析 jdbcUrl
     */
    public String resolveJdbcUrl(DataSource dataSource) {
        if (dataSourceClass.isInstance(dataSource)) {
            return getJdbcUrl((T) dataSource);
        }
        return null;
    }
}
