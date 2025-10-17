package org.tinycloud.jdbc.page.urlparser;

import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.exception.TinyJdbcException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class FallbackJdbcUrlParser extends JdbcUrlParser<DataSource> {
    @Override
    public String getJdbcUrl(DataSource dataSource) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new TinyJdbcException("Can not get jdbcUrl from connection metadata!", e);
        } finally {
            if (GlobalConfig.getConfig().getCloseConn()) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }
}
