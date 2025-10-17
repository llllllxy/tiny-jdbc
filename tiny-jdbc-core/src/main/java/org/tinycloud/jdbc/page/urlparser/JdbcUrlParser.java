package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

/**
 * 连接池 jdbcUrl 解析器接口
 */
public interface JdbcUrlParser {
    /**
     * 判断当前解析器是否支持该数据源
     */
    boolean supports(DataSource dataSource);

    /**
     * 从数据源中获取 jdbcUrl
     */
    String getJdbcUrl(DataSource dataSource);
}
