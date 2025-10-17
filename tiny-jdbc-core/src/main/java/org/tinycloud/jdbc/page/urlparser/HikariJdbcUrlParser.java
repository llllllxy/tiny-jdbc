package org.tinycloud.jdbc.page.urlparser;

import com.zaxxer.hikari.HikariDataSource;

public class HikariJdbcUrlParser extends JdbcUrlParser<HikariDataSource> {

    @Override
    public String getJdbcUrl(HikariDataSource dataSource) {
        // 直接调用 Hikari 的 getJdbcUrl() 方法
        return (dataSource).getJdbcUrl();
    }
}
