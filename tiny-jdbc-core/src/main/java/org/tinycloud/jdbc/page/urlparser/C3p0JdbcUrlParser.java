package org.tinycloud.jdbc.page.urlparser;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3p0JdbcUrlParser extends JdbcUrlParser<ComboPooledDataSource>{

    @Override
    public String getJdbcUrl(ComboPooledDataSource dataSource) {
        return dataSource.getJdbcUrl();
    }
}
