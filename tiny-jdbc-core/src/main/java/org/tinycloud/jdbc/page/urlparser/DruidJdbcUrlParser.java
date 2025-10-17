package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class DruidJdbcUrlParser implements JdbcUrlParser{
    @Override
    public boolean supports(DataSource dataSource) {
        return dataSource instanceof com.alibaba.druid.pool.DruidDataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return ((com.alibaba.druid.pool.DruidDataSource) dataSource).getUrl();
    }
}
