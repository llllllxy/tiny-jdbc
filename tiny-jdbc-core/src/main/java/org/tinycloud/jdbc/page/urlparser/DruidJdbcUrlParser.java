package org.tinycloud.jdbc.page.urlparser;

import com.alibaba.druid.pool.DruidDataSource;

public class DruidJdbcUrlParser extends JdbcUrlParser<DruidDataSource> {
    @Override
    public String getJdbcUrl(DruidDataSource dataSource) {
        return dataSource.getUrl();
    }
}
