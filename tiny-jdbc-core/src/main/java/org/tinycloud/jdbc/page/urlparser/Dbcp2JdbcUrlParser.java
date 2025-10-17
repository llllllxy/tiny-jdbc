package org.tinycloud.jdbc.page.urlparser;

import org.apache.commons.dbcp2.BasicDataSource;

public class Dbcp2JdbcUrlParser extends JdbcUrlParser<BasicDataSource> {

    @Override
    public String getJdbcUrl(BasicDataSource dataSource) {
        return dataSource.getUrl();
    }
}
