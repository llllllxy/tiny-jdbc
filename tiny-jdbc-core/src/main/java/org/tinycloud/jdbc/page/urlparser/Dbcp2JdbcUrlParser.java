package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class Dbcp2JdbcUrlParser implements JdbcUrlParser {
    @Override
    public boolean supports(DataSource dataSource) {
        return dataSource instanceof org.apache.commons.dbcp2.BasicDataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return ((org.apache.commons.dbcp2.BasicDataSource) dataSource).getUrl();
    }
}
