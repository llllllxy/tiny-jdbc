package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class BeecpJdbcUrlParser implements JdbcUrlParser{
    @Override
    public boolean supports(DataSource dataSource) {
        return dataSource instanceof org.stone.beecp.BeeDataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return ((org.stone.beecp.BeeDataSource) dataSource).getJdbcUrl();
    }
}
