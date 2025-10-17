package org.tinycloud.jdbc.page.urlparser;

import org.stone.beecp.BeeDataSource;

public class BeecpJdbcUrlParser extends JdbcUrlParser<BeeDataSource>{

    @Override
    public String getJdbcUrl(BeeDataSource dataSource) {
        return dataSource.getJdbcUrl();
    }
}
