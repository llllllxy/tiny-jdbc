package org.tinycloud.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.tinycloud.jdbc.util.DbType;

@ConfigurationProperties(prefix = "tiny-jdbc")
public class TinyJdbcProperties {

    private boolean banner;

    private DbType dbType;

    public boolean getBanner() {
        return banner;
    }

    public void setBanner(boolean banner) {
        this.banner = banner;
    }

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }
}
