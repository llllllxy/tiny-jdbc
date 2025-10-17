package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class C3p0JdbcUrlParser implements JdbcUrlParser{
    @Override
    public boolean supports(DataSource dataSource) {
        return dataSource instanceof com.mchange.v2.c3p0.ComboPooledDataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return ((com.mchange.v2.c3p0.ComboPooledDataSource) dataSource).getJdbcUrl();
    }
}
