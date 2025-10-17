package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class HikariJdbcUrlParser implements JdbcUrlParser {
    @Override
    public boolean supports(DataSource dataSource) {
        // 判断数据源是否为 HikariDataSource 类型
        return dataSource instanceof com.zaxxer.hikari.HikariDataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        // 直接调用 Hikari 的 getJdbcUrl() 方法
        return ((com.zaxxer.hikari.HikariDataSource) dataSource).getJdbcUrl();
    }
}
