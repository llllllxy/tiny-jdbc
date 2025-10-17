package org.tinycloud.jdbc.page.urlparser;

import org.apache.tomcat.jdbc.pool.DataSource;

public class TomcatJdbcUrlParser extends JdbcUrlParser<DataSource>{

    @Override
    public String getJdbcUrl(DataSource dataSource) {
        return  dataSource.getUrl();
    }
}
