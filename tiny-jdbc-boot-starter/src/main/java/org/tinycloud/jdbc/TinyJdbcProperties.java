package org.tinycloud.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.tinycloud.jdbc.util.DbType;

@ConfigurationProperties(prefix = "tiny-jdbc")
public class TinyJdbcProperties {

    /**
     * 是否打印 banner
     */
    private Boolean banner = true;

    /**
     * 默认数据库类型
     */
    private DbType dbType;

    /**
     * 是否使用运行时数据库类型，默认值为 false，设置为 true 时，会在运行时根据多数据源自动识别对应的分页处理器
     */
    private Boolean openRuntimeDbType = false;

    /**
     * 默认值为 true，当使用运行时动态数据源自动获取数据库类型时，会自动获取一个数据库连接， 通过该属性来设置是否关闭获取的这个连接，默认true关闭，设置为 false 后，不会关闭获取的连接，这个参数的设置要根据自己选择的数据源来决定。
     */
    private Boolean closeConn = true;

    /**
     * 数据源连接池类型（配置获取jdbcUrl的方法），允许配置为 hikari,druid,tomcat-jdbc,c3p0,dbcp,beecp,default，默认为 空
     */
    private String datasourceType;

    /**
     * 是否开启sql统计，默认值为 false
     */
    private Boolean sqlStatEnabled = false;

    public Boolean getBanner() {
        return banner;
    }

    public void setBanner(Boolean banner) {
        this.banner = banner;
    }

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    public Boolean getOpenRuntimeDbType() {
        return openRuntimeDbType;
    }

    public void setOpenRuntimeDbType(Boolean openRuntimeDbType) {
        this.openRuntimeDbType = openRuntimeDbType;
    }

    public Boolean getCloseConn() {
        return closeConn;
    }

    public void setCloseConn(Boolean closeConn) {
        this.closeConn = closeConn;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public Boolean getSqlStatEnabled() {
        return sqlStatEnabled;
    }

    public void setSqlStatEnabled(Boolean sqlStatEnabled) {
        this.sqlStatEnabled = sqlStatEnabled;
    }
}
