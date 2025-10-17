package org.tinycloud.jdbc.page.urlparser;

import javax.sql.DataSource;

public class TomcatJdbcUrlParser implements JdbcUrlParser{
    @Override
    public boolean supports(DataSource dataSource) {
        return dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource;
    }

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getUrl();
    }
}
